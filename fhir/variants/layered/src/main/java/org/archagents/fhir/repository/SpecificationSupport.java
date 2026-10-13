package org.archagents.fhir.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.Locale;
import org.archagents.fhir.entity.IdentifierValue;

final class SpecificationSupport {

    private static final char ESCAPE = '\\';

    private SpecificationSupport() {
    }

    static Predicate startsWithIgnoreCase(CriteriaBuilder cb, Expression<String> attribute, String prefix) {
        String pattern = escapeLike(prefix.toLowerCase(Locale.ROOT)) + "%";
        return cb.like(cb.lower(attribute), pattern, ESCAPE);
    }

    static Predicate identifierMatches(CriteriaBuilder cb, From<?, ?> root, String system, String value) {
        Join<?, IdentifierValue> identifier = root.join("identifiers");
        Predicate valueMatches = cb.equal(identifier.get("value"), value);
        if (system == null) {
            return valueMatches;
        }
        Predicate systemMatches = system.isEmpty()
                ? cb.isNull(identifier.get("system"))
                : cb.equal(identifier.get("system"), system);
        return cb.and(systemMatches, valueMatches);
    }

    /**
     * Matches a value against the half-open interval {@code [from, to)} that represents one calendar day.
     */
    static <T extends Comparable<? super T>> Predicate matchesDay(CriteriaBuilder cb, Expression<T> attribute,
                                                                  DateCriterion.Comparator comparator, T from, T to) {
        return switch (comparator) {
            case EQ -> cb.and(cb.greaterThanOrEqualTo(attribute, from), cb.lessThan(attribute, to));
            case NE -> cb.or(cb.lessThan(attribute, from), cb.greaterThanOrEqualTo(attribute, to));
            case LT -> cb.lessThan(attribute, from);
            case LE -> cb.lessThan(attribute, to);
            case GT -> cb.greaterThanOrEqualTo(attribute, to);
            case GE -> cb.greaterThanOrEqualTo(attribute, from);
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
