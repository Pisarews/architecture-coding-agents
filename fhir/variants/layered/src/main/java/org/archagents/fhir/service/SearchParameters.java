package org.archagents.fhir.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.archagents.fhir.repository.DateCriterion;
import org.springframework.data.domain.Sort;

/**
 * Parsing of FHIR search parameter values shared by the resource services.
 */
final class SearchParameters {

    static final Sort BY_ID = Sort.by("id");

    private static final Pattern DATE = Pattern.compile("(eq|ne|lt|le|gt|ge)?(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern NUMERIC_ID = Pattern.compile("\\d{1,18}");

    private SearchParameters() {
    }

    record Token(String system, String code) {
    }

    /**
     * {@code system|code} or a bare {@code code}; a bare code leaves the system unconstrained.
     */
    static Token token(String value) {
        int bar = value.indexOf('|');
        if (bar < 0) {
            return new Token(null, value);
        }
        return new Token(value.substring(0, bar), value.substring(bar + 1));
    }

    static DateCriterion date(String name, String value) {
        Matcher matcher = DATE.matcher(value);
        if (!matcher.matches()) {
            throw new InvalidSearchParameterException(name, value);
        }
        String prefix = matcher.group(1) == null ? "eq" : matcher.group(1);
        try {
            LocalDate date = LocalDate.parse(matcher.group(2));
            return new DateCriterion(DateCriterion.Comparator.valueOf(prefix.toUpperCase(Locale.ROOT)), date);
        } catch (DateTimeParseException e) {
            throw new InvalidSearchParameterException(name, value);
        }
    }

    /**
     * Accepts {@code <id>} or {@code <resourceType>/<id>}.
     */
    static long referenceId(String name, String value, String resourceType) {
        String id = value.startsWith(resourceType + "/") ? value.substring(resourceType.length() + 1) : value;
        if (!NUMERIC_ID.matcher(id).matches()) {
            throw new InvalidSearchParameterException(name, value);
        }
        return Long.parseLong(id);
    }
}
