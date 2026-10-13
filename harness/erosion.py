"""Measure structural erosion on every commit of one or more trajectories (PREREGISTRATION §4.3).

    python harness/erosion.py data/pilot                       # every trajectory in a run dir
    python harness/erosion.py data/pilot/fhir-none-strong-r1   # one trajectory
    python harness/erosion.py data/pilot -j 2                  # two containers in parallel

A trajectory dir holds `repo/`, a git repository with one commit per task ("baseline", then the
task ids). Each commit is exported with `git archive` (committed state only, like grade.py),
compiled offline in the sandbox image (`mvn -o -q compile`), and measured there with
harness/metrics-tool (ArchUnit + CK), PMD, CPD and SpotBugs. One JSON row per commit is appended
to `<run dir>/erosion.jsonl`. Commits already in that file are skipped, so the script is
resumable and can be re-run while trajectories are still growing.

If the snapshot does not compile, the row has `compiles: false` and every metric is null.
See harness/EROSION.md for the definition of every column and the erosion index.
"""

from __future__ import annotations

import argparse
import concurrent.futures as cf
import datetime as dt
import json
import re
import subprocess
import sys
import tempfile
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path

DEFAULT_IMAGE = "archagents/sandbox:2"
TRAJ_RE = re.compile(r"^(?P<domain>[a-z]+)-(?P<variant>none|layered|hexagonal)-(?P<model>[a-z0-9]+)-r(?P<run>\d+)$")

# Metric columns (null when the snapshot does not compile).
METRICS = [
    "classes", "classes_incl_nested", "packages",
    "cycles", "cycles_capped", "cycle_sccs", "cycle_packages",
    "dms_mean", "dms_max", "dms_by_package",
    "ck_types", "ck_errors", "ck_files_with_records",
    "cbo_mean", "cbo_max", "lcom_mean", "wmc_sum", "loc",
    "conformance_layered", "conformance_layered_rules", "conformance_layered_empty_rules",
    "conformance_hexagonal", "conformance_hexagonal_rules", "conformance_hexagonal_empty_rules",
    "pmd_violations", "pmd_by_rule", "pmd_errors",
    "spotbugs_bugs", "spotbugs_by_category",
    "cpd_duplicated_lines", "cpd_duplications", "cpd_duplicated_tokens",
]

# Runs inside the container. /m/app is the exported snapshot, /m/out collects the raw outputs.
_SCRIPT = r"""
set -u
cd /m/app
. /opt/erosion-tools.env
echo "$PMD_VERSION" > /m/out/pmd.version
echo "$SPOTBUGS_VERSION" > /m/out/spotbugs.version
if ! mvn -o -B -q compile > /m/out/compile.log 2>&1; then echo COMPILE_FAILED; exit 0; fi
mvn -o -B -q dependency:build-classpath -Dmdep.outputFile=/m/out/cp.txt -Dmdep.includeScope=compile > /m/out/cp.log 2>&1 || echo CP_FAILED
CP=$(cat /m/out/cp.txt 2>/dev/null || true)
java -jar /opt/metrics-tool/metrics-tool.jar target/classes src/main/java > /m/out/metrics.json 2> /m/out/metrics.err || echo METRICS_FAILED
/opt/pmd/bin/pmd check --no-cache --no-progress --no-fail-on-violation --no-fail-on-error --use-version java-21 \
    -d src/main/java -R /opt/metrics-tool/pmd-ruleset.xml --aux-classpath "target/classes:$CP" \
    -f json -r /m/out/pmd.json > /m/out/pmd.log 2>&1 || echo PMD_FAILED
/opt/pmd/bin/pmd cpd --no-fail-on-violation --minimum-tokens 100 --language java \
    -d src/main/java -f xml > /m/out/cpd.xml 2> /m/out/cpd.log || echo CPD_FAILED
java -jar /opt/spotbugs/lib/spotbugs.jar -textui -quiet -effort:default -medium \
    -auxclasspath "$CP" -xml:withMessages -output /m/out/spotbugs.xml target/classes > /m/out/spotbugs.log 2>&1 || echo SPOTBUGS_FAILED
echo DONE
"""


