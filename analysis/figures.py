"""Paper figures from a finished run directory (written before Phase 1 data were seen; tested on the pilot).

    python analysis/figures.py data/phase1 paper/figures

Reads <run>/results.jsonl and <run>/erosion.jsonl. Produces vector PDFs (+ PNG previews):
    fig_success.pdf      success rate per architecture, one panel per model, 95% bootstrap CI over tasks
    fig_cost.pdf         geometric-mean tokens per attempt per architecture, one panel per model
    fig_strata.pdf       per-stratum success rate per architecture (pooled models)
    fig_erosion.pdf      erosion index over task position, mean per architecture with 95% band
    fig_conformance.pdf  own-architecture conformance violations over task position (LAYERED, HEXAGONAL)

Colours: validated categorical palette (slots 1-3), every series also direct-labelled and marker-coded.
"""

from __future__ import annotations

import json
import math
import sys
from collections import defaultdict
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
from matplotlib.ticker import MaxNLocator  # noqa: E402

sys.path.insert(0, str(Path(__file__).resolve().parent))
from analyze import erosion_index, erosion_table  # noqa: E402

ARMS = ["none", "layered", "hexagonal"]
LABEL = {"none": "NONE", "layered": "LAYERED", "hexagonal": "HEXAGONAL"}
COLOR = {"none": "#2a78d6", "layered": "#eb6834", "hexagonal": "#1baf7a"}
MARKER = {"none": "o", "layered": "s", "hexagonal": "D"}
MODELS = [("strong", "Strong model (Laguna S 2.1)"), ("weak", "Weak model (Qwen3-Coder-30B)")]
INK, MUTED, GRID, SURFACE = "#0b0b0b", "#52514e", "#e6e5e0", "#fcfcfb"
RNG = np.random.default_rng(20261012)

plt.rcParams.update({
    "font.family": "sans-serif", "font.size": 9, "axes.edgecolor": MUTED, "axes.labelcolor": INK,
    "xtick.color": MUTED, "ytick.color": MUTED, "axes.spines.top": False, "axes.spines.right": False,
    "axes.grid": True, "grid.color": GRID, "grid.linewidth": 0.6, "figure.facecolor": SURFACE,
    "axes.facecolor": SURFACE, "savefig.facecolor": SURFACE, "legend.frameon": False,
})


def load(run: Path):
    res = [json.loads(l) for l in (run / "results.jsonl").read_text().splitlines() if l.strip()]
    res = [r for r in res if not r.get("missing")]
    ero_f = run / "erosion.jsonl"
    ero = [json.loads(l) for l in ero_f.read_text().splitlines() if l.strip()] if ero_f.exists() else []
    return res, ero


def task_means(rows, value):
    by = defaultdict(list)
    for r in rows:
        by[r["task"]].append(value(r))
    return np.array([np.mean(v) for v in by.values()])


def boot_ci(per_task: np.ndarray, stat=np.mean):
    if len(per_task) < 2:
        return (np.nan, np.nan)
    idx = RNG.integers(0, len(per_task), size=(5000, len(per_task)))
    s = stat(per_task[idx], axis=1)
    return np.percentile(s, 2.5), np.percentile(s, 97.5)


def dot_panel(ax, rows, value, transform=lambda x: x, ylabel=""):
    for i, arm in enumerate(ARMS):
        sub = [r for r in rows if r["variant"] == arm]
        if not sub:
            continue
        pt = task_means(sub, value)
        m = transform(pt.mean())
        lo, hi = (transform(x) for x in boot_ci(pt))
        ax.errorbar(i, m, yerr=[[m - lo], [hi - m]], fmt=MARKER[arm], color=COLOR[arm], ms=7,
                    elinewidth=2, capsize=0, zorder=3)
        ax.annotate(f"{m:.2f}" if m < 10 else f"{m:,.0f}", (i, m), xytext=(9, 0), textcoords="offset points",
                    va="center", color=INK, fontsize=8)
    ax.set_xticks(range(len(ARMS)), [LABEL[a] for a in ARMS])
    ax.set_xlim(-0.5, len(ARMS) - 0.3)
    ax.set_ylabel(ylabel)


def fig_success(res, out):
    fig, axes = plt.subplots(1, 2, figsize=(7, 2.8), sharey=True)
    for ax, (m, title) in zip(axes, MODELS):
        dot_panel(ax, [r for r in res if r["model"] == m], lambda r: float(r["success"]), ylabel="Task success rate")
        ax.set_title(title, fontsize=9, color=INK)
        ax.set_ylim(0, 1)
    fig.tight_layout()
    save(fig, out / "fig_success")


def fig_cost(res, out):
    fig, axes = plt.subplots(1, 2, figsize=(7, 2.8), sharey=True)
    for ax, (m, title) in zip(axes, MODELS):
        dot_panel(ax, [r for r in res if r["model"] == m],
                  lambda r: math.log(r["prompt_tokens"] + r["completion_tokens"] + 1),
                  transform=lambda x: math.exp(x) / 1000, ylabel="Tokens per attempt (k, geometric mean)")
        ax.set_title(title, fontsize=9, color=INK)
        ax.set_ylim(bottom=0)
    fig.tight_layout()
    save(fig, out / "fig_cost")


