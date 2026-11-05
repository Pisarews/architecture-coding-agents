"""Build the data for the web review form, comparative design (post-hoc check, DEVIATIONS 2026-10-15).

    python harness/make_review_form.py OUT_DIR

Part C shows the same ticket solved in all three variants side by side, so the reviewer judges each
solution with context. Sample: of the tasks that the strong model solved in all three variants,
`random.Random(SEED)` picks N_TASKS; for each (task, variant) one successful strong attempt is drawn,
preferring attempts already rated in the AI quality review (data/review/quality_review.jsonl) so the
human ratings can be compared with them. Attempts without an AI rating are listed in the output and
must be rated with the same rubric before the agreement is computed.

Writes OUT_DIR/pack.json, OUT_DIR/snap/<item>.json and data/review/human_key.json (kept private).
"""

from __future__ import annotations

import json
import random
import shutil
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SEED = 20261018
N_TASKS = 7
VARIANTS = ("none", "layered", "hexagonal")
SNAP_SUFFIX = (".java", ".sql", ".properties")


def git(repo: Path, *args: str) -> str:
    return subprocess.run(["git", "-C", str(repo), *args], check=True, capture_output=True, text=True).stdout


def snapshot(repo: Path, commit: str) -> dict:
    names = git(repo, "ls-tree", "-r", "--name-only", commit).splitlines()
    keep = [n for n in names if n in ("AGENTS.md", "pom.xml") or (n.startswith("src/") and n.endswith(SNAP_SUFFIX))]
    files = {n: git(repo, "show", f"{commit}:{n}") for n in keep}
    changed = {}
    for line in git(repo, "show", "--name-status", "--format=", commit).splitlines():
        status, *paths = line.split("\t")
        changed[paths[-1]] = status[0]
    return {"files": files, "changed": changed}


def main(out: Path) -> None:
    results = [json.loads(l) for l in (ROOT / "data/phase1/results.jsonl").read_text().splitlines() if l.strip()]
    rated = {(r["trajectory"], r["task"]) for r in
             (json.loads(l) for l in (ROOT / "data/review/quality_review.jsonl").read_text().splitlines() if l.strip())}
    pool: dict[str, dict[str, list]] = defaultdict(lambda: defaultdict(list))
    for r in sorted(results, key=lambda r: (r["trajectory"], r["task_index"])):
        if r["model"] == "strong" and r["success"]:
            pool[r["task"]][r["variant"]].append(r)
    tasks = sorted(t for t in pool if all(pool[t][v] for v in VARIANTS))
    rng = random.Random(SEED)
    chosen = sorted(rng.sample(tasks, N_TASKS))

    old = json.loads((out / "pack.json").read_text())
    pack = {k: old[k] for k in ("variants", "tickets", "architectures", "spec")}
    pack["groups"], key, unrated = [], [], []
    if (out / "snap").exists():
        shutil.rmtree(out / "snap")
    (out / "snap").mkdir()
    n = 0
    for gi, task in enumerate(chosen, 1):
        group = {"id": f"G{gi}", "taskId": task, "ticket": pack["tickets"][task], "items": {}}
        tests = {f.name: f.read_text() for f in sorted((ROOT / "fhir/hidden-tests" / task).glob("*.java"))}
        group["tests"] = tests
        for v in VARIANTS:
            cands = pool[task][v]
            pref = [r for r in cands if (r["trajectory"], r["task"]) in rated]
            r = rng.choice(pref or cands)
            n += 1
            item = f"S{n:02d}"
            repo = ROOT / "data/phase1" / r["trajectory"] / "repo"
            diff = git(repo, "show", "--stat", "--patch", "--format=", r["commit"])
            group["items"][v] = {"id": item, "diff": diff}
            (out / "snap" / f"{item}.json").write_text(json.dumps(snapshot(repo, r["commit"])))
            key.append({"item": item, "group": group["id"], **{k: r[k] for k in ("trajectory", "variant", "model", "run", "task", "commit")},
                        "ai_rated": (r["trajectory"], r["task"]) in rated})
            if (r["trajectory"], r["task"]) not in rated:
                unrated.append(f"{item} {r['trajectory']} {task}")
        pack["groups"].append(group)
    (out / "pack.json").write_text(json.dumps(pack))
    (ROOT / "data/review/human_key.json").write_text(json.dumps(key, indent=2))
    print("tasks:", " ".join(chosen))
    print(f"items: {len(key)}; without AI rating: {len(unrated)}")
    print("\n".join(unrated))


if __name__ == "__main__":
    main(Path(sys.argv[1]))
