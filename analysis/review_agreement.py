"""Compare the independent human review with the AI quality review (post-hoc check).

    python analysis/review_agreement.py path/to/REVIEW-FORM.xlsx

Prints and writes data/review/human_review_summary.json with: variant ratings (part A), ticket
neutrality (part B), and for the sampled solutions (part C; 20 since 2026-10-15, see human_key.json) the human counts, % agreement and Cohen's kappa
with the AI reviewer, for correctness and architecture fit.
"""

from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path

from openpyxl import load_workbook

ROOT = Path(__file__).resolve().parent.parent


def kappa(a: list[str], b: list[str]) -> float:
    n = len(a)
    if n == 0:
        return float("nan")
    po = sum(x == y for x, y in zip(a, b)) / n
    ca, cb = Counter(a), Counter(b)
    pe = sum(ca[k] * cb[k] for k in set(a) | set(b)) / (n * n)
    return (po - pe) / (1 - pe) if pe < 1 else 1.0


def rows(ws):
    hdr = [c.value for c in ws[1]]
    return [dict(zip(hdr, [c.value for c in r])) for r in ws.iter_rows(min_row=2) if r[0].value]


def main(xlsx: Path) -> None:
    wb = load_workbook(xlsx, data_only=True)
    key = {k["item"]: k for k in json.loads((ROOT / "data/review/human_key.json").read_text())}
    ai = {(r["trajectory"], r["task"]): r for r in
          (json.loads(l) for l in (ROOT / "data/review/quality_review.jsonl").read_text().splitlines() if l.strip())}
    out: dict = {"variants": rows(wb["A_variants"]), "reviewer": rows(wb["Reviewer"])}
    tickets = rows(wb["B_tickets"])
    out["tickets_favouring_a_variant"] = [t for t in tickets if str(t[list(t)[1]]).lower() not in ("none", "")]
    sol = rows(wb["C_solutions"])
    corr_h, corr_a, arch_h, arch_a, disagreements = [], [], [], [], []
    for s in sol:
        k = key[s["Item"]]
        a = ai[(k["trajectory"], k["task"])]
        hc = str(s["Correctness (GENUINE/PARTIAL/GAMING)"] or "").upper()
        ha = str(s["Architecture fit (FITS/MINOR/MAJOR/n-a)"] or "").upper().replace("N-A", "n/a")
        if hc:
            corr_h.append(hc)
            corr_a.append(a["correctness"])
            if hc != a["correctness"]:
                disagreements.append({"item": s["Item"], "task": k["task"], "variant": k["variant"],
                                      "human": hc, "ai": a["correctness"], "human_comment": s.get("Comment (one sentence)")})
        if ha and k["variant"] != "none":
            arch_h.append(ha)
            arch_a.append(a["architecture_fit"])
    out["solutions"] = {
        "n_rated": len(corr_h),
        "human_correctness": dict(Counter(corr_h)),
        "agreement_correctness": sum(x == y for x, y in zip(corr_h, corr_a)) / len(corr_h) if corr_h else None,
        "kappa_correctness": kappa(corr_h, corr_a),
        "human_architecture_fit": dict(Counter(arch_h)),
        "agreement_architecture": sum(x == y for x, y in zip(arch_h, arch_a)) / len(arch_h) if arch_h else None,
        "kappa_architecture": kappa(arch_h, arch_a),
        "disagreements": disagreements,
    }
    (ROOT / "data/review/human_review_summary.json").write_text(json.dumps(out, indent=2, default=str))
    print(json.dumps(out, indent=2, default=str))


if __name__ == "__main__":
    main(Path(sys.argv[1]))
