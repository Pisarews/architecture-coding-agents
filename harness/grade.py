"""Grade one repository snapshot against the hidden tests, in an offline sandbox container.

The snapshot is built, started, and the full black-box suite runs against it: the baseline
acceptance tests plus the hidden tests of every task up to and including the current one. Each test
is then classified on the host:

  task        hidden tests of the current task          -> success
  regression  earlier tasks' hidden tests + baseline,    -> regression flag
              minus baseline tests superseded so far

Nothing here is ever visible to the agent.
"""

from __future__ import annotations

import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MOCK_PORT = 18080


@dataclass
class GradeResult:
    built: bool
    started: bool
    task_tests: dict[str, bool] = field(default_factory=dict)
    regression_tests: dict[str, bool] = field(default_factory=dict)
    log_tail: str = ""

    @property
    def success(self) -> bool:
        return self.built and self.started and bool(self.task_tests) and all(self.task_tests.values())

    @property
    def regressions(self) -> list[str]:
        return sorted(t for t, ok in self.regression_tests.items() if not ok)

    def to_dict(self) -> dict:
        return {
            "built": self.built,
            "started": self.started,
            "success": self.success,
            "task_passed": sum(self.task_tests.values()),
            "task_total": len(self.task_tests),
            "regressions": self.regressions,
            "regression_total": len(self.regression_tests),
            "failed_task_tests": sorted(t for t, ok in self.task_tests.items() if not ok),
        }


_SCRIPT = r"""
set -u
cd /grade/app
if ! mvn -o -B -q package -DskipTests > /grade/build.log 2>&1; then echo BUILD_FAILED; exit 0; fi
java -jar $(ls target/*.jar | grep -v original | head -1) --server.port=8080 __APP_ARGS__ > /grade/server.log 2>&1 &
for i in $(seq 1 90); do
  curl -sf http://localhost:8080/fhir/metadata > /dev/null 2>&1 && break
  sleep 1
done
curl -sf http://localhost:8080/fhir/metadata > /dev/null 2>&1 || { echo START_FAILED; exit 0; }
cd /grade/acc
mvn -o -B -q test -Dbase.url=http://localhost:8080 -Dmock.port=__MOCK_PORT__ > /grade/test.log 2>&1
echo TESTS_DONE
"""


def grade(
    *,
    domain: str,
    repo: Path,
    task: dict,
    previous_tasks: list[dict],
    image: str,
) -> GradeResult:
    """`task`/`previous_tasks` are parsed meta.yaml dicts."""
    tasks_dir = ROOT / domain / "hidden-tests"
    with tempfile.TemporaryDirectory(prefix="grade-") as tmp:
        work = Path(tmp)
        # snapshot = committed state only (what the agent left behind, minus build output)
        subprocess.run(["git", "-C", str(repo), "archive", "--format=tar", "-o", str(work / "app.tar"), "HEAD"], check=True)
        (work / "app").mkdir()
        subprocess.run(["tar", "-xf", str(work / "app.tar"), "-C", str(work / "app")], check=True)

        acc = work / "acc"
        shutil.copytree(ROOT / domain / "acceptance", acc, ignore=shutil.ignore_patterns("target"))
        test_pkg = acc / "src/test/java/org/archagents" / domain / "acceptance"
        for t in [*previous_tasks, task]:
            for f in (tasks_dir / t["id"]).glob("*.java"):
                shutil.copy(f, test_pkg / f.name)

        # app configuration accumulates: earlier integration tasks' hidden tests still run as regression
        args = [a for t in [*previous_tasks, task] for a in t.get("app_args", [])]
        app_args = " ".join(dict.fromkeys(a.replace("{MOCK_PORT}", str(MOCK_PORT)) for a in args))
        script = _SCRIPT.replace("__APP_ARGS__", app_args).replace("__MOCK_PORT__", str(MOCK_PORT))
        proc = subprocess.run(
            ["docker", "run", "--rm", "--network", "none", "--memory", "3g", "--cpus", "2",
             "-v", f"{work}:/grade", image, "bash", "-c", script],
            capture_output=True, text=True, timeout=1800,
        )
        out = proc.stdout
        if "BUILD_FAILED" in out:
            return GradeResult(False, False, log_tail=_tail(work / "build.log"))
        if "START_FAILED" in out:
            return GradeResult(True, False, log_tail=_tail(work / "server.log"))

        results = _parse_reports(acc / "target" / "surefire-reports")
        if not results:
            return GradeResult(True, True, log_tail=_tail(work / "test.log") + proc.stderr[-2000:])

    current = set(task.get("hidden_tests", []))
    superseded = {s for t in [*previous_tasks, task] for s in t.get("supersedes", [])}
    res = GradeResult(True, True)
    for name, ok in results.items():
        cls, method = name.split("#", 1)
        if cls in current:
            res.task_tests[name] = ok
        elif name not in superseded and cls not in superseded:
            res.regression_tests[name] = ok
    return res


def _parse_reports(reports: Path) -> dict[str, bool]:
    out: dict[str, bool] = {}
    for xml in sorted(reports.glob("TEST-*.xml")):
        for case in ET.parse(xml).getroot().iter("testcase"):
            cls = case.get("classname", "").rsplit(".", 1)[-1]
            failed = any(child.tag in ("failure", "error") for child in case)
            skipped = any(child.tag == "skipped" for child in case)
            if not skipped:
                method = case.get("name", "").split("(", 1)[0]
                out[f"{cls}#{method}"] = not failed
    return out


def _tail(p: Path, n: int = 3000) -> str:
    return p.read_text(errors="replace")[-n:] if p.exists() else ""
