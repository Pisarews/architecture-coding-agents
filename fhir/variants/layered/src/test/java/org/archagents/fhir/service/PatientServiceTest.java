package org.archagents.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.archagents.fhir.entity.IdentifierValue;
import org.archagents.fhir.entity.PatientEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({PatientService.class, PatientServiceTest.FixedClock.class})
class PatientServiceTest {

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2030-06-15T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    PatientService patientService;

    @Test
    void rejectsBirthDateAfterToday() {
        PatientEntity patient = new PatientEntity();
        patient.setFamily("Nowak");
        patient.setBirthDate(LocalDate.of(2030, 6, 16));

        assertThatThrownBy(() -> patientService.create(patient))
                .isInstanceOf(ResourceValidationException.class)
                .extracting(e -> ((ResourceValidationException) e).getViolation())
                .isEqualTo(ResourceValidationException.Violation.BUSINESS_RULE);
    }

    @Test
    void identifierWithoutFamilyIsEnoughAndUpdateIncrementsVersion() {
        PatientEntity patient = new PatientEntity();
        patient.setIdentifiers(List.of(new IdentifierValue("urn:test", "123")));
        PatientEntity created = patientService.create(patient);
        assertThat(created.getVersion()).isEqualTo(1);

        PatientEntity changed = new PatientEntity();
        changed.setFamily("Kowalska");
        PatientEntity updated = patientService.update(created.getId(), changed);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getVersion()).isEqualTo(2);
        assertThat(updated.getLastUpdated()).isEqualTo(Instant.parse("2030-06-15T10:00:00Z"));
    }
}
