package org.archagents.fhir.adapter.out.persistence;

import org.archagents.fhir.domain.Gender;
import org.archagents.fhir.domain.HumanName;
import org.archagents.fhir.domain.Patient;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.stereotype.Component;

@Component
class PatientPersistenceMapper {

    StoredResource<Patient> toDomain(PatientJpaEntity entity) {
        HumanName name = entity.getFamily() == null && entity.getGiven().isEmpty()
                ? null
                : new HumanName(entity.getFamily(), entity.getGiven());
        Patient patient = new Patient(
                entity.getActive(),
                name,
                entity.getIdentifiers().stream().map(EmbeddedIdentifier::toDomain).toList(),
                entity.getTelecoms().stream().map(EmbeddedTelecom::toDomain).toList(),
                entity.getGender() == null ? null : Gender.fromCode(entity.getGender()),
                entity.getBirthDate());
        return new StoredResource<>(
                entity.getId(), new ResourceMeta(entity.getVersion(), entity.getLastUpdated()), patient);
    }

    void copyToEntity(Patient patient, ResourceMeta meta, PatientJpaEntity entity) {
        entity.setVersion(meta.versionId());
        entity.setLastUpdated(meta.lastUpdated());
        entity.setActive(patient.active());
        entity.setFamily(patient.name() == null ? null : patient.name().family());
        entity.setGender(patient.gender() == null ? null : patient.gender().code());
        entity.setBirthDate(patient.birthDate());
        entity.getGiven().clear();
        if (patient.name() != null) {
            entity.getGiven().addAll(patient.name().given());
        }
        entity.getIdentifiers().clear();
        patient.identifiers().forEach(identifier -> entity.getIdentifiers().add(EmbeddedIdentifier.from(identifier)));
        entity.getTelecoms().clear();
        patient.telecoms().forEach(telecom -> entity.getTelecoms().add(EmbeddedTelecom.from(telecom)));
    }
}
