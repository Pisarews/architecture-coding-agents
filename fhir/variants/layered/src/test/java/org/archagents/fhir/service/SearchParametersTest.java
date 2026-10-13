package org.archagents.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.archagents.fhir.repository.DateCriterion;
import org.archagents.fhir.repository.DateCriterion.Comparator;
import org.junit.jupiter.api.Test;

class SearchParametersTest {

    @Test
    void parsesDatesWithAndWithoutPrefixAndRejectsMalformedOnes() {
        assertThat(SearchParameters.date("birthdate", "1980-01-01"))
                .isEqualTo(new DateCriterion(Comparator.EQ, LocalDate.of(1980, 1, 1)));
        assertThat(SearchParameters.date("birthdate", "ge2031-06-02"))
                .isEqualTo(new DateCriterion(Comparator.GE, LocalDate.of(2031, 6, 2)));

        assertThatThrownBy(() -> SearchParameters.date("birthdate", "yesterday"))
                .isInstanceOf(InvalidSearchParameterException.class);
        assertThatThrownBy(() -> SearchParameters.date("birthdate", "2031-02-30"))
                .isInstanceOf(InvalidSearchParameterException.class);
    }

    @Test
    void parsesTokensAndReferences() {
        assertThat(SearchParameters.token("urn:sys|abc")).isEqualTo(new SearchParameters.Token("urn:sys", "abc"));
        assertThat(SearchParameters.token("abc")).isEqualTo(new SearchParameters.Token(null, "abc"));
        assertThat(SearchParameters.referenceId("patient", "Patient/42", "Patient")).isEqualTo(42L);
        assertThat(SearchParameters.referenceId("patient", "42", "Patient")).isEqualTo(42L);
        assertThatThrownBy(() -> SearchParameters.referenceId("patient", "Practitioner/42", "Patient"))
                .isInstanceOf(InvalidSearchParameterException.class);
    }
}
