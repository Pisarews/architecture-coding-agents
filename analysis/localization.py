"""Exploratory localization and command-mix analysis of agent transcripts (PREREGISTRATION §6.6).

    python analysis/localization.py data/phase1

For every attempt: the bash commands the agent ran, classified by keyword (read, search, edit,
build/test, git, other); the Java files it read before its first edit; and the main-code files the
graded commit changed. Localization recall = share of changed main files (matched by file name)
that the agent had read before its first edit. Descriptive only; no tests. Writes
<run>/localization.json and prints per-model, per-variant means.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
from collections import Counter, defaultdict
from pathlib import Path

JAVA = re.compile(r"([A-Za-z0-9_]+\.java)\b")
EDIT = re.compile(r"sed -i|cat\s*>|cat <<|tee |python3? -c .*open\(.*['\"]w|perl -pi|apply_patch|>\s*src/")


def category(cmd: str) -> str:
    if re.search(r"\bmvn\b|mvnw", cmd):
        return "build_test"
    if EDIT.search(cmd):
        return "edit"
    if re.search(r"\bgit\b", cmd):
        return "git"
    if re.search(r"\b(grep|find|rg|ls|tree)\b", cmd):
        return "search"
    if re.search(r"\b(cat|head|tail|sed -n|nl|wc)\b", cmd):
        return "read"
    return "other"


def commands(traj: Path) -> list[str]:
    out = []
    for m in json.loads(traj.read_text())["messages"]:
        for tc in m.get("tool_calls") or []:
            try:
                out.append(json.loads(tc["function"]["arguments"]).get("command", ""))
            except (ValueError, KeyError, TypeError):
                continue
    return out


def changed_main(repo: Path, commit: str) -> set[str]:
    names = subprocess.run(["git", "-C", str(repo), "show", "--name-only", "--format=", commit],
                           capture_output=True, text=True).stdout.split()
    return {Path(n).name for n in names if n.startswith("src/main/") and n.endswith(".java")}


def main(run: Path) -> None:
    rows = [json.loads(l) for l in (run / "results.jsonl").read_text().splitlines() if l.strip()]
    per = []
    for r in rows:
        traj = run / r["trajectory"] / f"{r['task']}.traj.json"
        if not traj.exists():
            continue
        cmds = commands(traj)
        cats = Counter(category(c) for c in cmds)
        first_edit = next((i for i, c in enumerate(cmds) if category(c) == "edit"), len(cmds))
        read_before = {f for c in cmds[:first_edit] if category(c) in ("read", "search")
                       for f in JAVA.findall(c)}
        changed = changed_main(run / r["trajectory"] / "repo", r["commit"])
        per.append({"trajectory": r["trajectory"], "task": r["task"], "variant": r["variant"],
                     "model": r["model"], "success": bool(r["success"]), "n_commands": len(cmds),
                     "categories": dict(cats), "commands_before_first_edit": first_edit,
                     "java_read_before_edit": len(read_before), "changed_main": len(changed),
                     "recall": (len(changed & read_before) / len(changed)) if changed else None})
    summary = {}
    for model in ("strong", "weak"):
        for v in ("none", "layered", "hexagonal"):
            g = [p for p in per if p["model"] == model and p["variant"] == v]
            if not g:
                continue
            tot = Counter()
            for p in g:
                tot.update(p["categories"])
            n = sum(tot.values())
            rec = [p["recall"] for p in g if p["recall"] is not None]
            summary[f"{model}/{v}"] = {
                "attempts": len(g),
                "commands_per_attempt": round(n / len(g), 1),
                "share": {k: round(tot[k] / n, 3) for k in ("read", "search", "edit", "build_test", "git", "other")},
                "commands_before_first_edit": round(sum(p["commands_before_first_edit"] for p in g) / len(g), 1),
                "java_files_read_before_edit": round(sum(p["java_read_before_edit"] for p in g) / len(g), 1),
                "localization_recall": round(sum(rec) / len(rec), 3) if rec else None,
                "attempts_with_changed_main": len(rec),
            }
    (run / "localization.json").write_text(json.dumps({"summary": summary, "attempts": per}, indent=1))
    print(json.dumps(summary, indent=1))


if __name__ == "__main__":
    main(Path(sys.argv[1]))
