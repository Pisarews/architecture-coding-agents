package org.archagents.metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.metrics.ArchitectureMetrics;
import com.tngtech.archunit.library.metrics.ComponentDependencyMetrics;
import com.tngtech.archunit.library.metrics.MetricsComponent;
import com.tngtech.archunit.library.metrics.MetricsComponents;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Erosion metrics for one compiled snapshot of the FHIR app.
 *
 * <pre>java -jar metrics-tool.jar CLASSES_DIR SOURCES_DIR</pre>
 *
 * CLASSES_DIR is the compiled main output (target/classes), SOURCES_DIR is src/main/java.
 * Prints exactly one JSON object on stdout; diagnostics go to stderr. See harness/EROSION.md.
 */
public final class MetricsCli {

    static final String ROOT = "org.archagents.fhir";
    /** Cap for ArchUnit's Johnson cycle enumeration (ArchUnit default: 100). */
    static final int MAX_CYCLES = 1000;

    private MetricsCli() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("usage: java -jar metrics-tool.jar CLASSES_DIR SOURCES_DIR");
            System.exit(2);
        }
        Path classesDir = Path.of(args[0]);
        Path sourcesDir = Path.of(args[1]);
        if (!Files.isDirectory(classesDir) || !Files.isDirectory(sourcesDir)) {
            System.err.println("not a directory: " + (Files.isDirectory(classesDir) ? sourcesDir : classesDir));
            System.exit(2);
        }

        ArchConfiguration cfg = ArchConfiguration.get();
        // Results must not depend on what happens to be on this tool's own classpath:
        // every class outside the snapshot is an import stub identified by its name only.
        cfg.setResolveMissingDependenciesFromClassPath(false);
        cfg.setProperty("cycles.maxNumberToDetect", String.valueOf(MAX_CYCLES));

        JavaClasses all = new ClassFileImporter().importPath(classesDir);
        JavaClasses classes = all.that(com.tngtech.archunit.base.DescribedPredicate.describe(
                "in " + ROOT, (JavaClass c) -> c.getPackageName().equals(ROOT) || c.getPackageName().startsWith(ROOT + ".")));

        Map<String, Object> out = new LinkedHashMap<>();
        size(classes, out);
        cycles(classes, out);
        distanceFromMainSequence(classes, out);
        out.putAll(CkMetrics.compute(sourcesDir));
        conformance("layered", ConformanceRules.LAYERED, classes, out);
        conformance("hexagonal", ConformanceRules.HEXAGONAL, classes, out);

        System.out.println(Json.write(out));
    }

    static List<JavaClass> realClasses(JavaClasses classes) {
        return classes.stream()
                .filter(c -> !c.getSimpleName().equals("package-info"))
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .collect(Collectors.toList());
    }

    private static void size(JavaClasses classes, Map<String, Object> out) {
        List<JavaClass> real = realClasses(classes);
        out.put("classes", real.stream().filter(JavaClass::isTopLevelClass).count());
        out.put("classes_incl_nested", real.size());
        out.put("packages", real.stream().map(JavaClass::getPackageName).distinct().count());
    }

    /** ArchUnit cycle count (as specified), plus SCC-based counts that do not depend on the enumeration cap. */
    private static void cycles(JavaClasses classes, Map<String, Object> out) {
        ArchRule rule = slices().matching(ROOT + ".(**)").should().beFreeOfCycles().allowEmptyShould(true);
        EvaluationResult result = rule.evaluate(classes);
        List<String> details = result.getFailureReport().getDetails();
        long n = details.stream().filter(d -> d.startsWith("Cycle detected")).count();
        out.put("cycles", n);
        out.put("cycles_capped", n >= MAX_CYCLES);

        // package dependency graph inside ROOT, Tarjan SCCs
        Map<String, Set<String>> graph = new TreeMap<>();
        for (JavaClass c : realClasses(classes)) {
            String from = c.getPackageName();
            graph.computeIfAbsent(from, k -> new TreeSet<>());
            for (Dependency d : c.getDirectDependenciesFromSelf()) {
                String to = d.getTargetClass().getBaseComponentType().getPackageName();
                if (!to.equals(from) && (to.equals(ROOT) || to.startsWith(ROOT + "."))) {
                    graph.get(from).add(to);
                    graph.computeIfAbsent(to, k -> new TreeSet<>());
                }
            }
        }
        List<List<String>> sccs = Tarjan.sccs(graph).stream().filter(s -> s.size() > 1).collect(Collectors.toList());
        out.put("cycle_sccs", sccs.size());
        out.put("cycle_packages", sccs.stream().mapToInt(List::size).sum());
    }

    /** Mean normalized distance from the main sequence, one component per package (classes directly in it). */
    private static void distanceFromMainSequence(JavaClasses classes, Map<String, Object> out) {
        Map<String, List<JavaClass>> byPackage = new TreeMap<>();
        for (JavaClass c : realClasses(classes)) {
            byPackage.computeIfAbsent(c.getPackageName(), k -> new ArrayList<>()).add(c);
        }
        if (byPackage.isEmpty()) {
            out.put("dms_mean", null);
            out.put("dms_max", null);
            return;
        }
        List<MetricsComponent<JavaClass>> components = new ArrayList<>();
        byPackage.forEach((pkg, cs) -> components.add(MetricsComponent.of(pkg, cs)));
        ComponentDependencyMetrics m = ArchitectureMetrics.componentDependencyMetrics(MetricsComponents.of(components));
        double sum = 0;
        double max = 0;
        Map<String, Object> perPackage = new LinkedHashMap<>();
        for (String pkg : byPackage.keySet()) {
            double d = m.getNormalizedDistanceFromMainSequence(pkg);
            sum += d;
            max = Math.max(max, d);
            perPackage.put(pkg.equals(ROOT) ? "(root)" : pkg.substring(ROOT.length() + 1), Json.round(d));
        }
        out.put("dms_mean", Json.round(sum / byPackage.size()));
        out.put("dms_max", Json.round(max));
        out.put("dms_by_package", perPackage);
    }

    private static void conformance(String name, Map<String, ArchRule> rules, JavaClasses classes, Map<String, Object> out) {
        Map<String, Object> perRule = new LinkedHashMap<>();
        List<String> empty = new ArrayList<>();
        int total = 0;
        for (Map.Entry<String, ArchRule> e : rules.entrySet()) {
            int n;
            try {
                n = e.getValue().evaluate(classes).getFailureReport().getDetails().size();
            } catch (AssertionError emptyShould) {
                // ArchUnit's failOnEmptyShould (on by default, as in the baseline tests): the rule's
                // subject set is empty, e.g. a layer package no longer exists. The baseline test fails
                // with exactly one message in that case, so it counts as one violation.
                empty.add(e.getKey());
                n = 1;
            }
            perRule.put(e.getKey(), n);
            total += n;
        }
        out.put("conformance_" + name, total);
        out.put("conformance_" + name + "_rules", perRule);
        out.put("conformance_" + name + "_empty_rules", empty);
    }

    /** Tarjan's strongly connected components, iterative-safe for small graphs (recursive is fine here). */
    static final class Tarjan {
        private final Map<String, Set<String>> g;
        private final Map<String, Integer> index = new HashMap<>();
        private final Map<String, Integer> low = new HashMap<>();
        private final Deque<String> stack = new ArrayDeque<>();
        private final Set<String> onStack = new TreeSet<>();
        private final List<List<String>> result = new ArrayList<>();
        private int counter;

        private Tarjan(Map<String, Set<String>> g) {
            this.g = g;
        }

        static List<List<String>> sccs(Map<String, Set<String>> g) {
            Tarjan t = new Tarjan(g);
            for (String v : g.keySet()) {
                if (!t.index.containsKey(v)) {
                    t.connect(v);
                }
            }
            return t.result;
        }

        private void connect(String v) {
            index.put(v, counter);
            low.put(v, counter);
            counter++;
            stack.push(v);
            onStack.add(v);
            for (String w : g.getOrDefault(v, Set.of())) {
                if (!index.containsKey(w)) {
                    connect(w);
                    low.put(v, Math.min(low.get(v), low.get(w)));
                } else if (onStack.contains(w)) {
                    low.put(v, Math.min(low.get(v), index.get(w)));
                }
            }
            if (low.get(v).equals(index.get(v))) {
                List<String> scc = new ArrayList<>();
                String w;
                do {
                    w = stack.pop();
                    onStack.remove(w);
                    scc.add(w);
                } while (!w.equals(v));
                result.add(scc);
            }
        }
    }

    static Stream<Path> javaFiles(Path root) throws IOException {
        return Files.walk(root).filter(p -> p.toString().endsWith(".java") && Files.isRegularFile(p)).sorted();
    }
}
