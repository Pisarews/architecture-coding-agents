"""Phase 1 orchestrator: runs R1..R5 with at most MAX_PARALLEL runs at a time and applies the
outcome-blind cost rule of PREREGISTRATION §5.3 before starting R5.

    nohup python harness/run_phase1.py > data/phase1/orchestrator.log 2>&1 &

Each run is one worker (`run_queue.py --runs N`) that interleaves all six architecture × model
trajectories of that run, task position by task position, in seeded random order, so every worker
spreads provider drift over all arms (§2.4). Workers are resumable; restarting this script is safe.
"""

from __future__ import annotations

import json
import os
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
OUT = ROOT / "data" / "phase1"
PLAN = HERE / "plans" / "phase1.yaml"
RUNS = [1, 2, 3, 4, 5]
MAX_PARALLEL = int(os.getenv("PHASE1_PARALLEL", "2"))
ATTEMPTS_TOTAL = 720
BUDGET_USD = 30.0
TASKS_PER_TRAJ = 24
TRAJ_PER_RUN = 6
PRICES = {"strong": (0.09, 0.18, 0.009), "weak": (0.07, 0.28, 0.07)}  # same as analysis/analyze.py


def log(msg: str) -> None:
    print(f"{time.strftime('%F %T')} {msg}", flush=True)


def rows() -> list[dict]:
    f = OUT / "results.jsonl"
    return [json.loads(l) for l in f.read_text().splitlines() if l.strip()] if f.exists() else []


def cost(r: dict) -> float:
    if r.get("cost_usd"):
        return r["cost_usd"]
    pin, pout, pc = PRICES[r["model"]]
    cached = r.get("cached_tokens", 0) or 0
    return ((r["prompt_tokens"] - cached) * pin + cached * pc + r["completion_tokens"] * pout) / 1e6


def run_complete(run: int) -> bool:
    return sum(1 for r in rows() if r.get("run") == run and not r.get("missing")) >= TASKS_PER_TRAJ * TRAJ_PER_RUN


def cost_rule_allows_r5() -> bool:
    rs = [r for r in rows() if not r.get("missing")]
    mean = sum(cost(r) for r in rs) / len(rs)
    forecast = mean * ATTEMPTS_TOTAL
    log(f"cost rule: {len(rs)} attempts, mean ${mean:.4f}/attempt, forecast ${forecast:.2f} (budget ${BUDGET_USD})")
    return forecast <= BUDGET_USD


def start(run: int) -> subprocess.Popen:
    env = dict(os.environ)
    log_f = open(OUT / f"queue-r{run}.log", "a")
    cmd = ["caffeinate", "-dimsu", sys.executable, str(HERE / "run_queue.py"), "--plan", str(PLAN),
           "--out", str(OUT), "--runs", str(run)]
    log(f"start R{run}")
    return subprocess.Popen(cmd, stdout=log_f, stderr=subprocess.STDOUT, cwd=ROOT, env=env)


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    if not os.getenv("OPENROUTER_API_KEY"):
        envf = Path.home() / ".config/archagents/env"
        for line in envf.read_text().splitlines():
            k, _, v = line.partition("=")
            os.environ[k.strip()] = v.strip()
    pending = [r for r in RUNS if not run_complete(r)]
    log_txt = (OUT / "orchestrator.log").read_text() if (OUT / "orchestrator.log").exists() else ""
    if "R5 dropped" in log_txt and 5 in pending:
        pending.remove(5)  # the cost-rule decision is final; never re-evaluate it after a restart
    active: dict[int, subprocess.Popen] = {}
    r5_decided = 5 not in pending
    while pending or active:
        for run, proc in list(active.items()):
            rc = proc.poll()
            if rc is None:
                continue
            del active[run]
            if run_complete(run):
                log(f"R{run} complete")
            elif rc == 5:
                log(f"R{run} stopped on API limit / balance; retry in 10 min")
                time.sleep(600)
                pending.insert(0, run)
            else:
                log(f"R{run} exited rc={rc} before completion; restarting in 60 s")
                time.sleep(60)
                pending.insert(0, run)
        while pending and len(active) < MAX_PARALLEL:
            nxt = pending[0]
            if nxt == 5 and not r5_decided:
                if not run_complete(1):
                    break  # wait until R1 is complete before deciding on R5
                r5_decided = True
                if not cost_rule_allows_r5():
                    log("cost rule: R5 dropped (4 runs per cell), as preregistered")
                    pending.pop(0)
                    continue
            pending.pop(0)
            active[nxt] = start(nxt)
        time.sleep(30)
    log("Phase 1 complete")
    return 0


if __name__ == "__main__":
    sys.exit(main())
