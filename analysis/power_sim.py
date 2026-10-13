"""Sensitivity analysis for the Phase 1 design (PREREGISTRATION §5.3).

Simulates task-success data for two architecture arms under the Phase 1 design and estimates the
power of the primary test (§6.1): an exact sign-flip permutation test on per-task differences in
success rate, pooled over runs and models, two-sided, alpha = 0.05. With Holm across three
contrasts, the first contrast is tested at alpha = 0.05/3; both levels are reported.

Generative model (logit scale):
    logit p = mu + task_i + traj_j + model_m + delta * HEX
    task_i ~ N(0, sd_task)   - tasks differ a lot in difficulty
    traj_j ~ N(0, sd_traj)   - cumulative runs drift (path dependence)
    model_m = +/- model_gap / 2

The baseline success level mu and the variance components are chosen to match the pilot and the
exploratory study (~0.75 average success; easy and hard tasks). Effects are reported as
percentage-point differences in mean success.

    python analysis/power_sim.py
"""

from __future__ import annotations

import itertools

import numpy as np

RNG = np.random.default_rng(20261012)
N_TASKS = 24  # Phase 1 design; the registered sensitivity figures use 24 tasks x 5 runs
N_MODELS = 2
SIMS = 2000
SD_TASK = 1.5
SD_TRAJ = 0.5
MODEL_GAP = 1.0
MU = 1.4  # ~0.75 mean success in the reference arm
SIGNS = (np.array(list(itertools.product([-1, 1], repeat=N_TASKS))) if N_TASKS <= 14
         else RNG.choice([-1, 1], size=(4096, N_TASKS)))  # random flips when exact enumeration is too large


def signflip_p(d: np.ndarray) -> float:
    """Exact two-sided sign-flip permutation p-value for mean(d) != 0."""
    obs = abs(d.mean())
    null = np.abs((SIGNS * d).mean(axis=1))
    return float((null >= obs - 1e-12).mean())


def simulate(delta: float, runs: int) -> tuple[float, float]:
    """Return (mean pp difference, p-value) for one simulated dataset."""
    task = RNG.normal(0, SD_TASK, N_TASKS)
    rates = {}
    for arm, eff in (("ref", 0.0), ("hex", delta)):
        succ = np.zeros(N_TASKS)
        for m in range(N_MODELS):
            model_eff = (m - 0.5) * MODEL_GAP
            for _ in range(runs):
                traj = RNG.normal(0, SD_TRAJ)
                p = 1 / (1 + np.exp(-(MU + task + traj + model_eff + eff)))
                succ += RNG.random(N_TASKS) < p
        rates[arm] = succ / (N_MODELS * runs)
    d = rates["hex"] - rates["ref"]
    return d.mean(), signflip_p(d)


def main() -> None:
    print(f"tasks={N_TASKS} models={N_MODELS} sims={SIMS} sd_task={SD_TASK} sd_traj={SD_TRAJ}")
    for runs in (3, 5, 10):
        print(f"\nruns per cell = {runs}  (attempts per arm = {N_TASKS * N_MODELS * runs})")
        print(f"{'logit delta':>11} {'mean pp diff':>13} {'power a=.05':>12} {'power a=.0167':>14}")
        for delta in (-0.4, -0.7, -1.0, -1.3, -1.6, -2.0):
            res = [simulate(delta, runs) for _ in range(SIMS)]
            pp = 100 * np.mean([r[0] for r in res])
            ps = np.array([r[1] for r in res])
            print(f"{delta:>11.1f} {pp:>12.1f}  {np.mean(ps < 0.05):>11.2f} {np.mean(ps < 0.05 / 3):>13.2f}")


if __name__ == "__main__":
    main()
