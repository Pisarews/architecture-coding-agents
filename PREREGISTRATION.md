# Preregistration

**Software Architecture and AI Coding Agents: Effects of Layered and Hexagonal Architecture on
Task Success, Cost, and Architectural Erosion in Java Projects**

Author: Tomasz Pisarewski (ORCID: `TODO`)
Template: OSF Preregistration (fields below map 1:1 to the OSF form)
Status: **FROZEN 2026-10-13** — git tag `prereg-v1`; registered on OSF before any confirmatory run.


---

## 1. Study information

### 1.1 Title
As above.

### 1.2 Description
Controlled experiment. We build two Java 21 / Spring Boot 3.5 applications from two domains:
**ORDERS** (e-commerce order management) and **FHIR** (clinical data management exposing a
subset of the HL7 FHIR R4 REST API). Each application is implemented in three functionally
equivalent variants: **NONE** (no explicit architecture), **LAYERED**
(controller–service–repository), and **HEXAGONAL** (ports and adapters). An AI coding agent
completes the same ordered sequence of development tasks in each variant, working
cumulatively on its own previous changes. A second domain lets us test whether an
architecture effect generalizes beyond one application. We measure task success with hidden acceptance
tests, cost (tokens, agent steps, wall time, files touched), and architectural erosion after
every task.

**Prior evidence (exploratory, not part of this confirmatory test).** In our earlier minimal-pair
study on small Python repositories (*The Indirection Tax*, Pisarewski 2026, preprint
`TODO arXiv id`), ports-and-adapters indirection lowered single-task success
(pass@k 1.00 → 0.73 for a 14B model, → 0.40 for a 7B model). The effect was concentrated
in tasks whose logic sat in adapter modules away from the agent's entry point, and was
larger for the weaker model. That study did not cover cumulative evolution, erosion,
realistic project size, or Java. The present study tests whether the effect holds under
those conditions.

**Closest existing design.** SlopCodeBench (arXiv 2603.24755) also has agents extend their
own code over checkpoints and measures erosion at each step. It does not manipulate
architecture while holding behavior constant, and that manipulation is what this study adds.
See `RELATED-REPOS.md` for the full list of related repositories and tools.

### 1.3 Hypotheses

Primary (confirmatory):

- **H1 — Cost.** Per-task agent cost is ordered NONE < LAYERED < HEXAGONAL.
  Primary cost measure is total tokens. Secondary cost measures are agent steps, wall time,
  and number of distinct files modified. *Directional.*
- **H2 — Erosion.** Architecture-agnostic structural erosion grows more slowly over the task
  sequence in LAYERED and HEXAGONAL than in NONE. This is the slope of the erosion index
  over task index (see §4.3). *Directional.*
- **H3 — Success.** Task success rate differs between variants. *Two-sided.* The literature
  allows two outcomes. Structure may help localization, or indirection may impose a tax.
  Our prior study predicts HEXAGONAL < NONE, but we do not treat that prediction as the
  confirmatory claim.

Secondary:

- **H4 — Capability interaction.** The success gap between HEXAGONAL and NONE is larger for the
  weaker model than for the stronger model. *Directional.*
- **H5 — Task-type interaction.** HEXAGONAL's cost penalty relative to NONE is largest for
  tasks that change several architectural units (cross-cutting, new integration), and smallest
  for tasks confined to domain rules. *Directional.*
- **H6 — Conformance.** For LAYERED and HEXAGONAL only, ArchUnit rule violations build up over
  the task sequence (slope > 0). The LAYERED-vs-HEXAGONAL difference in slope is tested
  *two-sided*.
- **H7 — Domain generality.** The architecture effects in H1–H3 have the same direction in
  ORDERS and in FHIR. We test this with the architecture × domain interaction, reporting
  per-domain effects with CIs. A significant interaction that **reverses direction** would
  falsify generality. A significant difference in size only would qualify it.

---

## 2. Design plan

### 2.1 Study type
Experiment: a computational controlled experiment with no human participants.

### 2.2 Blinding
No human participants. The agent receives no information about the study. The hidden
acceptance tests stay outside the agent workspace for the whole session. Automated
scripts compute all metrics. The analysis script is written and frozen before data
collection, so no outcome is coded by hand.

