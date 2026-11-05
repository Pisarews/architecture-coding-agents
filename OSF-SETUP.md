# OSF setup — step by step

Goal: set up a project that anyone can audit and reproduce. It needs a timestamped
preregistration, open materials, open data, open code, and a DOI on each.

Order matters. **Steps 1–5 happen before any confirmatory run. Steps 6–8 happen after.**

---

## 1. Fill the missing metadata

On the OSF project page, open **Metadata → Edit**:

| Field | Value |
|---|---|
| Resource Type | `Project` if offered, otherwise `Other` (the registration gets `Study Registration`) |
| Language | English |
| Affiliated Institutions | your institution, if any (needs institutional SSO); otherwise leave empty |
| Funder | leave empty, or "None" (self-funded) |
| License | CC-BY 4.0 (already set) for text and data. Code goes on GitHub under MIT |
| Contributors | add ORCID to your OSF profile (Settings → Social), and the reviewer if they become a co-author |

## 1b. Update the project Description

The current description mentions one application. Replace it with:

> This study investigates whether software architecture affects how AI coding agents perform
> when developing and maintaining a codebase. We will implement two Java (Spring Boot)
> applications from different domains — order management and clinical data management
> exposing an HL7 FHIR R4 API — each in three functionally equivalent variants: without explicit
> architecture, with a layered architecture, and with a hexagonal (ports and adapters)
> architecture. AI coding agents (a stronger and a weaker model) will complete the same
> sequence of 24 development tasks in each variant, working cumulatively on their own previous
> changes, with five independent runs per condition. We will measure (1) task success using
> hidden acceptance tests, (2) cost in terms of tokens, agent steps, and time, and
> (3) architectural erosion after each task using ArchUnit rule violations, dependency cycles,
> and static analysis metrics. We expect that cleaner architectures will reduce erosion but may
> increase the cost per change, since changes span more files, and we test whether these
> effects hold across both domains. All data is synthetic. The study aims to provide controlled
> empirical evidence on whether architecture makes codebases more suitable for AI-assisted
> development.

Add tags: `FHIR`, `HL7`, `healthcare interoperability`, `preregistration`.

## 2. Paste the wiki (the "Edit" box on the project overview)

```markdown
## What
A controlled experiment on whether software architecture changes how AI coding agents
perform when they evolve a codebase. Two Java 21 / Spring Boot 3 applications from two
domains (order management; clinical data management via an HL7 FHIR R4 REST facade) each
exist in three functionally equivalent variants (no explicit architecture, layered, hexagonal).
An AI coding agent completes the same 24 development tasks per domain in each variant,
building on its own earlier changes. All data is synthetic.

## Measures
- **Success** — hidden black-box acceptance tests per task (+ regression on earlier tasks)
- **Cost** — tokens, agent steps, wall time, files touched
- **Erosion** — after every task: dependency cycles, coupling, distance from main sequence,
  static-analysis warnings, duplication; ArchUnit conformance for the layered/hexagonal variants

## Status
- [ ] Materials built and equivalence gate passed
- [ ] Preregistration frozen (link: TODO)
- [ ] Data collection
- [ ] Analysis + preprint

## Components
| Component | Contents | Visibility |
|---|---|---|
| 01 Protocol | preregistration, deviation log | public |
| 02 Materials | 2 domains × 3 app variants, tasks, synthetic seed data, AGENTS.md files | public |
| 03 Hidden tests | acceptance tests per task | **private until data collection ends** |
| 04 Harness | agent runner, metric extraction (GitHub-linked) | public |
| 05 Data | raw transcripts, per-task git snapshots, metric tables | public after collection |
| 06 Analysis | frozen analysis scripts, outputs, figures | public |
| 07 Manuscript | preprint + supplementary | public on release |

## License
Text and data: CC-BY 4.0. Code: MIT.
```

## 3. Create the components

**Add Component** seven times, using the names in the table above. Each component has its
own privacy setting. **03 Hidden tests stays private** even while the parent project is
public. Agents with web access, or later training crawls, must not see the tests before the
runs are done.

## 4. Connect GitHub and the code DOI

1. Create a GitHub repo (e.g. `arch-agents-java`). Push the contents of this folder.
2. In component **04 Harness**, go to **Add-ons → GitHub → Connect**, pick the repo. OSF now mirrors it.
3. On zenodo.org, sign in with GitHub, go to **GitHub** and turn on the repo. Each GitHub
   **release** then gets a Zenodo DOI:
   - `v0.1-materials`: variants + tasks frozen (before registration)
   - `v1.0-data`: after data collection
   - `v1.1-paper`: the version the paper cites
4. Fill in `CITATION.cff` in the repo with the DOIs.

## 5. Register the preregistration ← the key step

1. The equivalence gate (PREREGISTRATION §3.2) passes and pilot tasks P1–P3 run cleanly.
2. Resolve every **DECIDE** in `PREREGISTRATION.md`.
3. Run `git tag prereg-v1 && git push --tags`, and publish GitHub release `v0.1-materials` for the Zenodo DOI.
4. On OSF, go to **Registrations → New registration**, choose the **OSF Preregistration** template,
   and copy the sections from `PREREGISTRATION.md` (same numbering). Attach the PDF of the
   file and the hash of the `prereg-v1` commit.
5. Choose an embargo (OSF allows up to 4 years). The default is **no embargo**: a public
   prereg is stronger evidence, and an embargo only makes sense if you fear being scooped.
6. Submit. The registration is frozen, timestamped, and gets its own DOI. **Only now** start
   the confirmatory runs.

### Alternative with more weight: Registered Report
MSR and ESEM have run Registered Report tracks, with Stage 2 continuing at the EMSE journal.
At Stage 1 you submit the introduction, method, and analysis plan with no results. If it is
accepted in principle, the paper gets published whatever the results are. Check the current
calls for papers for deadlines. If you go this way, the reviewers' changes go into the
prereg **before** the OSF registration.

## 6. During data collection
- Log every deviation in `DEVIATIONS.md`, with the date and the reason.
- Do not look at outcome aggregates until all 60 trajectories are done. Watching the
  infrastructure is fine; looking at success rates is not.

## 7. After data collection
1. Make **03 Hidden tests** and **05 Data** public.
2. Run the frozen analysis script unchanged. Label any extra analyses "exploratory".
3. Publish GitHub release `v1.0-data` for a Zenodo DOI.
4. Project page **→ Make Public → Create DOI** (OSF project DOI).

## 8. Publication
- Preprint on arXiv (cs.SE) and/or OSF Preprints. Link the preprint from the OSF project.
- The paper's "Data availability" section cites the OSF DOI, the prereg DOI, and the Zenodo DOI.
- Self-check the paper against the ACM SIGSOFT Empirical Standards ("Experiments" +
  "Registered Reports" if you take that path).
