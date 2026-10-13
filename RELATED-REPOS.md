# Related repositories

Companion to `PREREGISTRATION.md`. Curated list of public repositories that the study cites,
uses, compares against, or uses to probe training exposure.

**Last verified: 2026-10-07.** Every GitHub entry was resolved through the GitHub REST API
(`gh api repos/<owner>/<name>`) on that date. The API follows redirects, so we list the
canonical owner/name it returned. Hugging Face entries were checked through the HF Hub API.
"License" is the SPDX id GitHub detects. "n/d" means GitHub detects no license. "custom" means
GitHub reports NOASSERTION, so check the repo's LICENSE file before reuse. Unless marked
**ARCHIVED**, a repo is not archived. "Last push" is given only where it signals low
maintenance.

**Role key:** **REF** = reference style we cite · **TOOL** = used in the pipeline ·
**BASE** = baseline / comparison point · **BENCH** = benchmark · **PROBE** = training-exposure
probe material · **RW** = related work.

---

## 1. Reference architecture implementations (Java / Spring)

| Repo | What it is | Role | License |
|---|---|---|---|
| [thombergs/buckpal](https://github.com/thombergs/buckpal) | Hexagonal example from Hombergs, *Get Your Hands Dirty on Clean Architecture* (`adapter/`, `application/`, `domain`). Last push 2024-06 | **REF** canonical HEXAGONAL style (§3.2); PROBE | n/d |
| [SvenWoltmann/hexagonal-architecture-java](https://github.com/SvenWoltmann/hexagonal-architecture-java) | HappyCoders hexagonal sample, multi-module Maven | REF secondary HEXAGONAL (checks the package convention); PROBE | n/d |
| [ddd-by-examples/library](https://github.com/ddd-by-examples/library) | DDD + hexagonal library-lending application, Spring | REF HEXAGONAL with rich domain; PROBE | MIT |
| [spring-projects/spring-petclinic](https://github.com/spring-projects/spring-petclinic) | The canonical Spring Boot sample. Package-by-feature, and controllers call Spring Data repositories directly (e.g. `OwnerController` → `OwnerRepository`) | **REF** for NONE ("tutorial-style" Spring); PROBE | Apache-2.0 |
| [spring-petclinic/spring-petclinic-rest](https://github.com/spring-petclinic/spring-petclinic-rest) | REST Petclinic with `rest/ → service/ → repository/` plus `mapper/` DTOs | **REF** canonical LAYERED style; PROBE | Apache-2.0 |
| [citerus/dddsample-core](https://github.com/citerus/dddsample-core) | Evans's DDD sample (cargo), classic layered DDD (interfaces / application / domain / infrastructure) | REF LAYERED-with-domain variant; PROBE | MIT |
| [gothinkster/spring-boot-realworld-example-app](https://github.com/gothinkster/spring-boot-realworld-example-app) | RealWorld (Conduit) spec in Spring Boot. Layered with a CQRS read side. Last push 2024-07 | BASE / PROBE (a widely copied spec, so high exposure) | MIT |
| [jhipster/generator-jhipster](https://github.com/jhipster/generator-jhipster) · [jhipster/jhipster-sample-app](https://github.com/jhipster/jhipster-sample-app) | Generator for layered Spring apps (`web.rest → service → repository`) and its generated sample | PROBE (very large footprint of the LAYERED idiom on GitHub) | Apache-2.0 |
| [xmolecules/jmolecules](https://github.com/xmolecules/jmolecules) | Annotations/types for architectural concepts (DDD, layers, onion, hexagonal `@Port`/`@Adapter`) | REF / TOOL (optional: annotate variants so rules are explicit) | Apache-2.0 |
| [xmolecules/jmolecules-integrations](https://github.com/xmolecules/jmolecules-integrations) | Tech integrations. `jmolecules-archunit` ships `JMoleculesArchitectureRules` for layered, onion, **hexagonal** (verified in source) | **TOOL** candidate ready-made conformance rules (H6) | Apache-2.0 |
| [xmolecules/jmolecules-examples](https://github.com/xmolecules/jmolecules-examples) | Example projects for jMolecules | REF | Apache-2.0 |
| [spring-projects/spring-modulith](https://github.com/spring-projects/spring-modulith) | Modular-monolith support. Its examples use order/inventory modules | RW (third style we do *not* test; threat-to-validity note) | Apache-2.0 |
| [eugenp/tutorials](https://github.com/eugenp/tutorials) | Baeldung tutorial monorepo (contains layered and hexagonal Spring samples) | PROBE (high-exposure source of both idioms) | MIT |
| [Sairyss/domain-driven-hexagon](https://github.com/Sairyss/domain-driven-hexagon) | Popular hexagonal/DDD guide (TypeScript). Last push 2024-06 | RW (non-Java hexagonal exposure) | MIT |

## 2. FHIR ecosystem

| Repo | What it is | Role | License |
|---|---|---|---|
| [hapifhir/hapi-fhir](https://github.com/hapifhir/hapi-fhir) | HAPI FHIR Java library. We use **only** `hapi-fhir-structures-r4` + parser | **TOOL** (dependency, pinned version); PROBE | Apache-2.0 |
| [hapifhir/org.hl7.fhir.core](https://github.com/hapifhir/org.hl7.fhir.core) | HL7 core Java libraries and the official FHIR **validator** (validator CLI) | **TOOL** (validates every response in the acceptance suite) | Apache-2.0 |
| [hapifhir/hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) | Ready-to-run HAPI JPA server | BASE / RW (the framework we deliberately exclude, §3.1); PROBE | Apache-2.0 |
| [synthetichealth/synthea](https://github.com/synthetichealth/synthea) | Synthetic patient population simulator (FHIR R4 export) | **TOOL** (FHIR seed data) | Apache-2.0 |
| [synthetichealth/synthea-sample-data](https://github.com/synthetichealth/synthea-sample-data) | Pre-generated Synthea datasets | TOOL (optional fixed seed set) | n/d |
| [HL7/fhir](https://github.com/HL7/fhir) | Source of the FHIR specification | RW (normative contract source) | custom |
| [openmrs/openmrs-module-fhir2](https://github.com/openmrs/openmrs-module-fhir2) | Java FHIR R4 facade over OpenMRS's **own** persistence (translators between the domain and FHIR) | **BASE** (closest real-world analogue to our FHIR design); PROBE | custom (MPL-2.0 + HD) |
| [LinuxForHealth/FHIR](https://github.com/LinuxForHealth/FHIR) | LinuxForHealth (formerly IBM) Java FHIR server. Not archived, but last push 2024-04 (inactive) | BASE / PROBE | Apache-2.0 |
| [google/fhir](https://github.com/google/fhir) | Google's FHIR Protocol Buffers (Java/Go/Python) | RW / PROBE | Apache-2.0 |
| [ohs-foundation/fhir-data-pipes](https://github.com/ohs-foundation/fhir-data-pipes) | FHIR extraction/analytics pipelines (Java). **Renamed from google/fhir-data-pipes** | RW | Apache-2.0 |
| [samply/blaze](https://github.com/samply/blaze) | FHIR server with a CQL engine (Clojure) | RW (non-Java server) | Apache-2.0 |
| [microsoft/fhir-server](https://github.com/microsoft/fhir-server) | Microsoft FHIR server (C#) | RW (non-Java server) | MIT |

## 3. Order-management / e-commerce (comparison points for ORDERS)

| Repo | What it is | Role | License |
|---|---|---|---|
| [odrotbohm/spring-restbucks](https://github.com/odrotbohm/spring-restbucks) | Order lifecycle sample ("REST in Practice") on Spring, DDD + jMolecules | **BASE** (closest small-scale ORDERS analogue); PROBE | n/d |
| [microservices-patterns/ftgo-application](https://github.com/microservices-patterns/ftgo-application) | Richardson's *Microservices Patterns* FTGO (orders, kitchen, accounting) | BASE / PROBE | custom |
| [eventuate-tram/eventuate-tram-examples-customers-and-orders](https://github.com/eventuate-tram/eventuate-tram-examples-customers-and-orders) | Customers & orders with choreography sagas, Spring Boot/JPA | BASE / PROBE | custom |
| [BroadleafCommerce/BroadleafCommerce](https://github.com/BroadleafCommerce/BroadleafCommerce) | Java/Spring e-commerce framework (CE) | BASE (realistic scale) | n/d (dual-licensed, see repo) |
| [shopizer-ecommerce/shopizer](https://github.com/shopizer-ecommerce/shopizer) | Java Spring Boot e-commerce. Last push 2025-03 | BASE | Apache-2.0 |
| [macrozheng/mall](https://github.com/macrozheng/mall) | Very popular Spring Boot + MyBatis e-commerce (~85k stars) | PROBE (huge exposure of the e-commerce Spring idiom) | Apache-2.0 |
| [linlinjava/litemall](https://github.com/linlinjava/litemall) | Small Spring Boot mall | PROBE | MIT |
| [apache/ofbiz-framework](https://github.com/apache/ofbiz-framework) | Apache OFBiz ERP with order management (Java, not Spring) | RW | Apache-2.0 |

## 4. Measurement tooling

| Repo | What it is | Role | License |
|---|---|---|---|
| [TNG/ArchUnit](https://github.com/TNG/ArchUnit) | Architecture rules as JUnit tests. `Architectures.layeredArchitecture()/onionArchitecture()`, `slices().beFreeOfCycles()`, and `library.metrics.ArchitectureMetrics` (Lakos + Martin I/A/D, verified in source) | **TOOL** (conformance H6, cycles, and a JDepend replacement for distance from main sequence) | Apache-2.0 |
| [TNG/ArchUnit-Examples](https://github.com/TNG/ArchUnit-Examples) | Example rule sets incl. layered/onion | REF for our rule sets | Apache-2.0 |
| [enofex/taikai](https://github.com/enofex/taikai) | Ready-made ArchUnit rule suites (Spring, naming, etc.) | TOOL candidate (optional extra rules) | MIT |
| [mauricioaniche/ck](https://github.com/mauricioaniche/ck) | CK metrics (CBO, LCOM, WMC, …) for Java | **TOOL** (CBO, LCOM in erosion index) | Apache-2.0 |
| [pmd/pmd](https://github.com/pmd/pmd) | Static analyzer. Includes **CPD** copy-paste detector | **TOOL** (PMD warnings + CPD duplication) | custom (BSD-style) |
| [spotbugs/spotbugs](https://github.com/spotbugs/spotbugs) | FindBugs successor, bytecode bug patterns | **TOOL** | LGPL-2.1 |
| [clarkware/jdepend](https://github.com/clarkware/jdepend) | Original package metrics (Ca, Ce, I, A, D). **Unmaintained** (last push 2020-04) | TOOL (cross-check only; prefer ArchUnit metrics) | MIT |
| [jQAssistant/jqassistant](https://github.com/jQAssistant/jqassistant) | Loads code into Neo4j, rules in Cypher | TOOL alternative (graph queries over dependencies) | GPL-3.0 |
| [tushartushar/DesigniteJava](https://github.com/tushartushar/DesigniteJava) | Design/implementation smell + metric detector. Last push 2025-03 | TOOL (exploratory smell counts) | Apache-2.0 |
| [cqfn/jpeek](https://github.com/cqfn/jpeek) | Cohesion metrics (LCOM variants, CAMC, …) | TOOL alternative for cohesion | MIT |
| [usc-softarch/arcade_core](https://github.com/usc-softarch/arcade_core) | ARCADE architecture recovery / architectural-change metrics (USC). Last push 2024-03 | RW / TOOL candidate (architecture drift over commits) | custom |
| [SonarSource/sonarqube](https://github.com/SonarSource/sonarqube) | SonarQube Community, a continuous inspection platform | RW (rule set used by 2605.20049); TOOL optional | LGPL-3.0 |
| [kucherenko/jscpd](https://github.com/kucherenko/jscpd) | Language-agnostic copy/paste detector | TOOL (cross-check for CPD) | MIT |
| [tsantalis/RefactoringMiner](https://github.com/tsantalis/RefactoringMiner) | Detects refactorings in Java commit histories | TOOL (exploratory: classify agent edits per task commit) | MIT |
| [GumTreeDiff/gumtree](https://github.com/GumTreeDiff/gumtree) | AST differencing | TOOL (exploratory edit-locality analysis) | LGPL-3.0 |
| [javaparser/javaparser](https://github.com/javaparser/javaparser) | Java parser + symbol solver | TOOL (custom metrics, localization analysis) | custom (LGPL/Apache dual) |

## 5. Agent scaffolds (candidates to pin, §3.4)

"Headless" means it can run non-interactively from a script. "Per-step tokens" means usage is
logged per model call in a machine-readable form. Both columns come from the projects' docs.
**Re-check them against the exact pinned version.**

| Repo | What it is | Headless | Per-step tokens | License |
|---|---|---|---|---|
| [SWE-agent/mini-swe-agent](https://github.com/SWE-agent/mini-swe-agent) | ~100-line bash-only agent, any LLM via LiteLLM | yes (CLI + Python API + batch runner) | yes (trajectory JSON with every message and cost; raw responses carry usage) | MIT |
| [SWE-agent/SWE-agent](https://github.com/SWE-agent/SWE-agent) | Full SWE-agent (ACI tools). **Formerly princeton-nlp/SWE-agent** | yes | yes (`.traj` + `model_stats`: tokens sent/received, API calls) | MIT |
| [OpenHands/OpenHands](https://github.com/OpenHands/OpenHands) | OpenHands platform. **Formerly All-Hands-AI/OpenHands** | yes (headless mode / CLI) | yes (event stream with LLM metrics) | MIT |
| [Aider-AI/aider](https://github.com/Aider-AI/aider) | Terminal pair programmer. **Formerly paul-gauthier/aider**. Not a free tool-loop agent. Last push 2026-05 | yes (`--message`, `--yes-always`) | partial (tokens/cost per exchange in chat log) | Apache-2.0 |
| [anthropics/claude-code](https://github.com/anthropics/claude-code) | Claude Code (issues/plugins repo; the CLI itself is not open source) | yes (`-p`, `--output-format stream-json`) | yes (usage per assistant message in stream-json) | n/d (proprietary) |
| [anthropics/claude-agent-sdk-python](https://github.com/anthropics/claude-agent-sdk-python) | Programmatic driver for the Claude Code agent loop | yes | yes | MIT |
| [anomalyco/opencode](https://github.com/anomalyco/opencode) | Open-source terminal coding agent, many providers. **Formerly sst/opencode** | yes (`opencode run --format json`) | yes (nd-JSON events; `opencode stats`) | MIT |
| [openai/codex](https://github.com/openai/codex) | Codex CLI | yes (`codex exec --json`) | per turn (`turn.completed` usage) | Apache-2.0 |
| [google-gemini/gemini-cli](https://github.com/google-gemini/gemini-cli) | Gemini CLI | yes (`-p`, `--output-format json`) | per session/turn (stats object) | Apache-2.0 |
| [QwenLM/qwen-code](https://github.com/QwenLM/qwen-code) | Qwen Code agent (Gemini CLI derivative). Natural pairing for an open-weights weak model | yes | as Gemini CLI | Apache-2.0 |
| [aaif-goose/goose](https://github.com/aaif-goose/goose) | Extensible local agent. **Formerly block/goose** | yes (`goose run`) | check at pin time | Apache-2.0 |
| [harbor-framework/harbor](https://github.com/harbor-framework/harbor) | Containerized agent-evaluation harness that wraps many of the above. Used by 2605.20049 (v0.4.0) | yes | via wrapped agent | Apache-2.0 |
| [OpenAutoCoder/Agentless](https://github.com/OpenAutoCoder/Agentless) | Non-agentic localize→repair pipeline. Last push 2024-12 | yes | yes | MIT |

## 6. Benchmarks and related empirical work

| Repo / resource | What it is | Role | License |
|---|---|---|---|
| [SWE-bench/SWE-bench](https://github.com/SWE-bench/SWE-bench) | Issue-resolution benchmark + harness. **Formerly princeton-nlp/SWE-bench** | RW / BENCH | MIT |
| [HF: princeton-nlp/SWE-bench_Verified](https://huggingface.co/datasets/princeton-nlp/SWE-bench_Verified) | Human-validated 500-instance subset | RW (anchor for model capability claims) | — |
| [multi-swe-bench/multi-swe-bench](https://github.com/multi-swe-bench/multi-swe-bench) · [HF: ByteDance-Seed/Multi-SWE-bench](https://huggingface.co/datasets/ByteDance-Seed/Multi-SWE-bench) | Multilingual issue resolution, including Java | BENCH (Java capability of candidate models) | Apache-2.0 |
| [multi-swe-bench/multi-swe-bench-env](https://github.com/multi-swe-bench/multi-swe-bench-env) · [HF: Daoguang/Multi-SWE-bench](https://huggingface.co/datasets/Daoguang/Multi-SWE-bench) | Eval env and data of **SWE-bench-java** (arXiv 2408.14354). Last push 2024-09 | BENCH (Java-specific prior) | MIT |
| [amazon-science/SWE-PolyBench](https://github.com/amazon-science/SWE-PolyBench) | Multi-language repo-level benchmark (incl. Java) | BENCH | MIT |
| [scaleapi/SWE-bench_Pro-os](https://github.com/scaleapi/SWE-bench_Pro-os) | SWE-Bench Pro, long-horizon, contamination-resistant | BENCH / RW | MIT |
| [SWE-EVO/SWE-EVO](https://github.com/SWE-EVO/SWE-EVO) | SWE-EVO (arXiv 2512.18470): long-horizon software evolution from release notes | **RW** (closest "evolution" benchmark; Python) | MIT |
| [SprocketLab/slop-code-bench](https://github.com/SprocketLab/slop-code-bench) | SlopCodeBench (arXiv 2603.24755): agents extend their *own* code over checkpoints, with erosion and verbosity measured per step | **RW** (closest design to our cumulative + erosion protocol) | MIT |
| [UniPat-AI/RoadmapBench](https://github.com/UniPat-AI/RoadmapBench) | RoadmapBench (arXiv 2605.15846): long-horizon version-upgrade tasks | RW | MIT |
| [ShadeCloak/SaaSBench](https://github.com/ShadeCloak/SaaSBench) | SaaSBench (arXiv 2605.17526): long-horizon full-stack enterprise SaaS | RW | n/d |
| [microsoft/FEA-Bench](https://github.com/microsoft/FEA-Bench) | Repo-level feature implementation (ACL'25) | RW (feature-type tasks, cf. H5) | MIT |
| [commit-0/commit0](https://github.com/commit-0/commit0) | Library generation from scratch | RW | MIT |
| [SalesforceAIResearch/LoCoBench](https://github.com/SalesforceAIResearch/LoCoBench) | Long-context software-engineering benchmark | RW | Apache-2.0 |
| [harbor-framework/terminal-bench](https://github.com/harbor-framework/terminal-bench) | Terminal-Bench, current. (`laude-institute/terminal-bench` now redirects to `harbor-framework/terminal-bench-1`) | RW | Apache-2.0 |
| [rjust/defects4j](https://github.com/rjust/defects4j) · [gitbugactions/gitbug-java](https://github.com/gitbugactions/gitbug-java) | Real Java bug datasets (GitBug-Java targets post-cutoff bugs) | RW (Java + contamination-aware design) | MIT |
| [openai/SWELancer-Benchmark](https://github.com/openai/SWELancer-Benchmark) | SWE-Lancer freelance-task benchmark. **ARCHIVED** | RW | n/d |
| [codescene-research/ai-friendliness-forge-2026](https://github.com/codescene-research/ai-friendliness-forge-2026) | Code for arXiv **2601.02200** (Borg et al., "Code for Machines, Not Just Humans", CodeHealth vs LLM refactoring, FORGE'26). Link taken from the paper | **RW** | n/d |

Related papers **without a public code repo** that we could find (cite as RW only):
- arXiv **2605.20049**, Trivedi & Schmitt (SonarSource), *Does Code Cleanliness Affect Coding
  Agents? A Controlled Minimal-Pair Study*. Built on `harbor-framework/harbor` v0.4.0. Its
  minimal pairs include Java bases (`apache/commons-bcel`, `Netflix/genie`). No artifact repo
  is linked in the paper. It is the closest methodological precedent: minimal pairs, hidden
  tests, token cost.
- arXiv **2604.07502**, Ustynov, *Beyond Human-Readable: Rethinking Software Engineering
  Conventions for the Agentic Development Era*. No repo linked.

## 7. Training-exposure / contamination tooling

| Repo / resource | What it is | Role | License |
|---|---|---|---|
| [HF: bigcode/the-stack-v2](https://huggingface.co/datasets/bigcode/the-stack-v2) · [bigcode-project/the-stack-v2](https://github.com/bigcode-project/the-stack-v2) | The Stack v2 (Software Heritage based, ~67 TB) + curation code | **PROBE** (ground-truth corpus for open-data models) | Apache-2.0 (code); dataset gated by ToU |
| [HF Space: bigcode/in-the-stack](https://huggingface.co/spaces/bigcode/in-the-stack) | "Am I in The Stack?" lookup by GitHub user → repos (v2) | **PROBE** P1 | — |
| [HF Space: HuggingFaceCode/in-the-stack](https://huggingface.co/spaces/HuggingFaceCode/in-the-stack) · [HF: HuggingFaceCode/stack-v3-train](https://huggingface.co/datasets/HuggingFaceCode/stack-v3-train) | **The Stack v3** (superset of v1+v2, 173M repos) and its lookup. The v2 space now points here | **PROBE** P1 | — |
| [HF Space: bigcode/search-v2](https://huggingface.co/spaces/bigcode/search-v2) | "StarCoder2 Search", full-text snippet search over StarCoder2 training data | PROBE P1 (snippet level) | — |
| [bigcode-project/starcoder2](https://github.com/bigcode-project/starcoder2) | StarCoder2: open weights **and** known training data | PROBE (calibration model where membership is ground truth) | Apache-2.0 |
| [OpenCoder-llm/OpenCoder-llm](https://github.com/OpenCoder-llm/OpenCoder-llm) | OpenCoder: open weights + released RefineCode data pipeline | PROBE (second calibration model) | MIT |
| [bigcode-project/bigcode-dataset](https://github.com/bigcode-project/bigcode-dataset) | BigCode preprocessing incl. near-dedup and decontamination scripts | TOOL (n-gram overlap of our materials vs a corpus) | Apache-2.0 |
| [swj0419/detect-pretrain-code](https://github.com/swj0419/detect-pretrain-code) | Min-K% Prob pretraining-data detection | TOOL P3 | Apache-2.0 |
| [zjysteven/mink-plus-plus](https://github.com/zjysteven/mink-plus-plus) | Min-K%++ (ICLR'25), improved detector | **TOOL** P3 (primary MIA score) | MIT |
| [iamgroot42/mimir](https://github.com/iamgroot42/mimir) | Membership-inference benchmark/package (LOSS, zlib, ref-model, Min-K, …) | TOOL P3 | MIT |
| [KDEGroup/Buzzer](https://github.com/KDEGroup/Buzzer) | Code-specific membership inference (EMNLP'24 Findings) | TOOL P3 (code-specific alternative) | MIT |
| [avduarte333/DE-COP_Method](https://github.com/avduarte333/DE-COP_Method) | DE-COP: multiple-choice verbatim-vs-paraphrase detection (works for black-box APIs) | TOOL P2b | Apache-2.0 |
| [ftramer/LM_Memorization](https://github.com/ftramer/LM_Memorization) | Training-data extraction (Carlini et al.) | RW (method for P2) | MIT |
| [liujch1998/infini-gram](https://github.com/liujch1998/infini-gram) | Suffix-array n-gram counting over large corpora | TOOL (corpus prevalence counts) | Apache-2.0 |
| [allenai/wimbd](https://github.com/allenai/wimbd) | "What's In My Big Data", corpus analysis toolkit | TOOL alternative | Apache-2.0 |
| [ruyimarone/data-portraits](https://github.com/ruyimarone/data-portraits) | Data Portraits: Bloom-filter membership sketches for large datasets (incl. The Stack) | TOOL alternative (fast overlap test) | n/d |
| [google-research/deduplicate-text-datasets](https://github.com/google-research/deduplicate-text-datasets) | Exact-substring dedup (suffix arrays). **ARCHIVED** | TOOL (overlap of our materials vs corpus) | Apache-2.0 |
| [EleutherAI/lm-evaluation-harness](https://github.com/EleutherAI/lm-evaluation-harness) | Eval harness with log-likelihood scoring and decontamination utilities | TOOL P3 (loss computation) | MIT |
| [LiveCodeBench/LiveCodeBench](https://github.com/LiveCodeBench/LiveCodeBench) | Time-windowed, contamination-free code eval | RW (post-cutoff control design) | MIT |

---

## Measuring training exposure (exploratory, secondary)

**Status.** Exploratory and not confirmatory. It describes threat §7.1 "Domain familiarity" and
the H7 interpretation. The output is a per-model **familiarity profile**:
- (a) per architecture style (NONE / LAYERED / HEXAGONAL),
- (b) per domain (ORDERS / FHIR).

It enters only exploratory analyses as a descriptive covariate. With two models, no inferential
test across models is possible. All probes are run **once per pinned model, before the first
confirmatory trajectory**. The probe code, prompts and file lists are frozen with `prereg-v1`.

### Probe materials (fixed before registration)

| Set | Content | Exposure status |
|---|---|---|
| **R** (reference) | 20 held-out-from-prompt files per cell (style × domain) from §1–3 public repos, e.g. buckpal / spring-petclinic-rest / spring-petclinic for style; openmrs-module-fhir2, hapi-fhir-jpaserver-starter vs spring-restbucks, ftgo for domain. Pinned commit, last change before the earliest model cutoff | likely seen |
| **F** (fresh) | Matched files from **our own unpublished variants** (same style × domain cell) | guaranteed unseen |
| **P** (post-cutoff) | Matched files from public repos created after the latest model cutoff (sampled by GitHub search on creation date) | unseen, but public-style |

A file appears in **R** only if P1 confirms it is in a known corpus. **F** − **R** contrasts
isolate memorization. **P** separates "familiar idiom" from "memorized file".

### Probes

| # | Probe | What it measures | API-only models | Open-weights models |
|---|---|---|---|---|
| **P1** | **Corpus presence and prevalence.** (i) Look up each reference repo in "Am I in The Stack" (v2 space) and The Stack v3 space. Snippet-check with StarCoder2 Search. (ii) Count Java files in The Stack v2 (or with infini-gram / GitHub code search, fixed query date) that match style markers (`..adapter.in.web`, `..application.port.in/out` vs `..service` + `..repository` packages without ports) and domain markers (`import org.hl7.fhir.r4.model.*` vs order-entity patterns, e.g. `@Entity class Order*` + `OrderStatus`) | Corpus exposure of each style/domain idiom. Model-agnostic prior; ground truth only for open-data models | yes (model-independent prior) | yes, and ground truth for StarCoder2 / OpenCoder |
| **P2** | **Completion / memorization probe.** For each file in R, F, P: give the first 50% of lines (cut at a statement boundary), greedy-decode 128 tokens, compare to the true continuation. Score = normalized Levenshtein similarity and exact-match prefix length (tokens). **Exposure index** = mean(R) − mean(F). **Idiom index** = mean(P) − mean(F) | Verbatim memorization (R) vs idiom familiarity (P) | yes (temperature 0. Chat models get a fixed "continue verbatim" instruction. Report the refusal rate) | yes (raw completion, no chat template) |
| **P2b** | **DE-COP multiple choice** on R vs F: pick the verbatim snippet among 3 paraphrases | Recognition-based membership | yes | yes |
| **P3** | **Loss / perplexity probe.** Per-token NLL on R, F, P. Min-K%++ score (`mink-plus-plus`), with LOSS and zlib-ratio as secondary (`mimir`). Calibrate the AUC on StarCoder2 using known Stack-v2 members vs F | Likelihood-based exposure | **no** (prompt log-probs are generally not exposed. Skip unless the API returns prompt-token logprobs) | yes (primary probe) |
| **P4** | **Style-recognition probe.** Show only a package/directory tree (no code, no names that reveal the style, e.g. project renamed to `app`) of 30 projects: 10 per style from §1, plus our 6 variants as held-out items. Ask for one label in {no explicit architecture, layered, hexagonal/ports-and-adapters, other} plus a 0–100 confidence. Score accuracy, macro-F1, and Brier score per style | Does the model *recognize* each style structurally? | yes | yes |
| **P5** | **Closed-book domain API probe.** 20 micro-tasks per domain with no docs or web, compiled against our pinned dependencies. FHIR: build a `searchset` Bundle, set `meta.versionId`, return an `OperationOutcome` with HAPI structures-r4. ORDERS: Spring Data pagination, Bean Validation → 422, `@Version` optimistic locking. Score = compile + unit-test pass rate | Working familiarity with each domain's APIs | yes | yes |

### Reporting and use
- One table per model: P1 prevalence ratios (HEXAGONAL : LAYERED markers, FHIR : ORDERS
  markers), P2 exposure/idiom indices with bootstrap CIs over files, P3 AUC/score gaps (open
  weights only), P4 per-style F1, P5 per-domain pass rates.
- **Pre-stated expectations** (descriptive, not tested): corpus prevalence LAYERED > HEXAGONAL.
  FHIR shows higher API familiarity (P5) than a generic ORDERS model, but ORDERS-style Spring
  idioms are more prevalent overall (P1). Familiarity is higher for the stronger model.
- **Exploratory use.** (1) Discuss the H7 architecture × domain contrasts next to the per-domain
  familiarity gap. (2) Discuss whether a larger HEXAGONAL−NONE success gap (H4) goes with lower
  P4 hexagonal recognition and P2 idiom familiarity. With n = 2 models these are descriptive
  juxtapositions, never causal claims.
- **Caveats.** Memorization ≠ familiarity: low P2 on R does not imply an unfamiliar idiom, which is
  why P and P4/P5 exist. API models may refuse verbatim continuation. P1 is exact only for
  open-data models. Sending set F to providers must use no-training / zero-retention settings,
  the same as the main experiment, so the materials stay uncontaminated until release.
- **Cost.** Under 1% of the main experiment's token budget. Recorded in the run log and reported.

## 8. Closest papers (literature check 2026-10-13)

| Paper | What it does | Relation to this study |
|---|---|---|
| [arXiv 2608.21747](https://arxiv.org/abs/2608.21747), *Architecture as Capability Equalizer for Coding Agents* (Aug 2026) | Controlled experiment, 6 models. Varies how an architecture is *specified* (including ArchUnit-style rules); structured specifications help weaker models most | Does not vary the architecture itself. Directly relevant to Study 2 (G1 instructions): we must position G2 hooks and erosion-over-time against it |
| [arXiv 2512.04273](https://arxiv.org/pdf/2512.04273), *Quantitative Analysis of Technical Debt and Pattern…* | Pilot: GPT-5.1, Claude 4.5 Sonnet and Llama 3 8B build a hexagonal microservice; without automated architectural linting, structural debt accumulates faster with small models | Single style, no controlled variants; supports H2/H6 motivation and Study 2 G2 |
| Stoyanov (2026), [blog](https://dev.to/kstoyanovai/i-expected-hexagonal-architecture-to-help-my-coding-agent-it-didnt-3blg) + Zenodo report | Flat vs hexagonal, one Java system, 9 + 6 tasks; hexagonal took 37.8% longer to acceptance | Single system and author, not preregistered; anecdotal support for H1 |
| [arXiv 2605.20049](https://arxiv.org/abs/2605.20049), *Does Code Cleanliness Affect Coding Agents?* | Minimal pairs on cleanliness; leaves open whether effects compound over long-horizon evolution | We address that open question for architecture |
| [arXiv 2604.16323](https://arxiv.org/pdf/2604.16323), *Beyond the 'Diff': Addressing Agentic Entropy* | Framing: agents optimise local correctness and drift from global architectural intent | Conceptual framing for H2 and Study 2 |
| [arXiv 2608.16630](https://arxiv.org/pdf/2608.16630), *The Working Set of a Coding Agent: Coherence Debt in Repository-Scale Tasks* | Coherence debt in repository-scale tasks | Related mechanism (localization / working set), to read in full |