### 2.3 Study design
The study runs in two phases. **This registration covers Phase 1** (the FHIR domain). ORDERS
is built with the same method and registered separately as Phase 2, a replication. That is
where H7 (domain generality) is tested. The reason is resources: the study is self-funded with
a total API budget of about USD 30 (§5.3). Phase 1 uses all 24 FHIR tasks.

Phase 1 is a full factorial within-material design:

| Factor | Levels | Type |
|---|---|---|
| Architecture | NONE, LAYERED, HEXAGONAL | manipulated, primary |
| Model | strong, weak (§3.4) | manipulated, secondary |
| Task | T01…T24 from `fhir/tasks/TASKS.md` (6 per stratum), fixed order | repeated measure |
| Run | R1…R5 independent cumulative runs | replication |

Each (architecture × model × run) is one **trajectory**: the agent starts from the variant's
baseline commit and does the 24 tasks in order, each task on top of its own previous result.
That gives 3 × 2 × 5 = **30 trajectories** and **720 task attempts** (`harness/plans/phase1.yaml`).

### 2.4 Randomization
Task order is fixed, because the tasks are a realistic evolution of the product. Execution is
interleaved: for each task position, the harness visits all trajectories in a seeded random
order (`harness/run_queue.py`, seed 20261011). Provider-side drift, rate limiting or a model
being withdrawn therefore hits every architecture arm equally. API-served arms may run at any time. Sandbox
containers are capped at 2 CPUs and 3 GB RAM. Only a locally served model, if one is used, runs in a
nightly window (22:00–07:00).

---

## 3. Materials

### 3.1 Platform (identical for all six variants)
Java 21 (`release 21`), Spring Boot **3.5.16**, Spring Data JPA/Hibernate, H2 in-memory,
Flyway, HAPI FHIR structures 8.12.1 (FHIR only), ArchUnit 1.5.1 (test scope only).
**Why Boot 3.5 and not 4.x:** Boot 4 shipped in Nov 2025 and changed core APIs (Jackson 3,
modularized starters). It is newer than much of the candidate models' training data, so it
would add a framework-novelty confound that is unrelated to architecture. Boot 3.5 is the
most recent line that is broadly represented in training data.
Full per-domain contract: `fhir/SPEC.md`, `orders/SPEC.md`. Variant rules: `ARCHITECTURES.md`.

### 3.1a Applications
Two domains. Each baseline is about 2–4 kLOC of main code, which is large enough that a task
cannot be solved by reading one file. Both baselines should match in size within ±20%.

**ORDERS — order management.** Catalog, customers, orders, pricing and discounts, inventory,
payments status, notifications. Plain JSON REST API (OpenAPI contract).

**FHIR — clinical data management.** A FHIR R4 REST facade over our own persistence. It covers
`Patient`, `Practitioner`, `Encounter`, `Observation`, and `Appointment`: read/create/update,
a defined subset of search parameters, resource versioning (`meta.versionId`, `ETag`), and
`OperationOutcome` errors. Every variant uses HAPI FHIR **structures and parser only**
(`hapi-fhir-structures-r4`), plus the HAPI validator where a task needs it. **No HAPI JPA
server or HAPI plain server.** Those frameworks impose their own architecture and would
erase the manipulated variable. The external contract is a fixed `CapabilityStatement`
that is identical across variants.

Why these two domains differ in useful ways:
- ORDERS owns its own model. FHIR is bound to an external, standardized model.
- In FHIR, *where the standard model lives* is itself architectural. In NONE, HAPI resource
  classes run through the whole codebase. In LAYERED they stop at the service boundary. In
  HEXAGONAL the domain has its own clinical model, and FHIR mapping lives only in the web
  adapter. This is exactly the indirection the prior study found costly.
- The model's prior familiarity with each domain probably differs (FHIR is a public standard
  that appears often in training data). That is why domain is a factor and not a pooled
  replicate.

**Data.** All data is synthetic. FHIR seed data is generated with Synthea; ORDERS seed data
comes from a seeded generator. No real personal or health data is used anywhere, so every
artifact can be published.

