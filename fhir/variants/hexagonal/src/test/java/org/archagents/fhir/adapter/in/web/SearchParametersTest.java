package org.archagents.fhir.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.archagents.fhir.domain.DateCriterion;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class SearchParametersTest {

    @Test
    void parsesDatePrefixesAndRejectsUnknownOrMalformedParameters() {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("birthdate", "1980-01-01");
        query.add("birthdate", "lt1990-06-15");

        assertThat(SearchParameters.of(SupportedResource.PATIENT, query).dates("birthdate")).containsExactly(
                new DateCriterion(DateCriterion.Prefix.EQ, LocalDate.of(1980, 1, 1)),
                new DateCriterion(DateCriterion.Prefix.LT, LocalDate.of(1990, 6, 15)));

        MultiValueMap<String, String> unknown = new LinkedMultiValueMap<>();
        unknown.add("_count", "10");
        assertThatThrownBy(() -> SearchParameters.of(SupportedResource.PATIENT, unknown))
                .isInstanceOfSatisfying(FhirRequestException.class,
                        e -> assertThat(e.issueType()).isEqualTo(IssueType.NOTSUPPORTED));

        MultiValueMap<String, String> malformed = new LinkedMultiValueMap<>();
        malformed.add("patient", "Practitioner/1");
        assertThatThrownBy(() -> SearchParameters.of(SupportedResource.ENCOUNTER, malformed)
                        .references("patient", "Patient"))
                .isInstanceOfSatisfying(FhirRequestException.class,
                        e -> assertThat(e.issueType()).isEqualTo(IssueType.INVALID));
    }
}
