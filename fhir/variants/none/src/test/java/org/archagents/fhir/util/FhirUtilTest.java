package org.archagents.fhir.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class FhirUtilTest {

    @Test
    void parseRejectsMalformedOrMismatchedBodiesAsStructureErrors() {
        assertThat(FhirUtil.parse("{\"resourceType\":\"Patient\",\"gender\":\"female\"}", Patient.class).getGender().toCode())
                .isEqualTo("female");

        for (String body : new String[] {"{not json", "{\"hello\":\"world\"}", "{\"resourceType\":\"Practitioner\"}"}) {
            assertThatThrownBy(() -> FhirUtil.parse(body, Patient.class))
                    .isInstanceOfSatisfying(FhirException.class,
                            e -> assertThat(e.getIssueType()).isEqualTo(IssueType.STRUCTURE));
        }
    }

    @Test
    void normalizeStripsStorageScale() {
        assertThat(FhirUtil.normalize(new BigDecimal("72.500000"))).isEqualTo(new BigDecimal("72.5"));
        assertThat(FhirUtil.normalize(new BigDecimal("100.000000"))).isEqualTo(new BigDecimal("100"));
    }
}