### 3.2 Variants and equivalence
- **NONE**: idiomatic "tutorial-style" Spring. Controllers call Spring Data repositories
  directly, business logic sits where convenient (controllers/entities), and JPA entities
  appear in the API. It is realistic, not deliberately bad.
- **LAYERED**: package-by-layer `web → service → repository`, DTOs at the web boundary, and
  services that hold the business logic.
- **HEXAGONAL**: `domain` (pure Java, no Spring/JPA imports), `application` (use cases, inbound
  and outbound ports), `adapter.in.web`, `adapter.out.persistence`, `adapter.out.notification`,
  and wiring in `config`. Reference style: Hombergs, *Get Your Hands Dirty on Clean
  Architecture*.

**Equivalence gate, per domain. All of these must pass before registration:**
1. All three variants pass the same baseline acceptance suite, which runs black-box over HTTP
   against one shared contract (OpenAPI for ORDERS; `CapabilityStatement` + FHIR R4
   validation of every response for FHIR).
2. They have the same `pom.xml` dependency set (exception: ArchUnit in the test scope only),
   the same DB schema (Flyway migrations), and the same Java/Spring versions.
3. Each variant has 0 ArchUnit violations against its own rule set at baseline.
4. We report the size profile of each variant (LOC, files, packages, classes) and do not hide it.
5. No independent human reviewer was available. The variants were built by three AI agents
   in parallel from one specification. The author reviewed them, and the equivalence gate checks
   them mechanically. This is a stated limitation (§7.1). An external review, if one happens
   later, will be published as a note and logged in `DEVIATIONS.md`.

### 3.3 Tasks
24 tasks per domain (48 total), written as product tickets. They describe behavior and never
name files, classes, or layers, so all three variants of a domain get identical text. Both
domains use the same strata, 6 tasks each:

| Stratum | ORDERS example | FHIR example | Expected units touched |
|---|---|---|---|
| Domain rule change | "Orders over 500 PLN get free shipping" | "Reject body-weight Observations whose unit is not kg/g (UCUM)" | 1 |
| Feature (new endpoint / search) | "List a customer's orders with pagination" | "Support `Observation?patient=&code=&date=ge` search with a `searchset` Bundle" | 3+ |
| Cross-cutting | "Reject orders with invalid postal codes, return 422 with field errors" | "Support optimistic locking with `If-Match` on update; return 412 on version conflict" | many |
| Integration | "Notify the warehouse via HTTP webhook when an order is paid" | "POST a rest-hook notification when a critical-value Observation is created" | new adapter |

FHIR tasks never ask the agent to adopt a different FHIR server framework.

Each task comes with:
- **Hidden acceptance tests**: black-box over HTTP (RestAssured + Testcontainers or
  `@SpringBootTest` on a random port). The same file serves all variants. Each task has
  FAIL_TO_PASS tests (fail on the baseline) and may have guard tests (PASS_TO_PASS) that stop
  over-reaching solutions. `harness/validate_tasks.py` checks this on all three baselines before
  registration.
- **Dependency tag**: which earlier tasks, if any, its tests need. We design tasks to be
  independent where possible.
- **Stratum tag** for H5.

A further 3 **pilot tasks** per domain (P1–P3) are used only to debug the harness. They are
not part of the 24 and are never analyzed.

### 3.4 Agent
- **Scaffold: mini-swe-agent 2.4.6** (`pip install mini-swe-agent==2.4.6`), `DefaultAgent`,
  with a single bash tool through native tool calling. It is the scaffold behind the official
  SWE-bench Bash Only leaderboard. Its minimal loop keeps scaffold effects small, and the
  same scaffold runs both models, so scaffold and model are not confounded. Configuration:
  `harness/agent.yaml`. The prompt is identical for every condition and neutral about
  architecture.
