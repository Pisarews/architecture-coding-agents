"""Post-hoc re-grading of recorded Phase 1 attempts at low load (DEVIATIONS 2026-10-15).

    python harness/regrade.py data/phase1 --only-failed -j 1

Grades the exact committed snapshot of each selected attempt again with the experiment grader and
appends one row per attempt to <run>/regrade.jsonl. Resumable. An attempt whose outcome differs from
the recorded one is graded a third time; the majority of the three gradings is the "stable" outcome.
The recorded results are never modified.
"""

from __future__ import annotations

import argparse
import concurrent.futures as cf
import json
import sys
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).resolve().parent))
from grade import grade  # noqa: E402
from run_trajectory import load_task  # noqa: E402

HERE = Path(__file__).resolve().parent
ORDER = [f"T{i:02d}" for i in range(1, 25)]


def regrade_one(run: Path, r: dict, image: str) -> dict:
    ids = ORDER[: ORDER.index(r["task"]) + 1]
    tasks = [load_task(r["domain"], t) for t in ids]
    repo = run / r["trajectory"] / "repo"
    outs = []
    for k in range(3):
        g = grade(domain=r["domain"], repo=repo, task=tasks[-1], previous_tasks=tasks[:-1], image=image, ref=r["commit"])
        outs.append({"success": g.success, "built": g.built, "started": g.started,
                     "task_passed": sum(g.task_tests.values()), "task_total": len(g.task_tests),
                     "failing_regression_tests": sum(1 for v in g.regression_tests.values() if not v)})
        if k == 0 and g.success == bool(r["success"]):
            break  # agrees with the recorded outcome: one grading is enough
    votes = [o["success"] for o in outs]
    stable = max(set(votes), key=votes.count) if len(outs) > 1 else votes[0]
    return {"trajectory": r["trajectory"], "task": r["task"], "commit": r["commit"],
            "recorded_success": bool(r["success"]), "gradings": outs, "stable_success": stable,
            "changed": stable != bool(r["success"])}


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("run")
    ap.add_argument("--only-failed", action="store_true")
    ap.add_argument("-j", type=int, default=1)
    args = ap.parse_args()
    run = Path(args.run).resolve()
    image = yaml.safe_load((HERE / "agent.yaml").read_text())["environment"]["image"]
    rows = [json.loads(l) for l in (run / "results.jsonl").read_text().splitlines() if l.strip()]
    if args.only_failed:
        rows = [r for r in rows if not r["success"]]
    out_f = run / "regrade.jsonl"
    done = set()
    if out_f.exists():
        done = {(json.loads(l)["trajectory"], json.loads(l)["task"]) for l in out_f.read_text().splitlines() if l.strip()}
    todo = [r for r in rows if (r["trajectory"], r["task"]) not in done]
    print(f"{len(todo)} attempts to re-grade ({len(done)} already done)", flush=True)
    with cf.ThreadPoolExecutor(max_workers=args.j) as ex, out_f.open("a") as f:
        for res in ex.map(lambda r: regrade_one(run, r, image), todo):
            f.write(json.dumps(res) + "\n")
            f.flush()
            if res["changed"]:
                print(f"CHANGED {res['trajectory']} {res['task']}: recorded {res['recorded_success']} -> {res['stable_success']}", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
