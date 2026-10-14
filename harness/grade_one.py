"""Grade the committed HEAD of a repository for one task, exactly as the experiment grader does.

    python harness/grade_one.py --repo data/reference/fhir-none-ref/repo --upto T05

Runs the baseline suite plus the hidden tests of T01..T05 against the committed state (HEAD) and prints
per-test results: task tests of the last task, then regressions. Used to build and check the
post-hoc reference solutions (DEVIATIONS 2026-10-15). Exit code 0 if the last task succeeds and
nothing regressed.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).resolve().parent))
from grade import grade  # noqa: E402
from run_trajectory import load_task  # noqa: E402

HERE = Path(__file__).resolve().parent
ORDER = [f"T{i:02d}" for i in range(1, 25)]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", required=True)
    ap.add_argument("--upto", required=True, help="last task id to grade, e.g. T05")
    ap.add_argument("--domain", default="fhir")
    args = ap.parse_args()
    image = yaml.safe_load((HERE / "agent.yaml").read_text())["environment"]["image"]
    ids = ORDER[: ORDER.index(args.upto) + 1]
    tasks = [load_task(args.domain, t) for t in ids]
    g = grade(domain=args.domain, repo=Path(args.repo).resolve(), task=tasks[-1], previous_tasks=tasks[:-1], image=image)
    print(f"built={g.built} started={g.started}")
    if not (g.built and g.started):
        print(g.log_tail[-3000:])
        return 1
    for t, ok in sorted(g.task_tests.items()):
        print(("PASS " if ok else "FAIL ") + t)
    failing_reg = [t for t, ok in g.regression_tests.items() if not ok]
    print(f"regression tests: {len(g.regression_tests)} run, {len(failing_reg)} failing")
    for t in sorted(failing_reg):
        print("REGRESSION " + t)
    ok = g.success and not failing_reg
    print("RESULT", "OK" if ok else "NOT OK")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
