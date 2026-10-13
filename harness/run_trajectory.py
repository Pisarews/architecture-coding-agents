"""Run one trajectory: one agent works through a task sequence on its own evolving copy of a variant.

    python harness/run_trajectory.py --domain fhir --variant none --model weak --run 1 \
        --tasks P1,P2,P3 --out data/pilot

Resumable: finished tasks are recorded in <out>/<trajectory>/state.json and skipped on restart.
An attempt that dies of an infrastructure problem (API limits, provider errors, Docker) is rolled
back to the last commit and retried later; it never counts as an agent failure (PREREGISTRATION §6.3).

Exit codes: 0 = trajectory complete, 3 = stopped (task budget for this call used / outside window),
4 = API limit reached (stop the whole queue), 1 = error.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import shutil
import subprocess
import sys
import time
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).resolve().parent))
from grade import grade  # noqa: E402

from minisweagent import __version__ as MINI_VERSION  # noqa: E402
from minisweagent.agents.default import DefaultAgent  # noqa: E402
from minisweagent.environments.docker import DockerEnvironment  # noqa: E402
from minisweagent.models import get_model  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
HARNESS = ROOT / "harness"
STOPPED = 3
API_LIMIT = 4
MAX_INFRA_RETRIES = 3


class Stop(Exception):
    """Stop the night's work cleanly (window closed or daily API limit reached)."""


def in_window(window: str | None) -> bool:
    if not window:
        return True
    start, end = (int(x) for x in window.split("-"))
    h = dt.datetime.now().hour
    return start <= h < end if start < end else (h >= start or h < end)


def git(repo: Path, *args: str) -> str:
    return subprocess.run(["git", "-C", str(repo), *args], check=True, capture_output=True, text=True).stdout


def init_repo(domain: str, variant: str, repo: Path) -> None:
    src = ROOT / domain / "variants" / variant
    shutil.copytree(src, repo, ignore=shutil.ignore_patterns("target", "gate-logs"))
    (repo / ".gitignore").write_text("target/\n*.log\n")
    git(repo, "init", "-q")
    git(repo, "add", "-A")
    git(repo, "-c", "user.name=harness", "-c", "user.email=harness@archagents.invalid", "commit", "-q", "-m", "baseline")


def load_task(domain: str, task_id: str) -> dict:
    d = ROOT / domain / "tasks" / task_id
    meta = yaml.safe_load((d / "meta.yaml").read_text())
    meta["ticket"] = (d / "ticket.md").read_text()
    return meta


def usage_totals(messages: list[dict]) -> dict:
    prompt = completion = cached = 0
    providers: set[str] = set()
    for m in messages:
        resp = (m.get("extra") or {}).get("response") or {}
        u = resp.get("usage") or {}
        prompt += u.get("prompt_tokens", 0) or 0
        completion += u.get("completion_tokens", 0) or 0
        cached += ((u.get("prompt_tokens_details") or {}).get("cached_tokens", 0)) or 0
        if resp.get("provider"):
            providers.add(resp["provider"])
    return {"prompt_tokens": prompt, "completion_tokens": completion, "cached_tokens": cached,
            "providers": sorted(providers)}


def is_daily_limit(err: str) -> bool:
    e = err.lower()
    return "rate limit" in e or "429" in e or "402" in e or "free-models-per-day" in e