def git(repo: Path, *args: str) -> str:
    return subprocess.run(["git", "-C", str(repo), *args], check=True, capture_output=True, text=True).stdout


def commits(repo: Path) -> list[tuple[str, str]]:
    """(sha, subject) in task order: first-parent history from the root commit."""
    out = git(repo, "log", "--first-parent", "--reverse", "--format=%H%x09%s", "HEAD")
    return [tuple(line.split("\t", 1)) for line in out.splitlines() if line.strip()]  # type: ignore[misc]


def image_id(image: str) -> str:
    return subprocess.run(["docker", "image", "inspect", image, "--format", "{{.Id}}"],
                          check=True, capture_output=True, text=True).stdout.strip()


def measure(repo: Path, sha: str, image: str) -> dict:
    """Export one commit, compile and measure it in the sandbox. Returns the metric part of a row."""
    with tempfile.TemporaryDirectory(prefix="erosion-") as tmp:
        work = Path(tmp)
        (work / "app").mkdir()
        (work / "out").mkdir()
        subprocess.run(["git", "-C", str(repo), "archive", "--format=tar", "-o", str(work / "app.tar"), sha], check=True)
        subprocess.run(["tar", "-xf", str(work / "app.tar"), "-C", str(work / "app")], check=True)
        (work / "app.tar").unlink()
        name = f"erosion-{uuid.uuid4().hex[:12]}"
        proc = subprocess.run(
            ["docker", "run", "--rm", "--name", name, "--hostname", "localhost", "--network", "none", "--cpus", "2", "--memory", "3g",
             "-v", f"{work}:/m", image, "bash", "-c", _SCRIPT],
            capture_output=True, text=True, timeout=1800,
        )
        out = work / "out"
        status = proc.stdout
        versions = {
            "pmd_version": _read(out / "pmd.version").strip() or None,
            "spotbugs_version": _read(out / "spotbugs.version").strip() or None,
        }
        if "COMPILE_FAILED" in status:
            return {"compiles": False, **versions, **{k: None for k in METRICS},
                    "log_tail": _read(out / "compile.log")[-2000:]}
        if "DONE" not in status:
            raise RuntimeError(f"measurement container failed: {proc.stdout[-1000:]} {proc.stderr[-2000:]}")
        failed = [w for w in ("CP_FAILED", "METRICS_FAILED", "PMD_FAILED", "CPD_FAILED", "SPOTBUGS_FAILED") if w in status]
        if failed:
            raise RuntimeError(f"tool failure {failed}: metrics.err={_read(out / 'metrics.err')[-1500:]} "
                               f"pmd.log={_read(out / 'pmd.log')[-1500:]} cpd.log={_read(out / 'cpd.log')[-800:]} "
                               f"spotbugs.log={_read(out / 'spotbugs.log')[-1500:]} cp.log={_read(out / 'cp.log')[-800:]}")
        row: dict = {"compiles": True, **versions}
        row.update(json.loads((out / "metrics.json").read_text()))
        row.update(_pmd(out / "pmd.json"))
        row.update(_cpd(out / "cpd.xml"))
        row.update(_spotbugs(out / "spotbugs.xml"))
        return row


def _read(p: Path) -> str:
    return p.read_text(errors="replace") if p.is_file() else ""


def _pmd(path: Path) -> dict:
    report = json.loads(path.read_text())
    by_rule: dict[str, int] = {}
    for f in report.get("files", []):
        for v in f.get("violations", []):
            by_rule[v["rule"]] = by_rule.get(v["rule"], 0) + 1
    return {
        "pmd_violations": sum(by_rule.values()),
        "pmd_by_rule": dict(sorted(by_rule.items())),
        "pmd_errors": len(report.get("processingErrors", [])),
    }


