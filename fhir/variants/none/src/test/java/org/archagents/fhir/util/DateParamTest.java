package org.archagents.fhir.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.junit.jupiter.api.Test;

class DateParamTest {

    @Test
    void parsesOptionalPrefix() {
        assertThat(DateParam.parse("birthdate", "1980-01-01"))
                .isEqualTo(new DateParam(DateParam.Prefix.EQ, LocalDate.of(1980, 1, 1)));
        assertThat(DateParam.parse("birthdate", "ge1980-01-01"))
                .isEqualTo(new DateParam(DateParam.Prefix.GE, LocalDate.of(1980, 1, 1)));
        assertThat(DateParam.parse("date", "ne2031-06-02").prefix()).isEqualTo(DateParam.Prefix.NE);
    }

    @Test
    void rejectsMalformedValuesAsInvalid() {
        for (String value : new String[] {"yesterday", "xx1980-01-01", "1980-13-01", "ge"}) {
            assertThatThrownBy(() -> DateParam.parse("birthdate", value))
                    .isInstanceOfSatisfying(FhirException.class,
                            e -> assertThat(e.getIssueType()).isEqualTo(IssueType.INVALID));
        }
    }
}