def run_attempt(*, task: dict, repo: Path, model_cfg: dict, agent_cfg: dict, traj_out: Path) -> dict:
    env_cfg = dict(agent_cfg["environment"])
    env_cfg["run_args"] = ["--rm", "--network", "none", "--memory", "3g", "--cpus", "2", "-v", f"{repo}:/workspace"]
    env = DockerEnvironment(**env_cfg)
    model = get_model(config={**agent_cfg.get("model", {}), **{k: v for k, v in model_cfg.items() if k != "window_only"}})
    agent = DefaultAgent(model, env, **agent_cfg["agent"], output_path=traj_out)
    t0 = time.time()
    try:
        info = agent.run(task=task["ticket"])
    finally:
        subprocess.run(["docker", "rm", "-f", env.container_id], capture_output=True)
    return {
        "exit_status": info.get("exit_status", ""),
        "exception": info.get("exception_str", ""),
        "api_calls": agent.n_calls,
        "cost_usd": agent.cost,
        "wall_seconds": round(time.time() - t0, 1),
        **usage_totals(agent.messages),
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--domain", required=True)
    ap.add_argument("--variant", required=True, choices=["none", "layered", "hexagonal"])
    ap.add_argument("--model", required=True)
    ap.add_argument("--run", type=int, required=True)
    ap.add_argument("--tasks", required=True, help="comma-separated, in order, e.g. P1,P2,P3")
    ap.add_argument("--out", required=True)
    ap.add_argument("--window", default=None, help="local hours, e.g. 22-7; outside it the run stops cleanly")
    ap.add_argument("--models-file", default=str(HARNESS / "models.yaml"))
    ap.add_argument("--max-tasks", type=int, default=0, help="do at most N tasks this call (0 = all); used by the queue")
    args = ap.parse_args()

    agent_cfg = yaml.safe_load((HARNESS / "agent.yaml").read_text())
    model_cfg = yaml.safe_load(Path(args.models_file).read_text())[args.model]
    image = agent_cfg["environment"]["image"]
    tasks = [load_task(args.domain, t) for t in args.tasks.split(",")]

    traj_id = f"{args.domain}-{args.variant}-{args.model}-r{args.run}"
    tdir = Path(args.out).resolve() / traj_id
    repo = tdir / "repo"
    state_f = tdir / "state.json"
    results_f = Path(args.out).resolve() / "results.jsonl"
    tdir.mkdir(parents=True, exist_ok=True)
    if not repo.exists():
        init_repo(args.domain, args.variant, repo)
    state = json.loads(state_f.read_text()) if state_f.exists() else {"done": [], "infra_retries": {}}

    # guard against a kill between writing the result row and saving state: never grade a task twice
    if results_f.exists():
        for line in results_f.read_text().splitlines():
            r = json.loads(line) if line.strip() else {}
            if r.get("trajectory") == traj_id and not r.get("missing") and r.get("task") not in state["done"]:
                state["done"].append(r["task"])
    did = 0
    for i, task in enumerate(tasks):
        if task["id"] in state["done"]:
            continue
        if args.max_tasks and did >= args.max_tasks:
            return STOPPED
        did += 1
        if model_cfg.get("window_only") and not in_window(args.window):
            print(f"[{traj_id}] outside window {args.window}; stopping before {task['id']}")
            return STOPPED
        # start every attempt from the last committed state (an interrupted attempt may have left edits)
        git(repo, "reset", "-q", "--hard")
        git(repo, "clean", "-qfdx")
        head_before = git(repo, "rev-parse", "HEAD").strip()
        print(f"[{traj_id}] {task['id']} ...", flush=True)
        try:
            att = run_attempt(task=task, repo=repo, model_cfg=model_cfg, agent_cfg=agent_cfg,
                              traj_out=tdir / f"{task['id']}.traj.json")
        except Exception as e:  # noqa: BLE001 - every escape here is infrastructure, not agent behaviour
            att = {"exit_status": type(e).__name__, "exception": str(e)}

        infra = att["exit_status"] not in ("Submitted", "LimitsExceeded", "TimeExceeded", "RepeatedFormatError")
        if infra:
            git(repo, "reset", "-q", "--hard", head_before)
            git(repo, "clean", "-qfdx")
            n = state["infra_retries"].get(task["id"], 0) + 1
            state["infra_retries"][task["id"]] = n
            state_f.write_text(json.dumps(state, indent=2))
            print(f"[{traj_id}] {task['id']} infra failure #{n}: {att['exit_status']} {att['exception'][:300]}")
            if is_daily_limit(att.get("exception", "")):
                return API_LIMIT
            if n >= MAX_INFRA_RETRIES:
                print(f"[{traj_id}] giving up on {task['id']} after {n} infra failures (recorded as missing)")
                _append(results_f, {"trajectory": traj_id, "task": task["id"], "missing": True, **att})
                return 1
            return STOPPED

        git(repo, "add", "-A")
        diff = git(repo, "diff", "--cached", "--shortstat")
        git(repo, "-c", "user.name=agent", "-c", "user.email=agent@archagents.invalid",
            "commit", "-q", "--allow-empty", "-m", f"{task['id']}")
        g = grade(domain=args.domain, repo=repo, task=task, previous_tasks=tasks[:i], image=image)
        record = {
            "trajectory": traj_id, "domain": args.domain, "variant": args.variant, "model": args.model,
            "model_name": model_cfg["model_name"], "run": args.run, "task": task["id"], "task_index": i,
            "stratum": task.get("stratum"), "commit": git(repo, "rev-parse", "HEAD").strip(),
            "diff_shortstat": diff.strip(), "mini_version": MINI_VERSION,
            "finished_at": dt.datetime.now(dt.timezone.utc).isoformat(),
            **att, **g.to_dict(),
        }
        # A regression is a test that passed at the previous grading and fails now. Tests of an earlier
        # task the agent never solved are "still failing", not regressions. Before the first task only
        # the baseline suite exists, and it passes on every baseline (equivalence gate).
        now_passing = {t for t, ok in {**g.task_tests, **g.regression_tests}.items() if ok}
        prev_passing = set(state["passing"]) if "passing" in state else None
        failing_reg = {t for t, ok in g.regression_tests.items() if not ok}
        record["regressions"] = sorted(failing_reg if prev_passing is None else failing_reg & prev_passing)
        record["still_failing"] = sorted(failing_reg - set(record["regressions"]))
        if g.built and g.started:
            state["passing"] = sorted(now_passing)
        if not g.built or not g.started:
            record["grade_log_tail"] = g.log_tail[-1500:]
        _append(results_f, record)
        state["done"].append(task["id"])
        state_f.write_text(json.dumps(state, indent=2))
        print(f"[{traj_id}] {task['id']} success={g.success} regressions={len(g.regressions)} "
              f"calls={att['api_calls']} tokens={att['prompt_tokens']}+{att['completion_tokens']}", flush=True)
    return 0


def _append(path: Path, rec: dict) -> None:
    with path.open("a") as f:
        f.write(json.dumps(rec) + "\n")


if __name__ == "__main__":
    sys.exit(main())
