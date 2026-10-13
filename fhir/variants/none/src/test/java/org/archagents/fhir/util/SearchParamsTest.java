package org.archagents.fhir.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

class SearchParamsTest {

    @Test
    void parsesTokensAndReferences() {
        assertThat(SearchParams.Token.parse("http://loinc.org|8867-4"))
                .isEqualTo(new SearchParams.Token("http://loinc.org", "8867-4"));
        assertThat(SearchParams.Token.parse("8867-4")).isEqualTo(new SearchParams.Token(null, "8867-4"));

        assertThat(SearchParams.referenceId("patient", "42", "Patient")).isEqualTo(42L);
        assertThat(SearchParams.referenceId("patient", "Patient/42", "Patient")).isEqualTo(42L);
        assertThatThrownBy(() -> SearchParams.referenceId("patient", "Practitioner/42", "Patient"))
                .isInstanceOfSatisfying(FhirException.class, e -> assertThat(e.getIssueType()).isEqualTo(IssueType.INVALID));
    }

    @Test
    void escapesLikeWildcardsAndRejectsUnknownParameters() {
        assertThat(SearchParams.likePrefix("Ko_wal%")).isEqualTo("ko\\_wal\\%%");

        var params = new LinkedMultiValueMap<String, String>();
        params.add("family", "x");
        params.add("_count", "10");
        assertThatThrownBy(() -> SearchParams.requireSupported(params, Set.of("family")))
                .isInstanceOfSatisfying(FhirException.class,
                        e -> assertThat(e.getIssueType()).isEqualTo(IssueType.NOTSUPPORTED));
    }
}
