"""Pre-registration check on the untouched baseline of every variant:
- the task is not already solved: at least one hidden test fails (FAIL_TO_PASS);
- hidden tests that pass on the baseline are guards against over-reaching solutions (PASS_TO_PASS)
  and are listed for review;
- the baseline regression suite passes (nothing flaky).

    python harness/validate_tasks.py --domain fhir --tasks P1,P2,P3
"""

from __future__ import annotations

import argparse
import sys
import tempfile
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).resolve().parent))
from grade import grade  # noqa: E402
from run_trajectory import init_repo, load_task  # noqa: E402

HERE = Path(__file__).resolve().parent


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--domain", required=True)
    ap.add_argument("--tasks", required=True)
    ap.add_argument("--variants", default="none,layered,hexagonal")
    args = ap.parse_args()
    image = yaml.safe_load((HERE / "agent.yaml").read_text())["environment"]["image"]
    tasks = [load_task(args.domain, t) for t in args.tasks.split(",")]
    ok = True
    for v in args.variants.split(","):
        with tempfile.TemporaryDirectory() as tmp:
            repo = Path(tmp) / "repo"
            init_repo(args.domain, v, repo)
            for task in tasks:
                g = grade(domain=args.domain, repo=repo, task=task, previous_tasks=[], image=image)
                passed = [t for t, p in g.task_tests.items() if p]
                failing = [t for t, p in g.task_tests.items() if not p]
                good = g.built and g.started and failing and not g.success and not g.regressions
                ok &= bool(good)
                print(f"{v:9} {task['id']}: built={g.built} started={g.started} "
                      f"fail_to_pass={len(failing)} guards={passed} "
                      f"regressions={g.regressions} -> {'OK' if good else 'FAIL'}", flush=True)
                if not (g.built and g.started):
                    print(g.log_tail[-1500:])
    print("ALL OK" if ok else "VALIDATION FAILED")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
