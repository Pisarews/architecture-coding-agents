# FHIR baseline — equivalence record

Gate: `scripts/gate.sh fhir <variant>`. It runs these checks in order: Flyway schema
byte-identical, exact dependency set, build with the variant's own unit and ArchUnit tests,
then the black-box acceptance suite (79 tests) against the running jar.

| Date | Variant | Gate | Main LOC | .java files | Packages | Own tests (incl. ArchUnit) | AGENTS.md words |
|---|---|---|---|---|---|---|---|
| 2026-10-07 | NONE | PASS | 2243 | 27 | 4 | 8 | 173 |
| 2026-10-07 | LAYERED | PASS | 2677 | 55 | 5 | 14 | 183 |
| 2026-10-07 | HEXAGONAL | PASS | 4064 | 104 | 12 | 16 | 185 |

The size differences are intrinsic to the styles: ports, mapping, and a separate domain
model. They are reported here, not equalized (PREREGISTRATION §3.2, item 4). HEXAGONAL is
1.81× NONE in LOC and 3.9× in files.

## How equivalence was reached
Three builders worked in parallel from `SPEC.md`. Each time they disagreed, the ambiguity
became a binding clarification (SPEC §8, C1–C13) with a test in `ClarificationsTest`, and
every variant was re-gated. Builders found and resolved 13 ambiguities in total, and the
gate caught 4 real behavioral divergences: C1, C7, C9 and C11.

## AGENTS.md
All three share the same introduction and command section. Only the package-layout section
differs. That section is at package granularity with no class names, and word counts are
within ±10% of their mean.

## Pending before registration
- Independent review that each variant is idiomatic (PREREGISTRATION §3.2, item 5).
- Static erosion baseline (cycles, CK, PMD, SpotBugs, CPD) per variant.
