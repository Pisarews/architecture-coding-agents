# Erosion measurement (PREREGISTRATION §4.3, H2, H6)

Erosion is measured **offline**, after data collection or while it runs, on the per-task git
snapshots in `data/<run>/<trajectory>/repo` (one commit per task: `baseline`, then the task ids).

```
python harness/erosion.py data/<run>            # all trajectories of a run, resumable
python harness/erosion.py data/<run> -j 2       # 2 containers in parallel (each --cpus 2)
python harness/erosion.py data/<run>/<trajectory>
```

For every commit (first-parent order, `task_index` 0 = baseline) the script exports the committed
tree with `git archive`, then in one container
(`docker run --rm --network none --cpus 2 --memory 3g --hostname localhost archagents/sandbox:2`)
runs `mvn -o -q compile`, the metrics tool, PMD, CPD and SpotBugs. It appends one JSON row per commit
to `data/<run>/erosion.jsonl`. Commits already in that file are skipped. Tool or infrastructure
failures are printed and **not** written, so the next run retries them. A snapshot that does not
compile gets `compiles: false`, nulls for every metric, and `log_tail` (the compiler output).

Row keys: `run, trajectory, commit, task, task_index, domain, variant, model, compiles,
pmd_version, spotbugs_version, image, image_id, measured_at`, plus the metric columns below.

## Tools (all pinned, all inside `archagents/sandbox:2`)

| Tool | Version | Source / check |
|---|---|---|
| JDK (compile + all tools) | Temurin 21 (same base layers as tag 1) | `eclipse-temurin:21-jdk@sha256:3e3c176f…` |
| ArchUnit | 1.5.1 (library) | Maven Central, via `harness/metrics-tool/pom.xml` |
| CK | 0.7.0 (library), with JDT core 3.40.0 | Maven Central |
| PMD + CPD | 7.28.0 | GitHub release `pmd-dist-7.28.0-bin.zip`, SHA-256 `f974ba57…fbac2` checked at build |
| SpotBugs | 4.10.4 | GitHub release `spotbugs-4.10.4.tgz`, SHA-256 `72bc0d4e…e4b5` checked at build |
| metrics tool | `harness/metrics-tool` 1.0.0 | built into `/opt/metrics-tool/metrics-tool.jar` at image build |

Tag 2 = tag 1 + three layers (`harness/docker/Dockerfile`, bottom). The agents' `~/.m2` is not
changed: the metrics tool is built with a separate, throw-away local repository. Build with
`harness/docker/build_image.sh 2`. Never rebuild tag 1 (the experiment runs on it).

## Columns

**Metrics tool** (`java -jar metrics-tool.jar target/classes src/main/java`, one JSON object).
Classes are imported from bytecode. ArchUnit resolves nothing from the tool's own classpath, so
library classes are stubs that are known only by name. That way the results cannot depend on the
tool's dependencies.

