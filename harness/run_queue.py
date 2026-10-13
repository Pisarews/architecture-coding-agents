"""Night queue: interleave all trajectories of a study one task at a time.

    caffeinate -dimsu python harness/run_queue.py --plan harness/plans/pilot.yaml --out data/pilot --window 22-7

Order: for each task position, trajectories are visited in a seeded random order, so provider drift,
rate limiting or a model being pulled hits every architecture arm equally (PREREGISTRATION §2.4).
Safe to start every night; it resumes where it stopped.
"""

from __future__ import annotations

import argparse
import itertools
import random
import subprocess
import sys
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
STOPPED = 3


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--plan", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--window", default=None)
    ap.add_argument("--runs", default=None, help="comma-separated subset of the plan's runs (parallel workers)")
    args = ap.parse_args()
    plan = yaml.safe_load(Path(args.plan).read_text())
    if args.runs:
        plan["runs"] = [int(r) for r in args.runs.split(",")]
    trajs = [dict(domain=d, variant=v, model=m, run=r)
             for d, v, m, r in itertools.product(plan["domains"], plan["variants"], plan["models"], plan["runs"])]
    rng = random.Random(plan.get("seed", 20261011) + 1000 * min(plan["runs"]))
    tasks = ",".join(plan["tasks"])
    finished: set[int] = set()
    # one pass per task position, plus extra passes that pick up attempts retried after infra failures
    for _position in [*plan["tasks"], *["retry"] * 3]:
        order = list(range(len(trajs)))
        rng.shuffle(order)
        for k in order:
            if k in finished:
                continue
            t = trajs[k]
            cmd = [sys.executable, str(HERE / "run_trajectory.py"), "--domain", t["domain"], "--variant", t["variant"],
                   "--model", t["model"], "--run", str(t["run"]), "--tasks", tasks, "--out", args.out,
                   "--max-tasks", "1"]
            if args.window:
                cmd += ["--window", args.window]
            rc = subprocess.run(cmd).returncode
            if rc == 0:
                finished.add(k)
            elif rc == 4:
                print("API limit reached; queue stops until next night"); return 5
            elif rc == STOPPED:
                # either this trajectory's single task is done (normal) or the night is over / API limit hit
                if args.window and not _in_window(args.window):
                    print("window closed; queue stops"); return STOPPED
            else:
                print(f"trajectory {t} returned {rc}; continuing with the others")
    return 0


def _in_window(window: str) -> bool:
    import datetime as dt
    start, end = (int(x) for x in window.split("-"))
    h = dt.datetime.now().hour
    return start <= h < end if start < end else (h >= start or h < end)


if __name__ == "__main__":
    sys.exit(main())
