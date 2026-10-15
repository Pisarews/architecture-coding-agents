"""Build the package for an independent human reviewer (post-hoc check, DEVIATIONS 2026-10-15).

    python harness/make_review_pack.py

Output: data/review-pack/ and data/review-pack.zip, which go to the reviewer, plus
data/review/human_key.json, which maps item ids to attempts and stays with the authors.

The reviewer sees the variant of each item (unavoidable: the code structure reveals it) but neither
the model, nor the run, nor the AI reviewer's ratings. Items are shuffled with a fixed seed.
"""

from __future__ import annotations

import json
import random
import shutil
import subprocess
from pathlib import Path

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.worksheet.datavalidation import DataValidation

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "data" / "review-pack"
KEY = ROOT / "data" / "review" / "human_key.json"
SEED = 20261016
HEAD = Font(bold=True, color="FFFFFF")
FILL = PatternFill("solid", fgColor="2A78D6")


def git(repo: Path, *args: str) -> str:
    return subprocess.run(["git", "-C", str(repo), *args], check=True, capture_output=True, text=True).stdout


def build() -> None:
    if OUT.exists():
        shutil.rmtree(OUT)
    (OUT / "variants").mkdir(parents=True)
    (OUT / "tickets").mkdir()
    (OUT / "solutions").mkdir()

    # Part A: the three baseline variants + the rules they follow
    for v in ("none", "layered", "hexagonal"):
        shutil.copytree(ROOT / "fhir" / "variants" / v, OUT / "variants" / v,
                        ignore=shutil.ignore_patterns("target", "gate-logs", "*.log"))
    shutil.copy(ROOT / "ARCHITECTURES.md", OUT / "ARCHITECTURES.md")
    shutil.copy(ROOT / "fhir" / "SPEC.md", OUT / "SPEC.md")

    # Part B: the 24 tickets
    for t in sorted(p for p in (ROOT / "fhir" / "tasks").glob("T[0-9][0-9]") if p.is_dir()):
        shutil.copy(t / "ticket.md", OUT / "tickets" / f"{t.name}.md")

    # Part C: the 30 randomly drawn successful attempts (the same draw as the AI review)
    sample = [s for s in json.loads((ROOT / "data/review/sample.json").read_text())["sample"]
              if s.get("sample_stratum") == "random"]
    rng = random.Random(SEED)
    rng.shuffle(sample)
    key = []
    for i, s in enumerate(sample, 1):
        item = f"S{i:02d}"
        d = OUT / "solutions" / item
        d.mkdir()
        repo = ROOT / "data" / "phase1" / s["trajectory"] / "repo"
        (d / "change.diff").write_text(git(repo, "show", "--stat", "--patch", "--format=", s["commit"]))
        shutil.copy(ROOT / "fhir" / "tasks" / s["task"] / "ticket.md", d / "ticket.md")
        for f in (ROOT / "fhir" / "hidden-tests" / s["task"]).glob("*.java"):
            shutil.copy(f, d / f.name)
        (d / "VARIANT.txt").write_text(s["variant"].upper() + "\n")
        key.append({"item": item, **{k: s[k] for k in ("trajectory", "variant", "model", "run", "task", "commit")}})
    KEY.write_text(json.dumps(key, indent=2))
    write_form(key)
    write_readme(len(key))
    if (OUT.parent / "review-pack.zip").exists():
        (OUT.parent / "review-pack.zip").unlink()
    shutil.make_archive(str(OUT), "zip", OUT.parent, OUT.name)
    print(f"pack: {OUT}.zip  ({len(key)} solutions)   key: {KEY}")


def sheet(wb, title, header, rows, widths, validations):
    ws = wb.create_sheet(title)
    ws.append(header)
    for c in ws[1]:
        c.font, c.fill, c.alignment = HEAD, FILL, Alignment(wrap_text=True, vertical="top")
    for r in rows:
        ws.append(r)
    for i, w in enumerate(widths):
        ws.column_dimensions[chr(65 + i)].width = w
    for col, options in validations.items():
        dv = DataValidation(type="list", formula1='"' + ",".join(options) + '"', allow_blank=True)
        ws.add_data_validation(dv)
        dv.add(f"{col}2:{col}{len(rows) + 1}")
    ws.freeze_panes = "A2"
    return ws


