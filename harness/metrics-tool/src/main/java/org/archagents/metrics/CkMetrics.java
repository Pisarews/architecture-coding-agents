package org.archagents.metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKNotifier;
import com.github.mauricioaniche.ck.MetricsExecutor;
import com.github.mauricioaniche.ck.util.MetricsFinder;

/**
 * CK 0.7.0 class-level metrics over src/main/java.
 *
 * This is CK's own {@code CK.calculate} loop with three changes, all needed for Java 21 sources:
 * the parser runs at JLS 21 / compliance 21 (CK hard-codes 11), the source path is the real source
 * root (CK passes every sub-directory), and all files go into one batch so bindings resolve the same
 * way regardless of heap size. CK runs on a copy of the sources in which records are desugared to
 * classes ({@link RecordDesugar}); CK's visitor and metric classes are used unchanged.
 */
final class CkMetrics {

    private CkMetrics() {
    }

    static Map<String, Object> compute(Path sourcesDir) throws IOException {
        org.apache.log4j.BasicConfigurator.configure(new org.apache.log4j.varia.NullAppender());
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);

        Path work = Files.createTempDirectory("ck-src-");
        try {
            int desugared = RecordDesugar.copy(sourcesDir, work);
            Map<String, Object> out = run(work);
            out.put("ck_files_with_records", desugared);
            return out;
        } finally {
            try (var s = Files.walk(work)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private static Map<String, Object> run(Path sourcesDir) throws IOException {
        String[] files;
        try (var s = MetricsCli.javaFiles(sourcesDir)) {
            files = s.map(p -> p.toAbsolutePath().toString()).toArray(String[]::new);
        }
        Map<String, CKClassResult> results = new TreeMap<>();
        List<String> errors = new ArrayList<>();
        CKNotifier notifier = new CKNotifier() {
            @Override
            public void notify(CKClassResult r) {
                results.put(r.getClassName() + "@" + r.getFile(), r);
            }

            @Override
            public void notifyError(String file, Exception e) {
                errors.add(file + ": " + e);
            }
        };

        if (files.length > 0) {
            MetricsFinder finder = new MetricsFinder();
            MetricsExecutor executor = new MetricsExecutor(
                    finder::allClassLevelMetrics, () -> finder.allMethodLevelMetrics(false), notifier);
            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
            Map<String, String> options = JavaCore.getOptions();
            JavaCore.setComplianceOptions(JavaCore.VERSION_21, options);
            parser.setCompilerOptions(options);
            String root = sourcesDir.toAbsolutePath().toString();
            parser.setEnvironment(null, new String[] {root}, new String[] {"UTF-8"}, true);
            String[] enc = new String[files.length];
            Arrays.fill(enc, "UTF-8");
            parser.createASTs(files, enc, new String[0], executor, null);
        }
        for (String e : errors) {
            System.err.println("CK error: " + e);
        }

        // Means are over named types (classes, interfaces, enums, records, nested ones included);
        // anonymous classes are excluded from the means but their methods count towards wmc_sum.
        // LOC is summed over top-level types only, because CK's LOC of a type includes its nested types.
        List<CKClassResult> named = results.values().stream().filter(r -> !"anonymous".equals(r.getType())).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ck_types", named.size());
        out.put("ck_errors", errors.size());
        out.put("cbo_mean", named.isEmpty() ? null : Json.round(named.stream().mapToInt(CKClassResult::getCbo).average().orElse(0)));
        out.put("cbo_max", named.stream().mapToInt(CKClassResult::getCbo).max().orElse(0));
        out.put("lcom_mean", named.isEmpty() ? null : Json.round(named.stream().mapToInt(CKClassResult::getLcom).average().orElse(0)));
        out.put("wmc_sum", results.values().stream().mapToInt(CKClassResult::getWmc).sum());
        out.put("loc", named.stream().filter(r -> !r.getClassName().contains("$")).mapToInt(CKClassResult::getLoc).sum());
        return out;
    }
}