- **Models** (`harness/models.yaml`), both open-weights, served through OpenRouter, temperature 0.2:
  - *strong*: Poolside Laguna S 2.1 (`poolside/laguna-s-2.1`, 118B total / 8B active, coding MoE,
    OpenMDW-1.1 weights), run **without extended thinking** (`reasoning.enabled = false`). With
    thinking on, the serving provider ignored both the reasoning budget and `effort: low`, and calls
    ran to the output cap without a tool call. In the shakedown, 6 of 17 attempts ended in
    format errors for that reason. With thinking off there were no format errors. This was decided
    on reliability alone, before registration; architecture results played no part,
  - *weak*: Qwen3-Coder-30B-A3B-Instruct (`qwen/qwen3-coder-30b-a3b-instruct`). This is the
    same model used in the cloud runs of the exploratory Indirection Tax study.
  Open weights mean anyone can re-run the study with the same weights after the API endpoints
  change. The serving provider is logged per call. Measured cost in the pilot is about
  USD 0.01–0.02 per attempt for both arms, thanks to provider-side prompt caching.
  *History*: we planned free (`:free`) endpoints first. `qwen/qwen3-coder:free` was withdrawn on
  2026-10-11, and `poolside/laguna-s-2.1:free` was throttled for many minutes per call. Both
  were dropped before any confirmatory data, in favour of the paid endpoint of the same model.
- **Weak-model floor rule** (decided before any confirmatory data): if the weak model solves
  fewer than 30% of pilot attempts in NONE (P1–P3), it is replaced by
  Qwen3-Coder-Next (`qwen/qwen3-coder-next`) before registration. This avoids a floor cell
  like the one in the exploratory study.
- **Provider risk**: endpoints can be throttled, re-routed or withdrawn. Calls that fail on
  infrastructure are retried and never count as agent failures (§6.3). Because of the
  interleaving (§2.4), every arm is affected equally.
- Same system prompt and same tool set in every condition. **No network access** in the
  sandbox (`docker run --network none`). Maven runs offline from a pre-warmed image
  (`harness/docker/`, image `archagents/sandbox:1`).
- Each variant contains an `AGENTS.md` file of equal length (±10%). It states the build/test
  commands and a neutral description of the package layout. It holds no task hints.
- Per-task budget: 60 model calls. Success means *solved within this budget*: the hidden tests
  pass on the committed state, whatever the stop reason. In the shakedown, the strong model used
  the full budget in 18 of 24 tasks. Cost measures (H1) are therefore right-censored at the
  budget. Censoring lowers the power of H1 but does not bias the permutation test under the null.
  There is no wall-clock limit, because provider latency and throttling must never turn into agent
  failures. Wall time is still recorded, but as a secondary cost measure only. Each shell command
  times out after 600 s.
- The agent may run the visible test suite (`mvn test`). It never sees the hidden tests,
  which run in a separate grading container (`harness/grade.py`).

---

## 4. Variables

### 4.1 Manipulated
Architecture (3 levels) and model (2 levels).

### 4.2 Measured, per task attempt
- **Success** (binary): every hidden acceptance test for the task passes **and** the project
  compiles. Recorded as well: the *regression* flag, set when any earlier task's hidden test
  that passed before now fails.
- **Cost**: total input + output tokens; agent steps (tool calls); wall time; distinct files
  read; distinct files modified; added/deleted LOC.
- **Stop reason**: finished, budget, error.

### 4.3 Measured, after each task (on the resulting commit)
Every metric is computed afterwards from the committed snapshots by `harness/erosion.py` in the
offline sandbox (image `archagents/sandbox:2`). Because each snapshot is deterministic, the values
are the same as measuring inside the loop would give. Tools and versions: `harness/EROSION.md`
(ArchUnit 1.5.1, CK 0.7.0 run with a Java 21 parser on a copy where records are rewritten as final
classes, PMD/CPD 7.28.0 with the `quickstart` ruleset, SpotBugs 4.10.4 at default effort and
medium threshold).

- **Architecture-agnostic erosion** (all variants):
  - `cycles`: number of package-level dependency cycles in `org.archagents.fhir..` (ArchUnit
    slices; the strongly-connected-component count of packages in cycles is also reported),
  - `cbo_mean`, `cbo_max`, `lcom_mean` (CK),
  - `dms_mean`: mean normalized distance from the main sequence over packages (ArchUnit metrics),
  - `static_warnings` = PMD violations + SpotBugs bugs,
  - `cpd_duplicated_lines` (CPD, minimum 100 tokens).

  **Erosion index.** For each metric and commit, Δ = value − value at the trajectory's own
  baseline commit. Each Δ is divided by the pooled standard deviation of the final-minus-baseline
  Δ of that metric across all 30 trajectories. There is no centering, so 0 still means "no change".
  The index is the mean of the scaled Δs. A metric with zero pooled spread is left out of the
  index and reported as such. The index is the primary H2 outcome, and every metric is also
  reported on its own.