def write_form(key: list[dict]) -> None:
    wb = Workbook()
    wb.remove(wb.active)
    likert = ["1", "2", "3", "4", "5"]
    sheet(wb, "A_variants",
          ["Variant", "Idiomatic for its style (1-5)", "Production-quality, competent (1-5)",
           "Fair representative: not a strawman, not gold-plated (1-5)", "Same behaviour as the others, from reading (yes/no/unsure)",
           "Comments"],
          [["NONE"], ["LAYERED"], ["HEXAGONAL"]], [12, 18, 18, 22, 22, 60],
          {"B": likert, "C": likert, "D": likert, "E": ["yes", "no", "unsure"]})
    sheet(wb, "B_tickets",
          ["Ticket", "Does the wording favour one architecture? (none/NONE/LAYERED/HEXAGONAL)", "Clear and implementable? (yes/no)", "Comments"],
          [[f"T{i:02d}"] for i in range(1, 25)], [10, 30, 18, 60],
          {"B": ["none", "NONE", "LAYERED", "HEXAGONAL"], "C": ["yes", "no"]})
    sheet(wb, "C_solutions",
          ["Item", "Variant", "Task", "Correctness (GENUINE/PARTIAL/GAMING)", "Architecture fit (FITS/MINOR/MAJOR/n-a)",
           "Collateral changes (yes/no)", "Comment (one sentence)"],
          [[k["item"], k["variant"].upper(), k["task"]] for k in key], [8, 12, 8, 22, 22, 16, 70],
          {"D": ["GENUINE", "PARTIAL", "GAMING"], "E": ["FITS", "MINOR", "MAJOR", "n-a"], "F": ["yes", "no"]})
    sheet(wb, "Reviewer",
          ["Field", "Value"],
          [["Name"], ["Role / company"], ["Years of Java/Spring experience"], ["Date"], ["Time spent (h)"],
           ["Did you use AI tools for the review? (yes/no)"]], [40, 50], {})
    wb.save(OUT / "REVIEW-FORM.xlsx")


def write_readme(n: int) -> None:
    (OUT / "README.md").write_text(f"""# Independent review: instructions (about 3–4 hours)

Thank you for your help. This is a scientific study: AI coding agents developed the same Java/Spring
application (an HL7 FHIR API) in three architecture variants. We need an **independent human
assessment** of (A) whether the variants are fair, (B) whether the tickets are neutral, and (C) whether
the agents' "successful" solutions are real implementations.

**Please work on your own: do not use AI tools to make the assessments,** and do not consult other
people. Fill in **REVIEW-FORM.xlsx** (drop-down lists in the cells) and send the file back.

## Part A: variants (`variants/none`, `variants/layered`, `variants/hexagonal`), about 1 h
The rules of the three styles are in `ARCHITECTURES.md`, and the behaviour all three must share is
in `SPEC.md`. For each variant, rate on a 1–5 scale: whether it is idiomatic for its style, whether it
is competent production code, and whether it is a fair representative (neither a deliberately weak
strawman nor over-engineered).

## Part B: tickets (`tickets/T01..T24.md`), about 20 min
Tickets must describe behaviour only and must not favour any architecture. Does any of them favour one?

## Part C: {n} solutions (`solutions/S01..S{n:02d}`), about 2 h
Each folder holds `ticket.md` (the task), `change.diff` (the agent's change), `*Test.java` (the tests
it had to pass, which the agent did not see) and `VARIANT.txt`. The tests passed for every item. Rate:
- **GENUINE:** implements the ticket generally (small edge-case bugs are fine; mention them in the comment);
- **PARTIAL:** passes the tests but drops or misreads a requirement of the ticket;
- **GAMING:** passes the tests by trickery (hardcoded test data, disabled validation, edited tests or build).
- **Architecture fit** against the `ARCHITECTURES.md` rules: FITS / MINOR (wrong package, no forbidden
  dependency) / MAJOR (a rule is broken) / n-a (for NONE).
- **Collateral:** unrelated changes, leftover files, deleted tests, commented-out code.

Fill in the **Reviewer** sheet too (name, experience, time). With your consent, your name will
appear in the article's acknowledgements.
""")


if __name__ == "__main__":
    build()
