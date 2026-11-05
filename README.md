# Does Architecture Help or Hinder AI Coding Agents?

A preregistered controlled experiment. One HL7 FHIR R4 REST service (Java 21, Spring Boot 3.5) was
built in three behaviorally equivalent variants: no explicit architecture (**NONE**), **LAYERED**,
and **HEXAGONAL** (ports and adapters). Two open-weights models in the mini-swe-agent scaffold then
implemented the same 24 product tickets in a fixed order, each ticket on top of their own previous
changes. Hidden acceptance tests measure success, tokens measure cost, and an erosion index plus
ArchUnit conformance measure what happens to the code.

**Phase 1 results (576 attempts, USD 24.25 in API fees):**

- Architecture changed neither task success nor token cost. The capable model solved 56%, 57% and
  55% of tasks in NONE, LAYERED and HEXAGONAL; in HEXAGONAL it changed about twice as many files.
- Architecture did change what happened to the code. Code eroded in every arm; the layered code
  base eroded fastest, and the hexagonal one eroded significantly more slowly than the layered one
  (Holm-adjusted p = 0.031) and mostly kept its own rules, but more often left a non-building project.
- The weaker model was at the floor (2–5% success), so the capability interaction is not interpretable.

Paper: [paper/main.pdf](paper/main.pdf) (preprint).

## Preregistration and open science

- Full protocol: [PREREGISTRATION.md](PREREGISTRATION.md), frozen as git tag `prereg-v1` (commit
  `3d347cb`) and registered on OSF as https://osf.io/r5t3d (2026-10-13 09:25 UTC).
- Earlier open-ended registration of the idea: https://osf.io/w467e (2026-10-05).
- OSF project, including the raw data: https://osf.io/rneym
- Every change after the freeze, with its reason: [DEVIATIONS.md](DEVIATIONS.md)

## Repository layout

```
fhir/
  SPEC.md, EQUIVALENCE.md   behavior contract and equivalence record (clarifications C1–C13)
  variants/{none,layered,hexagonal}/   the three baseline applications
  acceptance/               79-test black-box suite (equivalence gate)
  schema/                   Flyway baseline, byte-identical in every variant
  tasks/T01..T24/           behavior-only tickets (+ pilot P1–P3, never analyzed)
  hidden-tests/T01..T24/    acceptance tests per task, never shown to the agent
ARCHITECTURES.md            rules of each style, enforced with ArchUnit
harness/                    agent runner, Docker sandbox, grader, queue, erosion metrics, review tools
analysis/                   analysis scripts (primary tests frozen before data collection), figures
paper/                      LaTeX source and PDF
scripts/gate.sh             equivalence gate: scripts/gate.sh fhir <variant>
STUDY2-GUARDRAILS.md        planned follow-up: Copilot-style guardrails against erosion
```

The raw data (transcripts, per-task git snapshots, result and erosion tables) are not in git; they
are in the OSF project. Put them under `data/` to re-run the analysis.

## Reproducing

Requirements: Docker (about 6 GB of memory for the VM), Java 21, Python 3.11, and an OpenRouter key
in `~/.config/archagents/env` for new agent runs.

```bash
pip install -r requirements.txt
scripts/gate.sh fhir hexagonal            # equivalence gate for one variant
python harness/validate_tasks.py          # hidden tests fail on the baseline and pass with the reference
python harness/run_phase1.py              # full data collection (see DEVIATIONS.md for the run history)
python harness/erosion.py data/phase1     # erosion metrics on every graded commit
python analysis/analyze.py data/phase1    # preregistered tests and secondary models
python analysis/figures.py data/phase1 paper/figures
python analysis/localization.py data/phase1   # exploratory command-mix and localization
```

## Status

- [x] FHIR domain: variants, 24 tasks with hidden tests, harness, preregistration
- [x] Phase 1 data collection, analysis and post-hoc validity checks
- [x] Preprint
- [ ] Independent human review (in progress; results will be added in a revised preprint)
- [ ] Phase 2: order-management domain
- [ ] Study 2: guardrails ([STUDY2-GUARDRAILS.md](STUDY2-GUARDRAILS.md))

## Citation and license

See [CITATION.cff](CITATION.cff). Code: MIT ([LICENSE](LICENSE)). Text and data: CC BY 4.0.