- **Conformance** (H6; LAYERED and HEXAGONAL only): number of violations of that variant's own
  layer or hexagon rules from `ARCHITECTURES.md`. They are evaluated by our frozen copy of the
  rules (`ConformanceRules` in `harness/metrics-tool`), never by the possibly edited test in the
  snapshot. The general checks (field injection, HAPI server modules) and the cycle rule are not
  counted here, because cycles are already measured above. Per-rule counts are kept. A rule
  whose target package was deleted entirely counts as one violation.
- **Non-compiling commits** are recorded as `compiles = false` with missing metrics. They are
  excluded from the slope fits of H2 and H6, and their number per arm is reported.

### 4.4 Commit snapshot
After every task the harness commits the workspace, so every trajectory is a git history
that can be replayed.

---

## 5. Sampling plan

### 5.1 Existing data
None. Before registration, only harness-debugging data exists: the pilot tasks P1–P3
(`data/pilot`) and one shakedown trajectory over T01–T24 (NONE, strong model, run 0,
`data/shakedown`) that checks cumulative grading. Both are excluded from every analysis.

### 5.2 Data collection procedure
The harness runs trajectories in a fresh container per trajectory. The steps are:
check out the baseline, then for each task (inject ticket → run agent → commit → run hidden
tests for this and all previous tasks → log JSONL), then the next task. Erosion metrics are
computed afterwards from the commits (§4.3).

### 5.3 Sample size
30 trajectories and 720 task attempts (Phase 1). **Rationale: resources plus sensitivity.** The
study is self-funded, with a total API budget of about USD 30. The pilot measured about USD 0.013
per attempt for the strong arm and about USD 0.05 for the weak arm, so the forecast total is about
USD 23.

**Sensitivity analysis** (`analysis/power_sim.py`, simulated under a logistic model with task
and trajectory random effects calibrated to the pilot). The primary test (§6.1) with 24 tasks × 5
runs has 0.93 power at α = 0.05 for a 15-percentage-point success difference, and 0.86 at the
Holm-adjusted α = 0.0167. For 10 pp the power is 0.67 / 0.53. The effects in the exploratory study
were 27–60 pp. Smaller differences are reported as inconclusive, not as absent. The simulation
also showed that adding tasks raises power more than adding runs, because the task is the unit of
inference. That is why we chose 24 × 5 over 12 × 10 at the same cost.

**Cost rule (outcome-blind, fixed now):** after all 30 trajectories finish task T01–T24 of run R1, we compute the
mean cost per attempt. If the forecast total exceeds USD 30, runs R5 are dropped (4 runs per
cell). The rule looks only at spend, never at outcomes.

### 5.4 Stopping rule
Fixed N. No interim looks at outcomes.

---

## 6. Analysis plan

### 6.1 Statistical models

**Primary tests: exact, assumption-light permutation tests on the unit of inference.**
The implementation is `analysis/analyze.py`, frozen with this registration and dry-run on the pilot data.
These match the sensitivity analysis (§5.3) and the exploratory study (`analysis/stats.py`).
- **H3 (success):** for each architecture contrast (LAYERED−NONE, HEXAGONAL−NONE, HEXAGONAL−LAYERED),
  compute per-task success rates for each arm, pooled over runs and models, and take the
  difference for each of the 24 tasks. The test is a two-sided sign-flip permutation test on the
  mean difference (2^24 sign patterns, approximated with 100,000 random flips, seed 20261012).
- **H1 (cost):** the same test on per-task differences in mean `log(total tokens)` over all
  attempts. Secondary analyses: the same on successful attempts only (failures that hit the
  budget inflate cost), and on model calls, files modified, and wall time.
- **H2 (erosion):** fit an OLS slope of the erosion index over task index for each trajectory.
  Compare slopes between arms with a two-sided permutation test that shuffles architecture labels
  across trajectories, stratified by model (10 trajectories per arm).
