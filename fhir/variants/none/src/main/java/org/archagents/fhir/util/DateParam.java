package org.archagents.fhir.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** A FHIR date search value ({@code [prefix]YYYY-MM-DD}) evaluated against whole days. */
public record DateParam(Prefix prefix, LocalDate date) {

    public enum Prefix { EQ, NE, LT, LE, GT, GE }

    public static DateParam parse(String name, String value) {
        Prefix prefix = Prefix.EQ;
        String date = value;
        if (value.length() > 2 && Character.isLetter(value.charAt(0))) {
            try {
                prefix = Prefix.valueOf(value.substring(0, 2).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw FhirException.invalid("Invalid prefix in " + name + "=" + value);
            }
            date = value.substring(2);
        }
        try {
            return new DateParam(prefix, LocalDate.parse(date));
        } catch (DateTimeParseException e) {
            throw FhirException.invalid("Invalid date in " + name + "=" + value + " (expected [prefix]YYYY-MM-DD)");
        }
    }

    public Predicate toPredicate(CriteriaBuilder cb, Expression<LocalDate> path) {
        return range(cb, path, date, date.plusDays(1));
    }

    /** Matches instants whose UTC calendar date satisfies this parameter. */
    public Predicate toPredicateOnUtcDay(CriteriaBuilder cb, Expression<Instant> path) {
        return range(cb, path, date.atStartOfDay(ZoneOffset.UTC).toInstant(),
                date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private <T extends Comparable<? super T>> Predicate range(CriteriaBuilder cb, Expression<T> path, T dayStart, T nextDayStart) {
        return switch (prefix) {
            case EQ -> cb.and(cb.greaterThanOrEqualTo(path, dayStart), cb.lessThan(path, nextDayStart));
            case NE -> cb.or(cb.lessThan(path, dayStart), cb.greaterThanOrEqualTo(path, nextDayStart));
            case LT -> cb.lessThan(path, dayStart);
            case LE -> cb.lessThan(path, nextDayStart);
            case GT -> cb.greaterThanOrEqualTo(path, nextDayStart);
            case GE -> cb.greaterThanOrEqualTo(path, dayStart);
        };
    }
}
