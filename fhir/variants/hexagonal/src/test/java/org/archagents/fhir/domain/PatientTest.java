package org.archagents.fhir.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PatientTest {

    private static final LocalDate TODAY = LocalDate.of(2030, 6, 1);

    @Test
    void requiresFamilyNameOrIdentifierValueAndRejectsFutureBirthDate() {
        Patient givenNameOnly = patient(new HumanName(null, List.of("Anna")), List.of(), null);
        Patient identifierOnly = patient(null, List.of(new Identifier("urn:test", "42")), null);
        Patient bornTomorrow = patient(new HumanName("Nowak", List.of()), List.of(), TODAY.plusDays(1));

        assertThatThrownBy(() -> givenNameOnly.validate(TODAY))
                .hasFieldOrPropertyWithValue("kind", RuleViolationException.Kind.REQUIRED);
        assertThatCode(() -> identifierOnly.validate(TODAY)).doesNotThrowAnyException();
        assertThatThrownBy(() -> bornTomorrow.validate(TODAY))
                .hasFieldOrPropertyWithValue("kind", RuleViolationException.Kind.BUSINESS_RULE);
    }

    private static Patient patient(HumanName name, List<Identifier> identifiers, LocalDate birthDate) {
        return new Patient(true, name, identifiers, List.of(), Gender.FEMALE, birthDate);
    }
}