- **H4:** per task, the interaction contrast (weak-model architecture effect − strong-model
  architecture effect, in pp of success), tested with the same sign-flip test.
- **H5:** the H1 test within each stratum (6 tasks per stratum), reported descriptively with CIs.
  The directional claim is tested on the contrast "multi-unit strata (F, C, I) minus D".
- **H6:** as H2, using the ArchUnit violation count, for LAYERED vs HEXAGONAL only.

**Secondary models (effect estimation, not the confirmatory test).**
- Logistic GLMM `success ~ architecture * model + (1 | task) + (1 | trajectory)`.
- LMM `log(tokens) ~ architecture * model + (1 | task) + (1 | trajectory)`.
- LMM `erosion_index ~ architecture * task_index + (task_index | trajectory)`.
These report adjusted effect sizes. Where they disagree with the primary tests, both are reported.

**Phase 1 has one domain, so the `domain` term and H7 are dropped. Both are tested in Phase 2.**

### 6.2 Inference criteria
α = 0.05. Within each primary hypothesis, the three architecture contrasts get a Holm
correction. Directional hypotheses (H1, H2, H4, H5) count as supported only if the
correction-adjusted test is significant **and** the effect goes in the predicted direction.
Every effect is reported with a 95% CI from a cluster bootstrap over tasks (10,000 resamples).
The GLMM/LMM estimates come with profile-likelihood CIs.

### 6.3 Data exclusion
- **Infrastructure failures** are provider 5xx errors, container crashes, and harness bugs.
  They are **not** agent failures. We re-run the same attempt up to 3 times and log the
  re-run. If more than 20% of a trajectory's attempts are infrastructure failures, the whole
  trajectory is re-run from scratch.
- We exclude nothing because of outcome values.

### 6.4 Missing data
An attempt that is still an infrastructure failure after 3 re-runs is set to missing and
reported. The mixed models are fitted on the available data.

### 6.5 Dependency between tasks
A failed task can make a later dependent task impossible. We add the covariate
`dependency_failed` (1 if any task this one depends on failed in this trajectory) to the H3
model as a sensitivity analysis. Results with and without it are reported.

### 6.6 Exploratory analyses (labeled as such)
- Localization: which files the agent reads before its first edit, and how that relates to
  where the change actually landed (replicates the "burial depth" mechanism from the prior
  study).
- Regression rate by variant.
- Qualitative coding of a stratified sample of failed trajectories.

---

## 7. Other

### 7.1 Threats to validity (pre-stated)
- **Construction bias**: one author, assisted by AI agents, built all three variants and all
  tasks, with no independent human review. Mitigations: a public reference style (buckpal), a
  mechanical equivalence gate (79 black-box tests, identical schema and dependencies), our own
  frozen ArchUnit rules, published size profiles, and full public release of every material.
- **Two applications**: two domains are better than one, but still not a sample of domains.
  Claims of generality cover only effects that hold in both (H7). We claim nothing about
  other languages, frameworks, or larger systems.
- **Domain familiarity**: models have probably seen far more FHIR than any specific
  order-management codebase. Main effects of domain are therefore not interpreted causally.
  Only the architecture × domain interaction is.
- **Model drift**: API models can change silently. We pin IDs where possible, interleave
  trajectories, and record dates and response metadata.
- **Contamination**: the materials are new and stay private until data collection ends.
  The hidden tests are never in the workspace, and web access is off.
- **Task authoring bias**: tickets are written in behavior-only language and graded by black-box
  tests that are identical for all variants. No second person reviewed them (see construction bias).

### 7.2 Deviations
Every deviation from this document after registration is logged in `DEVIATIONS.md`, with the
date and the reason, and is reported in the paper.

### 7.3 Open materials and data
All data is synthetic (Synthea for FHIR), so there are no privacy restrictions on release.
Everything is published under MIT (code) and CC-BY 4.0 (text, data): the six variants,
tasks, hidden tests, harness, agent configuration, raw agent transcripts, per-task git
snapshots, metric tables, and analysis scripts. See `OSF-SETUP.md`.