| Column | Definition |
|---|---|
| `classes` / `classes_incl_nested` / `packages` | top-level classes in `org.archagents.fhir..` (no `package-info`) / incl. nested and anonymous / packages with ≥1 class |
| `cycles` | number of cycles reported by ArchUnit `slices().matching("org.archagents.fhir.(**)").should().beFreeOfCycles()`. ArchUnit enumerates elementary cycles (Johnson), with the cap raised from 100 to 1000; `cycles_capped` is true if the cap was hit |
| `cycle_sccs`, `cycle_packages` | strongly connected components (size > 1) of the package dependency graph, and the number of packages in them ("packages involved in cycles"). Not affected by the cap |
| `dms_mean`, `dms_max`, `dms_by_package` | Martin's normalized distance from the main sequence `D = |A + I − 1|`, via ArchUnit `ArchitectureMetrics.componentDependencyMetrics`, one component per package (its own classes, not subpackages). Mean/max over packages that have classes. ArchUnit counts only public classes for A, and sets I = 1 for a package with Ca + Ce = 0 |
| `cbo_mean`, `cbo_max`, `lcom_mean` | CK class-level CBO and LCOM. Mean/max over named types (classes, interfaces, enums, records, nested types). Anonymous classes are excluded |
| `wmc_sum` | sum of CK WMC over all types (incl. anonymous) |
| `loc` | sum of CK LOC over top-level types (CK's LOC of a type already includes its nested types) |
| `ck_types`, `ck_errors`, `ck_files_with_records` | diagnostics (number of named types CK measured, files CK failed on, files where records were desugared) |
| `conformance_layered`, `conformance_hexagonal` | number of violation lines (`EvaluationResult.getFailureReport().getDetails().size()`), summed over the **frozen** rule set of that variant (`ConformanceRules.java`, a rule-by-rule copy of the baseline `ArchitectureTest` of `fhir/variants/{layered,hexagonal}`; the copy in the snapshot is ignored). Both are computed for every snapshot. The analysis uses the one that matches the variant |
| `conformance_*_rules`, `conformance_*_empty_rules` | per-rule counts. ArchUnit's default `failOnEmptyShould` is kept, as in the baseline tests: a rule whose subject set is empty (e.g. a layer package was deleted) counts as **1** violation and is listed in `_empty_rules` |

**PMD / CPD / SpotBugs** (run by `erosion.py` in the same container):

| Column | Definition |
|---|---|
| `pmd_violations`, `pmd_by_rule`, `pmd_errors` | `pmd check --use-version java-21 -d src/main/java -R harness/metrics-tool/pmd-ruleset.xml --aux-classpath target/classes:<compile deps>`. The rule set is PMD's `quickstart`, unchanged (122 Java rules in 7.28.0; the reason for this choice is in the XML comment). `pmd_errors` = rule exceptions inside PMD (e.g. PMD 7.28.0 throws on some record methods); the rest of the file is still checked |
| `spotbugs_bugs`, `spotbugs_by_category` | `spotbugs -textui -effort:default -medium -auxclasspath <compile deps> target/classes`: default effort, medium-or-higher priority threshold, all detectors |
| `cpd_duplicated_lines` | `pmd cpd --minimum-tokens 100 --language java -d src/main/java`. Number of distinct (file, line) pairs that lie in any clone occurrence, first occurrence included (the Sonar notion of "duplicated lines"; overlapping clones are not double-counted) |
| `cpd_duplications`, `cpd_duplicated_tokens` | number of clone groups; Σ tokens × (occurrences − 1) |

### CK and Java 21

CK 0.7.0 parses with JLS 11 and has no `RecordDeclaration` case. On these code bases this either
mis-parses Java 21 syntax or throws inside CK's metric visitors and loses every type in a file that
contains a record (22 files in HEXAGONAL's baseline, 3–4 in the others). The tool therefore:
1. parses at JLS 21 with JDT core 3.40.0, with the real source root as source path, in one batch;
2. runs CK on a temporary copy of `src/main/java` in which each record
   `record R(A a, B b) implements I { body }` is rewritten as
   `final class R implements I { private final A a; private final B b; body }`. A compact constructor
   becomes a constructor with the component parameters. Implicit accessors, `equals`, `hashCode` and
   `toString` are not added, because nobody wrote them.

CK's visitor and metric classes are used unchanged. With this, `ck_errors` is 0 on all three baselines.

## Erosion index (§4.3) from these columns

Components (the seven metrics listed in §4.3; PMD and SpotBugs form one "warning count"):

| # | component | column(s) |
|---|---|---|
| 1 | package cycles | `cycles` |
| 2 | mean CBO | `cbo_mean` |
| 3 | max CBO | `cbo_max` |
| 4 | mean LCOM | `lcom_mean` |
| 5 | distance from main sequence | `dms_mean` |
| 6 | static-analysis warnings | `pmd_violations + spotbugs_bugs` |
| 7 | duplication | `cpd_duplicated_lines` |

For trajectory `t`, task index `k` and component `m`, with `x` the value on that commit:

1. **Delta from the trajectory's own baseline:** `d[t,k,m] = x[t,k,m] − x[t,0,m]` (so `d[t,0,m] = 0`).
2. **Scale from the pooled baseline-to-end distribution:** `s[m] = SD over all trajectories t of
   d[t,K_t,m]`, where `K_t` is the last task index of `t` with `compiles = true`. Pooling is over all
   trajectories of the confirmatory run set, across arms and models, so the scale is the same for every arm.
3. **z-scored delta:** `z[t,k,m] = d[t,k,m] / s[m]`. Centering by the pooled mean would add a
   constant per metric. That constant does not change any per-trajectory slope (H2), so it is left out
   and the baseline stays at 0.
4. **Index:** `erosion_index[t,k] = mean over m of z[t,k,m]`.
5. **H2:** OLS slope of `erosion_index` over `task_index` per trajectory, then the permutation test of §6.1.
   **H6:** the same slope on `conformance_<variant>` (raw count, LAYERED and HEXAGONAL only).

Each component is also reported on its own (its slope of `d`). Commits with `compiles = false`
have no metric values and are left out of the slope fits. See the open points below.

## Open points vs. PREREGISTRATION.md (not changed there; to be settled before the freeze or logged in DEVIATIONS.md)

- §4.3 names "ArchUnit … count, JDepend" for cycles. Only ArchUnit is used (plus the SCC-based
  `cycle_packages`). JDepend is not run.
- §4.3 does not say whether "z-scored" means centered, or what happens when `s[m] = 0` (a metric that
  never changes in any trajectory, which is plausible for `cycles`). Proposed: no centering (it does
  not change slopes), and a component with `s[m] = 0` is dropped from the index.
- §4.3/§6.4 do not say how non-compiling snapshots enter H2/H6. Proposed: they are left out of the
  slope fit, and their count is reported per arm.
- The CK record desugaring and the JLS 21 parser (above) are a necessary technical adaptation that
  §4.3 does not mention.
- §5.2 lists "compute metrics" inside the per-task loop. Erosion is computed post hoc from the
  committed snapshots instead. This gives the same values, because the metrics depend only on the
  commit.
