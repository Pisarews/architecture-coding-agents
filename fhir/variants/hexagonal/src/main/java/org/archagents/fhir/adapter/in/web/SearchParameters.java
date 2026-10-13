package org.archagents.fhir.adapter.in.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.archagents.fhir.domain.DateCriterion;
import org.archagents.fhir.domain.TokenCriterion;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

/** Validated FHIR search parameters of one request, converted to domain criteria on demand. */
final class SearchParameters {

    private static final Pattern DATE = Pattern.compile("(eq|ne|lt|le|gt|ge)?(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern NUMERIC_ID = Pattern.compile("\\d{1,18}");

    private final MultiValueMap<String, String> query;

    private SearchParameters(MultiValueMap<String, String> query) {
        this.query = query;
    }

    static SearchParameters of(SupportedResource resource, MultiValueMap<String, String> query) {
        for (String name : query.keySet()) {
            if (!resource.searchParameters().containsKey(name)) {
                throw new FhirRequestException(HttpStatus.BAD_REQUEST, IssueType.NOTSUPPORTED,
                        "Unknown search parameter '" + name + "' for " + resource.typeName());
            }
        }
        return new SearchParameters(query);
    }

    List<String> strings(String name) {
        return values(name);
    }

    List<TokenCriterion> tokens(String name) {
        return values(name).stream()
                .map(value -> {
                    int bar = value.indexOf('|');
                    return bar < 0
                            ? TokenCriterion.anySystem(value)
                            : new TokenCriterion(value.substring(0, bar), value.substring(bar + 1));
                })
                .toList();
    }

    List<String> codes(String name) {
        return tokens(name).stream().map(TokenCriterion::code).toList();
    }

    List<DateCriterion> dates(String name) {
        return values(name).stream().map(value -> parseDate(name, value)).toList();
    }

    List<Long> references(String name, String targetType) {
        return values(name).stream().map(value -> parseReference(name, value, targetType)).toList();
    }

    private List<String> values(String name) {
        return query.getOrDefault(name, List.of());
    }

    private static DateCriterion parseDate(String name, String value) {
        Matcher matcher = DATE.matcher(value);
        if (matcher.matches()) {
            try {
                String prefix = matcher.group(1) == null ? "eq" : matcher.group(1);
                return new DateCriterion(
                        DateCriterion.Prefix.valueOf(prefix.toUpperCase(Locale.ROOT)),
                        LocalDate.parse(matcher.group(2)));
            } catch (DateTimeParseException ignored) {
                // falls through to the error below
            }
        }
        throw FhirRequestException.invalid("Invalid date '" + value + "' for search parameter '" + name + "'");
    }

    private static long parseReference(String name, String value, String targetType) {
        String id = value.startsWith(targetType + "/") ? value.substring(targetType.length() + 1) : value;
        if (!NUMERIC_ID.matcher(id).matches()) {
            throw FhirRequestException.invalid(
                    "Invalid reference '" + value + "' for search parameter '" + name + "'");
        }
        return Long.parseLong(id);
    }
}
