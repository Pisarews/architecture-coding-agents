# Study 2 (plan): Do Copilot-style guardrails control agentic entropy?

Status: **planning draft, not registered.** Build after Phase 1 analysis. Register separately on
OSF before any confirmatory run, like Phase 1.

## Question
Phase 1 asks whether architecture changes how agents evolve code. Its early, arm-blind numbers show
that agents degrade the codebase quickly: about 45% of attempts leave a non-compiling project, and
regressions accumulate. Industrial agents ship tooling meant to counter exactly this. GitHub Copilot
offers repository and organization instructions (`.github/copilot-instructions.md`, `AGENTS.md`),
custom agents, hooks that run validation during agent work, linters, and a self-review step before
the PR. **Do these guardrails reduce erosion and regressions? At what cost? Do they interact with
architecture?** That is: does an explicit architecture plus enforcement beat an explicit
architecture alone?

The research framing is "agentic entropy": agents optimise for local correctness and drift from
global architectural intent (arXiv 2604.16323).

## Design: guardrail level × architecture (× model)

| Level | Name | What the agent gets, on top of the previous level |
|---|---|---|
| G0 | Bare | Phase 1 setup: neutral AGENTS.md, bash tool |
| G1 | Instructions | AGENTS.md / `copilot-instructions.md`-style file with the variant's architecture rules, conventions and "where things go" (for NONE: general clean-code rules) |
| G2 | Enforcement hooks | Before the agent can submit, the harness runs `mvn verify` with the variant's ArchUnit rules + PMD + cycle check (a "hook"). Violations are returned to the agent, which gets up to K fix rounds inside the same step budget |
| G3 | Self-review | After G2 passes, one review pass: the agent receives its own diff plus the architecture rules and must fix any problems it finds before the final submit (the Copilot "self-review before PR" pattern) |

- Architecture: NONE, LAYERED, HEXAGONAL (same baselines, tasks and hidden tests as Phase 1).
- Model: the strong arm only (Laguna S 2.1) by default. The weak arm is optional if the budget allows.
  Guardrails probably matter most for weaker models.
- Same harness, sandbox, grader and erosion pipeline as Phase 1. Only the agent prompt, the
  repository files and the hook step change. This isolates the guardrail effect.

### Hypotheses (draft)
- **G-H1 Erosion:** the erosion slope and own-architecture conformance violations decrease G0 > G1 > G2 > G3.
- **G-H2 Regressions:** fewer true regressions and non-compiling commits at G2/G3 than at G0.
- **G-H3 Cost:** guardrails increase tokens per attempt (more feedback rounds), G0 < G1 < G2 < G3.
- **G-H4 Success:** two-sided. Enforcement may help through earlier error detection, or hurt
  because fix rounds consume the step budget.
- **G-H5 Interaction:** the guardrail benefit on erosion is larger for LAYERED/HEXAGONAL than for
  NONE, because NONE has no architecture to enforce.

## Budget options (Phase 1 measured about USD 0.04 per attempt; guardrails will cost more, assume about 0.06)

| Option | Cells | Attempts | Approx. cost |
|---|---|---|---|
| A: 4 levels × 3 arch × strong × 2 runs × 24 tasks | 24 traj | 576 | ~$35 |
| B: G0 reused from Phase 1 strong runs, G1–G3 × 3 arch × strong × 2 runs | 18 traj | 432 | ~$26 |
| C: like A but 12 tasks and 3 runs | 36 traj | 432 | ~$26 |

Option B is cheapest, but G0 then comes from a different time window (provider drift). That must be
disclosed. Option A is cleanest.

## Ecological arm (exploratory): real GitHub Copilot
Run the real **GitHub Copilot CLI** in headless mode (`copilot -p "<ticket>" --no-ask-user` with
narrow `--allow-tool` permissions) on the same tasks, with G1 instructions in `.github/copilot-instructions.md`.
Same grader and erosion pipeline.
- Requires a Copilot plan (Pro+/Business for custom agents). Cost is counted in premium requests,
  not tokens, so H3-type cost comparisons are limited.
- Model choice is Copilot's (closed models), so this arm is not comparable to the open-weights
  arms. It is reported separately as "what a real product achieves", not as a factor level.
- There is a known open issue with progressive latency in headless mode (github/copilot-cli#2755).

## To build
1. G1 instruction files per variant, equal length across variants, reviewed for neutrality.
2. Hook step in `harness/run_trajectory.py`: intercept the submit, run checks in the sandbox, feed
   back violations, enforce max fix rounds.
3. G3 self-review prompt.
4. Pilot: 3 tasks × 3 arch × 4 levels, plus a shakedown, then preregistration.

## Open decisions
- Option A/B/C and whether to include the weak model.
- Whether you have Copilot Business/Pro+ access for the ecological arm.