def _cpd(path: Path) -> dict:
    """Duplicated lines = distinct (file, line) pairs covered by any clone occurrence (first one included)."""
    text = path.read_text().strip()
    if not text:
        return {"cpd_duplicated_lines": 0, "cpd_duplications": 0, "cpd_duplicated_tokens": 0}
    root = ET.fromstring(text)
    ns = root.tag[: root.tag.index("}") + 1] if root.tag.startswith("{") else ""
    covered: set[tuple[str, int]] = set()
    dups = tokens = 0
    for d in root.iter(f"{ns}duplication"):
        dups += 1
        n_lines = int(d.get("lines", "0"))
        files = list(d.iter(f"{ns}file"))
        tokens += int(d.get("tokens", "0")) * max(len(files) - 1, 0)
        for f in files:
            start = int(f.get("line", "0"))
            end = int(f.get("endline", str(start + n_lines - 1)))
            rel = f.get("path", "").split("/src/main/java/", 1)[-1]
            covered.update((rel, ln) for ln in range(start, end + 1))
    return {"cpd_duplicated_lines": len(covered), "cpd_duplications": dups, "cpd_duplicated_tokens": tokens}


def _spotbugs(path: Path) -> dict:
    root = ET.parse(path).getroot()
    by_cat: dict[str, int] = {}
    for b in root.iter("BugInstance"):
        c = b.get("category", "?")
        by_cat[c] = by_cat.get(c, 0) + 1
    return {"spotbugs_bugs": sum(by_cat.values()), "spotbugs_by_category": dict(sorted(by_cat.items()))}


def trajectories(target: Path) -> tuple[Path, list[Path]]:
    """(run dir, trajectory dirs) for a run dir or a single trajectory dir."""
    if (target / "repo" / ".git").exists():
        return target.parent, [target]
    trajs = sorted(p for p in target.iterdir() if (p / "repo" / ".git").exists())
    if not trajs:
        sys.exit(f"no trajectories (*/repo with a git history) under {target}")
    return target, trajs


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("target", type=Path, help="run dir (data/<run>) or one trajectory dir")
    ap.add_argument("--image", default=DEFAULT_IMAGE)
    ap.add_argument("-j", "--jobs", type=int, default=1, help="containers in parallel (each --cpus 2)")
    ap.add_argument("--out", type=Path, help="output JSONL (default: <run dir>/erosion.jsonl)")
    args = ap.parse_args()

    run_dir, trajs = trajectories(args.target.resolve())
    out_path = args.out or run_dir / "erosion.jsonl"
    done: set[tuple[str, str]] = set()
    if out_path.exists():
        for line in out_path.read_text().splitlines():
            if line.strip():
                r = json.loads(line)
                done.add((r["trajectory"], r["commit"]))

    img = image_id(args.image)
    jobs = []
    for t in trajs:
        m = TRAJ_RE.match(t.name)
        for i, (sha, subject) in enumerate(commits(t / "repo")):
            if (t.name, sha) in done:
                continue
            head = {
                "run": run_dir.name, "trajectory": t.name, "commit": sha, "task": subject.strip(), "task_index": i,
                "domain": m["domain"] if m else None, "variant": m["variant"] if m else None,
                "model": m["model"] if m else None,
            }
            jobs.append((t / "repo", head))
    print(f"{len(trajs)} trajectories, {len(done)} commits already measured, {len(jobs)} to go -> {out_path}", flush=True)

    failures = 0
    with cf.ThreadPoolExecutor(max_workers=max(1, args.jobs)) as pool, out_path.open("a") as fh:
        futures = {pool.submit(measure, repo, head["commit"], args.image): head for repo, head in jobs}
        for fut in cf.as_completed(futures):
            head = futures[fut]
            label = f"{head['trajectory']} {head['task_index']:>2} {head['task']:<10} {head['commit'][:8]}"
            try:
                metrics = fut.result()
            except Exception as e:  # infrastructure/tool problem: not written, retried on the next run
                failures += 1
                print(f"FAIL  {label}: {e}", file=sys.stderr, flush=True)
                continue
            row = {**head, **metrics, "image": args.image, "image_id": img,
                   "measured_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds")}
            fh.write(json.dumps(row) + "\n")
            fh.flush()
            if metrics["compiles"]:
                print(f"ok    {label}: cycles={row['cycles']} cbo_mean={row['cbo_mean']} dms={row['dms_mean']} "
                      f"pmd={row['pmd_violations']} sb={row['spotbugs_bugs']} cpd={row['cpd_duplicated_lines']} "
                      f"conf L/H={row['conformance_layered']}/{row['conformance_hexagonal']}", flush=True)
            else:
                print(f"nocmp {label}", flush=True)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
