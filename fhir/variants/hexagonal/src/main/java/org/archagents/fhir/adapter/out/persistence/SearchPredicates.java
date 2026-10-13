package org.archagents.fhir.adapter.out.persistence;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.archagents.fhir.domain.DateCriterion;
import org.archagents.fhir.domain.TokenCriterion;

final class SearchPredicates {

    private static final char ESCAPE = '\\';

    private SearchPredicates() {
    }

    static Predicate allOf(CriteriaBuilder cb, List<Predicate> predicates) {
        return cb.and(predicates.toArray(Predicate[]::new));
    }

    static Predicate startsWithIgnoringCase(CriteriaBuilder cb, Expression<String> value, String prefix) {
        String pattern = escapeLike(prefix.toLowerCase(Locale.ROOT)) + "%";
        return cb.like(cb.lower(value), pattern, ESCAPE);
    }

    static Predicate matchesToken(
            CriteriaBuilder cb, Expression<String> system, Expression<String> code, TokenCriterion token) {
        Predicate codeMatches = cb.equal(code, token.code());
        if (token.anySystem()) {
            return codeMatches;
        }
        Predicate systemMatches = token.requiresAbsentSystem() ? cb.isNull(system) : cb.equal(system, token.system());
        return cb.and(systemMatches, codeMatches);
    }

    static <E, X> Predicate anyElement(
            Root<E> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String collection,
            Function<Join<E, X>, Predicate> condition) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Join<E, X> element = subquery.correlate(root).join(collection);
        return cb.exists(subquery.select(cb.literal(1)).where(condition.apply(element)));
    }

    static Predicate matchesDate(CriteriaBuilder cb, Expression<LocalDate> date, DateCriterion criterion) {
        return compare(cb, date, criterion.prefix(), criterion.date(), criterion.date().plusDays(1));
    }

    static Predicate matchesDay(CriteriaBuilder cb, Expression<Instant> instant, DateCriterion criterion) {
        Instant dayStart = criterion.date().atStartOfDay(ZoneOffset.UTC).toInstant();
        return compare(cb, instant, criterion.prefix(), dayStart, dayStart.plus(1, ChronoUnit.DAYS));
    }

    private static <T extends Comparable<? super T>> Predicate compare(
            CriteriaBuilder cb, Expression<T> value, DateCriterion.Prefix prefix, T dayStart, T nextDayStart) {
        return switch (prefix) {
            case EQ -> cb.and(cb.greaterThanOrEqualTo(value, dayStart), cb.lessThan(value, nextDayStart));
            case NE -> cb.or(cb.lessThan(value, dayStart), cb.greaterThanOrEqualTo(value, nextDayStart));
            case LT -> cb.lessThan(value, dayStart);
            case LE -> cb.lessThan(value, nextDayStart);
            case GT -> cb.greaterThanOrEqualTo(value, nextDayStart);
            case GE -> cb.greaterThanOrEqualTo(value, dayStart);
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
