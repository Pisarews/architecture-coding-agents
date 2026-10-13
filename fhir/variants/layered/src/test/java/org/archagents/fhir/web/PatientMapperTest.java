package org.archagents.fhir.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.archagents.fhir.entity.PatientEntity;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class PatientMapperTest {

    private final PatientMapper mapper = new PatientMapper();

    @Test
    void keepsOnlyFirstNameAndRoundTripsPersistedElements() {
        Patient patient = new Patient();
        patient.addName().setFamily("Kowalski").addGiven("Jan").addGiven("Maria");
        patient.addName().setFamily("Ignored");
        patient.addIdentifier().setSystem("urn:pesel").setValue("80022912345");
        patient.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue("jan@example.org");
        patient.setGender(AdministrativeGender.MALE);
        patient.setBirthDateElement(new DateType("1980-02-29"));

        PatientEntity entity = mapper.toEntity(patient);
        assertThat(entity.getFamily()).isEqualTo("Kowalski");
        assertThat(entity.getGiven()).containsExactly("Jan", "Maria");
        assertThat(entity.getBirthDate()).isEqualTo(LocalDate.of(1980, 2, 29));

        entity.setId(7L);
        entity.setVersion(3);
        entity.setLastUpdated(Instant.parse("2030-01-01T12:00:00Z"));
        Patient mapped = mapper.toResource(entity);

        assertThat(mapped.getIdElement().getIdPart()).isEqualTo("7");
        assertThat(mapped.getMeta().getVersionId()).isEqualTo("3");
        assertThat(mapped.getMeta().getLastUpdatedElement().getValueAsString()).isEqualTo("2030-01-01T12:00:00Z");
        assertThat(mapped.getName()).hasSize(1);
        assertThat(mapped.getIdentifierFirstRep().getValue()).isEqualTo("80022912345");
        assertThat(mapped.getTelecomFirstRep().getSystem()).isEqualTo(ContactPointSystem.EMAIL);
        assertThat(mapped.getGender()).isEqualTo(AdministrativeGender.MALE);
        assertThat(mapped.getBirthDateElement().getValueAsString()).isEqualTo("1980-02-29");
    }
}
