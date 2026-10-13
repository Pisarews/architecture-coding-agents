"""Confirmatory analysis for Phase 1 (PREREGISTRATION §6). Frozen before data collection.

    python analysis/analyze.py data/phase1            # writes data/phase1/analysis.md + analysis.json

Inputs (produced by the harness):
    <run>/results.jsonl   one row per task attempt (harness/run_trajectory.py)
    <run>/erosion.jsonl   one row per commit (harness/erosion.py)

Primary tests (§6.1):
    H3 success  — sign-flip permutation test on per-task success-rate differences (pooled over runs, models)
    H1 cost     — same, on per-task differences of mean log(total tokens), all attempts
    H4          — sign-flip test on per-task interaction contrasts (weak effect − strong effect)
    H5          — per-stratum H1 contrasts; permutation of stratum labels for (F,C,I) − D
    H2 erosion  — per-trajectory OLS slope of the erosion index; label permutation across trajectories,
                  stratified by model
    H6          — as H2 on own-architecture conformance violations, LAYERED vs HEXAGONAL
Holm correction over the three architecture contrasts within each hypothesis; 95% CIs by cluster
bootstrap over tasks (10,000 resamples). Everything is seeded (20261012).
"""

from __future__ import annotations

import itertools
import json
import math
import re
import sys
from collections import defaultdict
from pathlib import Path

import numpy as np
import yaml

ROOT = Path(__file__).resolve().parent.parent

SEED = 20261012
N_FLIPS = 100_000
N_BOOT = 10_000
ARMS = ["none", "layered", "hexagonal"]
CONTRASTS = [("layered", "none"), ("hexagonal", "none"), ("hexagonal", "layered")]
EROSION_METRICS = ["cycles", "cbo_mean", "cbo_max", "lcom_mean", "dms_mean", "static_warnings", "cpd_duplicated_lines"]
GENERAL_RULES = {"noPackageCycles", "noHapiServerModules", "noFieldInjection"}
# USD per 1M tokens (input, output, cache read) at registration time; secondary cost measure only.
PRICES = {"strong": (0.09, 0.18, 0.009), "weak": (0.07, 0.28, 0.07)}


# ---------------------------------------------------------------- tests

def signflip_test(d: np.ndarray, rng: np.random.Generator) -> float:
    """Two-sided sign-flip permutation p-value for mean(d) != 0 (exact for n <= 16)."""
    d = d[~np.isnan(d)]
    n = len(d)
    if n == 0:
        return float("nan")
    obs = abs(d.mean())
    if n <= 16:
        signs = np.array(list(itertools.product([-1, 1], repeat=n)))
    else:
        signs = rng.choice([-1, 1], size=(N_FLIPS, n))
        signs[0] = 1  # include the observed assignment
    null = np.abs((signs * d).mean(axis=1))
    return float((null >= obs - 1e-12).mean())


