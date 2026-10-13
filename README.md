# Software Architecture and AI Coding Agents (Java)

Does software architecture change how well, and how cheaply, AI coding agents evolve a codebase?
And does the codebase stay clean while they do it?

Two Spring Boot applications from two domains — **ORDERS** (order management) and **FHIR**
(clinical data, HL7 FHIR R4 REST facade) — each exist in three functionally equivalent
variants: **NONE**, **LAYERED**, and **HEXAGONAL**. An AI coding agent completes 24 development
tasks per domain in sequence in each variant, building on its own previous changes. After every task we
measure success (hidden acceptance tests), cost (tokens, steps, time), and architectural
erosion (cycles, coupling, ArchUnit conformance, static analysis).

- Preregistration: [PREREGISTRATION.md](PREREGISTRATION.md) (frozen copy on OSF: `TODO`)
- OSF project: `TODO https://osf.io/XXXXX/`
- Open-science workflow: [OSF-SETUP.md](OSF-SETUP.md)
- Variant rules: [ARCHITECTURES.md](ARCHITECTURES.md) · FHIR contract: [fhir/SPEC.md](fhir/SPEC.md)
- Related repositories, tools, and training-exposure probes: [RELATED-REPOS.md](RELATED-REPOS.md)
- Equivalence gate: `scripts/gate.sh <domain> <variant>`
- Predecessor study (exploratory, Python): *The Indirection Tax*, in the parent directory of this repo

## Planned layout

```
orders/ and fhir/      one subtree per domain, same layout:
  variants/
    none/          Spring Boot app, no explicit architecture
    layered/       web → service → repository
    hexagonal/     domain / application (ports) / adapters / config
  contract/        ORDERS: OpenAPI spec · FHIR: CapabilityStatement (identical external behavior)
  seed/            synthetic data (FHIR: Synthea)
  tasks/
    T01..T24/      ticket.md (behavior-only), meta.yaml (stratum, depends_on)
    pilot/         P1..P3, harness debugging only, never analyzed
  hidden-tests/    acceptance tests per task (kept out of agent workspace; private until data collection ends)
  schema/          Flyway baseline, byte-identical in every variant
  acceptance/      black-box baseline suite (equivalence gate)
harness/         trajectory runner, agent adapter, metric extraction
data/            raw transcripts, per-task snapshots, metrics.jsonl
analysis/        frozen analysis scripts (written before data collection)
DEVIATIONS.md    every change after preregistration, dated
```

## Status

- [ ] Resolve DECIDE items in the preregistration
- [x] FHIR: 3 baseline variants pass the equivalence gate (see fhir/EQUIVALENCE.md)
- [ ] ORDERS: 3 baseline variants + equivalence gate
- [x] FHIR: 24 tasks with hidden tests, validated on all 3 baselines (harness/validate_tasks.py)
- [ ] ORDERS tasks
- [x] Harness (mini-swe-agent 2.4.6, Docker sandbox, grader, night/day queue) + pilot P1–P3 (18 attempts, $0.55)
- [ ] Erosion metrics pipeline · shakedown trajectory · frozen analysis script · independent review
- [x] Sensitivity analysis (analysis/power_sim.py)
- [ ] Freeze and register on OSF
- [ ] Phase 1 data collection (FHIR, 30 trajectories, 720 attempts)
- [ ] Analysis, preprint

## License

Code: MIT. Text and data: CC-BY 4.0.
