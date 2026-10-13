package org.archagents.fhir.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.Locale;
import java.util.Set;
import org.springframework.util.MultiValueMap;

public final class SearchParams {

    /**
     * A token search value: {@code system|code}, {@code |code} (no system) or just {@code code} (any system).
     * {@code system} is null when not given and empty for {@code |code}.
     */
    public record Token(String system, String code) {

        public static Token parse(String value) {
            int bar = value.indexOf('|');
            return bar < 0 ? new Token(null, value) : new Token(value.substring(0, bar), value.substring(bar + 1));
        }

        public Predicate toPredicate(CriteriaBuilder cb, Expression<String> systemPath, Expression<String> codePath) {
            Predicate matchesCode = cb.equal(codePath, code);
            if (system == null) {
                return matchesCode;
            }
            Predicate matchesSystem = system.isEmpty() ? cb.isNull(systemPath) : cb.equal(systemPath, system);
            return cb.and(matchesSystem, matchesCode);
        }
    }

    private SearchParams() {
    }

    public static void requireSupported(MultiValueMap<String, String> params, Set<String> supported) {
        for (String name : params.keySet()) {
            if (!supported.contains(name)) {
                throw FhirException.unsupportedParameter(name);
            }
        }
    }

    /** Parses a reference search value given as {@code id} or {@code Type/id}. */
    public static long referenceId(String name, String value, String type) {
        String id = value.startsWith(type + "/") ? value.substring(type.length() + 1) : value;
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw FhirException.invalid("Invalid reference in " + name + "=" + value + " (expected <id> or " + type + "/<id>)");
        }
    }

    /** Lower-cased LIKE pattern for a case-insensitive prefix match, escaped with {@code \}. */
    public static String likePrefix(String value) {
        String escaped = value.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return escaped + "%";
    }
}