def label_perm_test(values: np.ndarray, labels: np.ndarray, strata: np.ndarray, rng: np.random.Generator) -> float:
    """Two-sided permutation test for mean difference between label 1 and label 0, shuffling within strata."""
    def stat(lab):
        return values[lab == 1].mean() - values[lab == 0].mean()
    obs = abs(stat(labels))
    count = 0
    for _ in range(N_FLIPS // 10):
        perm = labels.copy()
        for s in np.unique(strata):
            idx = np.where(strata == s)[0]
            perm[idx] = rng.permutation(perm[idx])
        count += abs(stat(perm)) >= obs - 1e-12
    return (count + 1) / (N_FLIPS // 10 + 1)


def bootstrap_ci(d: np.ndarray, rng: np.random.Generator) -> tuple[float, float]:
    d = d[~np.isnan(d)]
    if len(d) < 2:
        return (float("nan"), float("nan"))
    means = rng.choice(d, size=(N_BOOT, len(d)), replace=True).mean(axis=1)
    return float(np.percentile(means, 2.5)), float(np.percentile(means, 97.5))


def holm(pvals: list[float]) -> list[float]:
    order = np.argsort(pvals)
    m = len(pvals)
    adj = [0.0] * m
    running = 0.0
    for rank, i in enumerate(order):
        running = max(running, min(1.0, (m - rank) * pvals[i]))
        adj[i] = running
    return adj


# ---------------------------------------------------------------- data

def load(run: Path) -> tuple[list[dict], list[dict]]:
    res = [json.loads(l) for l in (run / "results.jsonl").read_text().splitlines() if l.strip()]
    res = [r for r in res if not r.get("missing")]
    ero_f = run / "erosion.jsonl"
    ero = [json.loads(l) for l in ero_f.read_text().splitlines() if l.strip()] if ero_f.exists() else []
    return res, ero


def files_changed(shortstat: str) -> float:
    m = re.search(r"(\d+) files? changed", shortstat or "")
    return float(m.group(1)) if m else 0.0


def recomputed_cost(r: dict) -> float:
    pin, pout, pcache = PRICES.get(r["model"], (0, 0, 0))
    cached = r.get("cached_tokens", 0) or 0
    return ((r["prompt_tokens"] - cached) * pin + cached * pcache + r["completion_tokens"] * pout) / 1e6


def per_task(rows: list[dict], value, models=None) -> dict[str, dict[str, float]]:
    """task -> arm -> mean(value) over runs (and models)."""
    acc: dict = defaultdict(lambda: defaultdict(list))
    for r in rows:
        if models and r["model"] not in models:
            continue
        v = value(r)
        if v is not None:
            acc[r["task"]][r["variant"]].append(v)
    return {t: {a: float(np.mean(v)) for a, v in arms.items()} for t, arms in acc.items()}


def contrast_vector(tbl: dict, a: str, b: str, tasks: list[str]) -> np.ndarray:
    return np.array([tbl.get(t, {}).get(a, np.nan) - tbl.get(t, {}).get(b, np.nan) for t in tasks])


def contrast_block(name, tbl, tasks, rng, scale=1.0) -> dict:
    out = {}
    ps = []
    for a, b in CONTRASTS:
        d = contrast_vector(tbl, a, b, tasks) * scale
        p = signflip_test(d, rng)
        ps.append(p)
        lo, hi = bootstrap_ci(d, rng)
        out[f"{a}-{b}"] = {"mean_diff": float(np.nanmean(d)), "ci95": [lo, hi], "p": p, "n_tasks": int((~np.isnan(d)).sum())}
    for (k, v), padj in zip(out.items(), holm(ps)):
        v["p_holm"] = padj
    return {"hypothesis": name, "contrasts": out}


# ---------------------------------------------------------------- erosion

def erosion_table(ero: list[dict]) -> dict[str, list[dict]]:
    by_traj: dict = defaultdict(list)
    for e in ero:
        if e.get("compiles"):
            e = dict(e)
            e["static_warnings"] = (e.get("pmd_violations") or 0) + (e.get("spotbugs_bugs") or 0)
            own = e["variant"] if e["variant"] in ("layered", "hexagonal") else None
            if own:
                rules = e.get(f"conformance_{own}_rules") or {}
                e["conformance_own"] = sum(v for k, v in rules.items() if k not in GENERAL_RULES)
            by_traj[e["trajectory"]].append(e)
    return {t: sorted(v, key=lambda e: e["task_index"]) for t, v in by_traj.items()}


def erosion_index(by_traj: dict) -> tuple[dict[str, list[tuple[int, float]]], dict]:
    """Per §4.3: Δ from own baseline, scaled (not centered) by pooled SD of final-minus-baseline Δ."""
    finals = defaultdict(list)
    for rows in by_traj.values():
        base, last = rows[0], rows[-1]
        for m in EROSION_METRICS:
            if base.get(m) is not None and last.get(m) is not None:
                finals[m].append(last[m] - base[m])
    scale = {m: float(np.std(v, ddof=1)) for m, v in finals.items() if len(v) > 1}
    used = [m for m, s in scale.items() if s > 0]
    series = {}
    for t, rows in by_traj.items():
        base = rows[0]
        pts = []
        for r in rows:
            zs = [(r[m] - base[m]) / scale[m] for m in used if r.get(m) is not None and base.get(m) is not None]
            if zs:
                pts.append((r["task_index"], float(np.mean(zs))))
        series[t] = pts
    return series, {"metrics_used": used, "metrics_dropped_zero_spread": [m for m in EROSION_METRICS if m not in used], "scale": scale}


def slope(points: list[tuple[int, float]]) -> float:
    if len(points) < 2:
        return float("nan")
    x = np.array([p[0] for p in points], float)
    y = np.array([p[1] for p in points], float)
    if np.ptp(x) == 0:
        return float("nan")
    return float(np.polyfit(x, y, 1)[0])


def slope_tests(name: str, slopes: dict[str, float], meta: dict[str, dict], pairs, rng) -> dict:
    out, ps = {}, []
    for a, b in pairs:
        ts = [t for t in slopes if meta[t]["variant"] in (a, b) and not math.isnan(slopes[t])]
        vals = np.array([slopes[t] for t in ts])
        labs = np.array([1 if meta[t]["variant"] == a else 0 for t in ts])
        strata = np.array([meta[t]["model"] for t in ts])
        if labs.sum() == 0 or labs.sum() == len(labs):
            out[f"{a}-{b}"] = {"p": float("nan")}
            ps.append(1.0)
            continue
        p = label_perm_test(vals, labs, strata, rng)
        ps.append(p)
        out[f"{a}-{b}"] = {"mean_slope_a": float(vals[labs == 1].mean()), "mean_slope_b": float(vals[labs == 0].mean()),
                           "diff": float(vals[labs == 1].mean() - vals[labs == 0].mean()), "p": p,
                           "n_traj": [int(labs.sum()), int((1 - labs).sum())]}
    for v, padj in zip(out.values(), holm(ps)):
        v["p_holm"] = padj
    return {"hypothesis": name, "contrasts": out}



# ---------------------------------------------------------------- secondary models (§6.1) and sensitivity (§6.5)

def dependency_failed(res: list[dict]) -> dict[tuple[str, str], int]:
    """1 if any task this one declares in meta.yaml `depends_on` failed earlier in the same trajectory."""
    deps = {}
    for meta in (ROOT / "fhir" / "tasks").glob("T*/meta.yaml"):
        m = yaml.safe_load(meta.read_text())
        deps[m["id"]] = m.get("depends_on") or []
    ok = {(r["trajectory"], r["task"]): bool(r.get("success")) for r in res}
    return {(r["trajectory"], r["task"]): int(any(not ok.get((r["trajectory"], d), True) for d in deps.get(r["task"], [])))
            for r in res}


def secondary_models(res: list[dict], ero_series: dict | None, by_traj: dict | None) -> dict:
    """GLMM / LMMs for effect estimation. Failures are reported, never fatal."""
    out: dict = {}
    try:
        import pandas as pd
        import statsmodels.formula.api as smf
        from statsmodels.genmod.bayes_mixed_glm import BinomialBayesMixedGLM
    except Exception as e:  # noqa: BLE001
        return {"error": f"statsmodels unavailable: {e}"}
    df = pd.DataFrame([{**r, "succ": int(bool(r.get("success"))),
                        "logtok": math.log(r["prompt_tokens"] + r["completion_tokens"] + 1)} for r in res])
    dep = dependency_failed(res)
    df["dependency_failed"] = [dep[(t, k)] for t, k in zip(df["trajectory"], df["task"])]
    out["dependency_failed_count"] = int(df["dependency_failed"].sum())
    fixed = "C(variant, Treatment('none')) * C(model)"
    vc = {"task": "0 + C(task)", "traj": "0 + C(trajectory)"}

    def glmm(formula):
        m = BinomialBayesMixedGLM.from_formula(formula, vc, df).fit_vb()
        return {n: {"post_mean_logodds": float(mu), "post_sd": float(sd)}
                for n, mu, sd in zip(m.model.exog_names, m.fe_mean, m.fe_sd) if "variant" in n}
    try:
        out["glmm_success"] = glmm(f"succ ~ {fixed}")
    except Exception as e:  # noqa: BLE001
        out["glmm_success"] = {"error": str(e)[:300]}
    try:
        if out["dependency_failed_count"] > 0:
            out["glmm_success_with_dependency_failed"] = glmm(f"succ ~ {fixed} + dependency_failed")
        else:
            out["glmm_success_with_dependency_failed"] = "not estimable: no attempt has a failed declared dependency"
    except Exception as e:  # noqa: BLE001
        out["glmm_success_with_dependency_failed"] = {"error": str(e)[:300]}
    try:
        df["one"] = 1
        m = smf.mixedlm(f"logtok ~ {fixed}", df, groups="one", vc_formula=vc).fit(reml=True)
        ci = m.conf_int()
        out["lmm_logtokens"] = {n: {"coef": float(m.params[n]), "ci95": [float(ci.loc[n, 0]), float(ci.loc[n, 1])],
                                    "p": float(m.pvalues[n])} for n in m.params.index if "variant" in n}
    except Exception as e:  # noqa: BLE001
        out["lmm_logtokens"] = {"error": str(e)[:300]}
    if ero_series and by_traj:
        try:
            rows = [{"trajectory": t, "variant": by_traj[t][0]["variant"], "task_index": x, "idx": y}
                    for t, pts in ero_series.items() for x, y in pts]
            de = pd.DataFrame(rows)
            m = smf.mixedlm("idx ~ C(variant, Treatment('none')) * task_index", de, groups="trajectory",
                            re_formula="~task_index").fit(reml=True)
            ci = m.conf_int()
            out["lmm_erosion"] = {n: {"coef": float(m.params[n]), "ci95": [float(ci.loc[n, 0]), float(ci.loc[n, 1])],
                                      "p": float(m.pvalues[n])} for n in m.params.index if "task_index" in n}
        except Exception as e:  # noqa: BLE001
            out["lmm_erosion"] = {"error": str(e)[:300]}
    return out


# ---------------------------------------------------------------- main

def main(run: Path) -> dict:
    rng = np.random.default_rng(SEED)
    res, ero = load(run)
    tasks = sorted({r["task"] for r in res}, key=lambda t: min(r["task_index"] for r in res if r["task"] == t))
    report: dict = {"n_attempts": len(res), "n_tasks": len(tasks),
                    "n_trajectories": len({r["trajectory"] for r in res}), "tests": []}

    succ = per_task(res, lambda r: 1.0 if r.get("success") else 0.0)
    report["tests"].append(contrast_block("H3 success (pp)", succ, tasks, rng, scale=100))

    logtok = per_task(res, lambda r: math.log(r["prompt_tokens"] + r["completion_tokens"] + 1))
    report["tests"].append(contrast_block("H1 cost: log(total tokens), all attempts", logtok, tasks, rng))
    for label, fn, filt in [
        ("H1-sec log(tokens), successful attempts", lambda r: math.log(r["prompt_tokens"] + r["completion_tokens"] + 1) if r.get("success") else None, None),
        ("H1-sec model calls", lambda r: float(r["api_calls"]), None),
        ("H1-sec files modified", lambda r: files_changed(r.get("diff_shortstat", "")), None),
        ("H1-sec wall seconds", lambda r: float(r.get("wall_seconds") or 0), None),
        ("H1-sec recomputed USD", recomputed_cost, None),
    ]:
        report["tests"].append(contrast_block(label, per_task(res, fn), tasks, rng))

    # H4: interaction (weak effect − strong effect), per task
    h4 = {}
    for a, b in CONTRASTS:
        s_w = per_task(res, lambda r: 100.0 if r.get("success") else 0.0, models={"weak"})
        s_s = per_task(res, lambda r: 100.0 if r.get("success") else 0.0, models={"strong"})
        d = contrast_vector(s_w, a, b, tasks) - contrast_vector(s_s, a, b, tasks)
        h4[f"{a}-{b}"] = {"weak_minus_strong_pp": float(np.nanmean(d)), "ci95": list(bootstrap_ci(d, rng)), "p": signflip_test(d, rng)}
    report["tests"].append({"hypothesis": "H4 architecture x model (success, pp)", "contrasts": h4})

    # H5: per-stratum cost contrasts + (F,C,I) − D on HEXAGONAL−NONE
    stratum = {r["task"]: r.get("stratum") for r in res}
    per_stratum = {}
    for st in sorted({s for s in stratum.values() if s}):
        st_tasks = [t for t in tasks if stratum[t] == st]
        per_stratum[st] = contrast_block(f"H5 cost in stratum {st}", logtok, st_tasks, rng)["contrasts"]
    d = contrast_vector(logtok, "hexagonal", "none", tasks)
    multi = np.array([1 if stratum[t] in ("F", "C", "I") else 0 for t in tasks])
    ok = ~np.isnan(d)
    h5p = label_perm_test(d[ok], multi[ok], np.zeros(ok.sum()), rng) if 0 < multi[ok].sum() < ok.sum() else float("nan")
    report["tests"].append({"hypothesis": "H5 HEX−NONE cost: multi-unit strata minus D", "p": h5p,
                            "diff": float(np.nanmean(d[multi == 1]) - np.nanmean(d[multi == 0])) if ok.any() else None,
                            "per_stratum": per_stratum})

    # H2 / H6 on erosion
    if ero:
        by_traj = erosion_table(ero)
        meta = {t: {"variant": rows[0]["variant"], "model": rows[0]["model"]} for t, rows in by_traj.items()}
        series, info = erosion_index(by_traj)
        slopes = {t: slope(pts) for t, pts in series.items()}
        h2 = slope_tests("H2 erosion-index slope", slopes, meta, [("layered", "none"), ("hexagonal", "none"), ("hexagonal", "layered")], rng)
        h2["index_info"] = info
        h2["non_compiling_commits"] = {a: sum(1 for e in ero if e["variant"] == a and not e.get("compiles")) for a in ARMS}
        report["tests"].append(h2)
        conf = {t: slope([(r["task_index"], r["conformance_own"]) for r in rows])
                for t, rows in by_traj.items() if meta[t]["variant"] in ("layered", "hexagonal")}
        h6 = slope_tests("H6 conformance-violation slope", conf, meta, [("hexagonal", "layered")], rng)
        # H6 also states that violations build up (slope > 0): sign-flip test on per-trajectory slopes
        for arm in ("layered", "hexagonal", "both"):
            v = np.array([sl for t, sl in conf.items() if not math.isnan(sl) and (arm == "both" or meta[t]["variant"] == arm)])
            if len(v):
                h6["contrasts"][f"slope>0 ({arm})"] = {"mean_diff": float(v.mean()), "ci95": list(bootstrap_ci(v, rng)),
                                                      "p": signflip_test(v, rng), "n_traj": int(len(v))}
        report["tests"].append(h6)

    by_traj_all = erosion_table(ero) if ero else None
    series_all = erosion_index(by_traj_all)[0] if ero else None
    report["secondary_models"] = secondary_models(res, series_all, by_traj_all)
    (run / "analysis.json").write_text(json.dumps(report, indent=2))
    (run / "analysis.md").write_text(to_markdown(report))
    return report


def to_markdown(rep: dict) -> str:
    out = [f"# Analysis\n\nattempts={rep['n_attempts']} tasks={rep['n_tasks']} trajectories={rep['n_trajectories']}\n"]
    for t in rep["tests"]:
        out.append(f"\n## {t['hypothesis']}\n")
        if "contrasts" in t:
            out.append("| contrast | estimate | 95% CI | p | p (Holm) |\n|---|---|---|---|---|")
            for k, v in t["contrasts"].items():
                est = v.get("mean_diff", v.get("diff", v.get("weak_minus_strong_pp")))
                ci = v.get("ci95", ["", ""])
                fmt = lambda x: f"{x:.3f}" if isinstance(x, (int, float)) and not (isinstance(x, float) and math.isnan(x)) else "–"
                out.append(f"| {k} | {fmt(est)} | [{fmt(ci[0])}, {fmt(ci[1])}] | {fmt(v.get('p'))} | {fmt(v.get('p_holm'))} |")
        else:
            out.append(f"diff={t.get('diff')} p={t.get('p')}")
    sm = rep.get("secondary_models")
    if sm:
        out.append("\n## Secondary models (effect estimation)\n")
        out.append("```\n" + json.dumps(sm, indent=2)[:6000] + "\n```")
    return "\n".join(out) + "\n"


if __name__ == "__main__":
    rep = main(Path(sys.argv[1]))
    print((Path(sys.argv[1]) / "analysis.md").read_text())