def fig_strata(res, out):
    strata = [("D", "Domain rule"), ("F", "Feature"), ("C", "Cross-cutting"), ("I", "Integration")]
    fig, ax = plt.subplots(figsize=(7, 2.8))
    width = 0.24
    for j, arm in enumerate(ARMS):
        xs, ys = [], []
        for i, (s, _) in enumerate(strata):
            sub = [r for r in res if r["variant"] == arm and r.get("stratum") == s]
            if sub:
                xs.append(i + (j - 1) * width)
                ys.append(np.mean([r["success"] for r in sub]))
        ax.bar(xs, ys, width=width - 0.02, color=COLOR[arm], label=LABEL[arm], zorder=3)
    ax.set_xticks(range(len(strata)), [n for _, n in strata])
    ax.set_ylim(0, 1)
    ax.set_ylabel("Task success rate")
    ax.legend(ncol=3, loc="upper right", fontsize=8)
    fig.tight_layout()
    save(fig, out / "fig_strata")


def fig_erosion(ero, out):
    if not ero:
        return
    by_traj = erosion_table(ero)
    series, _ = erosion_index(by_traj)
    fig, ax = plt.subplots(figsize=(7, 3))
    ends = []
    for arm in ARMS:
        pts = defaultdict(list)
        for t, s in series.items():
            if by_traj[t][0]["variant"] == arm:
                for x, y in s:
                    pts[x].append(y)
        if not pts:
            continue
        xs = sorted(pts)
        mean = np.array([np.mean(pts[x]) for x in xs])
        sem = np.array([np.std(pts[x], ddof=1) / math.sqrt(len(pts[x])) if len(pts[x]) > 1 else 0 for x in xs])
        ax.plot(xs, mean, color=COLOR[arm], lw=2, marker=MARKER[arm], ms=4, zorder=3)
        ax.fill_between(xs, mean - 1.96 * sem, mean + 1.96 * sem, color=COLOR[arm], alpha=0.15, lw=0)
        ends.append((mean[-1], arm, xs[-1]))
    # direct labels at the line ends, nudged apart so they never overlap
    ends.sort()
    min_gap = 0.06 * (ax.get_ylim()[1] - ax.get_ylim()[0])
    placed = []
    for y, arm, x in ends:
        y_lab = max(y, placed[-1] + min_gap) if placed else y
        placed.append(y_lab)
        ax.annotate(LABEL[arm], (x, y), xytext=(x + 0.4, y_lab), textcoords="data",
                    va="center", color=INK, fontsize=8)
    ax.axhline(0, color=MUTED, lw=0.8)
    last = max(x for _, _, x in ends) if ends else 0
    ax.set_xlim(-0.5, last + 3)
    ax.set_xticks(range(0, last + 1, 3))
    ax.set_xlabel("Task position (0 = baseline)")
    ax.set_ylabel("Erosion index (scaled Δ from baseline)")
    fig.tight_layout()
    save(fig, out / "fig_erosion")


def fig_conformance(ero, out):
    if not ero:
        return
    by_traj = erosion_table(ero)
    fig, ax = plt.subplots(figsize=(7, 3))
    for arm in ("layered", "hexagonal"):
        pts = defaultdict(list)
        for rows in by_traj.values():
            if rows[0]["variant"] == arm:
                for r in rows:
                    pts[r["task_index"]].append(r["conformance_own"])
        if not pts:
            continue
        xs = sorted(pts)
        mean = [np.mean(pts[x]) for x in xs]
        ax.plot(xs, mean, color=COLOR[arm], lw=2, marker=MARKER[arm], ms=4, zorder=3)
        ax.annotate(LABEL[arm], (xs[-1], mean[-1]), xytext=(6, 0), textcoords="offset points",
                    va="center", color=INK, fontsize=8)
    ax.xaxis.set_major_locator(MaxNLocator(integer=True))
    ax.set_xlabel("Task position (0 = baseline)")
    ax.set_ylabel("Own-architecture rule violations")
    ax.set_ylim(bottom=0)
    fig.tight_layout()
    save(fig, out / "fig_conformance")


def save(fig, stem: Path):
    fig.savefig(stem.with_suffix(".pdf"))
    fig.savefig(stem.with_suffix(".png"), dpi=160)
    plt.close(fig)


def main(run: Path, out: Path) -> None:
    out.mkdir(parents=True, exist_ok=True)
    res, ero = load(run)
    fig_success(res, out)
    fig_cost(res, out)
    fig_strata(res, out)
    fig_erosion(ero, out)
    fig_conformance(ero, out)
    print("figures written to", out)


if __name__ == "__main__":
    main(Path(sys.argv[1]), Path(sys.argv[2]))
