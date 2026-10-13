# Deviations from the preregistration

Every change made after the preregistration was frozen (`prereg-v1`) goes here, with its date and reason.
The paper reports all entries.

| Date | Section | Change | Reason | Affects confirmatory test? |
|---|---|---|---|---|
| 2026-10-13 | §2.4 Randomization (execution) | Execution runs in parallel workers, one per run (2 at a time from 11:30, 3 at a time from 11:50; `harness/run_phase1.py`, env `PHASE1_PARALLEL`). Each worker interleaves the six architecture × model trajectories of its run, task position by task position, in seeded random order (seed 20261012 + 1000 × run). This replaces a single queue over all 30 trajectories. | Wall-clock time: a single sequential queue would take about 70 h. | No. Every worker still contains all arms, so drift is spread over all arms as intended. Outcomes are unaffected. |
