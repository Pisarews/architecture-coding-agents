package org.archagents.fhir.web;

import java.time.LocalDate;
import org.archagents.fhir.entity.IdentifierValue;
import org.archagents.fhir.entity.PatientEntity;
import org.archagents.fhir.entity.TelecomValue;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public PatientEntity toEntity(Patient patient) {
        PatientEntity entity = new PatientEntity();
        entity.setActive(patient.hasActive() ? patient.getActive() : null);
        if (patient.hasName()) {
            HumanName name = patient.getName().getFirst();
            entity.setFamily(name.getFamily());
            entity.setGiven(name.getGiven().stream().filter(StringType::hasValue).map(StringType::getValue).toList());
        }
        entity.setIdentifiers(patient.getIdentifier().stream()
                .filter(i -> i.hasSystem() || i.hasValue())
                .map(i -> new IdentifierValue(i.getSystem(), i.getValue()))
                .toList());
        entity.setTelecoms(patient.getTelecom().stream()
                .filter(t -> t.hasSystem() || t.hasValue())
                .map(t -> new TelecomValue(t.hasSystem() ? t.getSystem().toCode() : null, t.getValue()))
                .toList());
        entity.setGender(patient.hasGender() ? patient.getGender().toCode() : null);
        if (patient.hasBirthDateElement() && patient.getBirthDateElement().hasValue()) {
            DateType birthDate = patient.getBirthDateElement();
            entity.setBirthDate(LocalDate.of(birthDate.getYear(), birthDate.getMonth() + 1, birthDate.getDay()));
        }
        return entity;
    }

    public Patient toResource(PatientEntity entity) {
        Patient patient = new Patient();
        FhirMapping.setIdAndMeta(patient, entity);
        if (entity.getActive() != null) {
            patient.setActive(entity.getActive());
        }
        if (entity.getFamily() != null || !entity.getGiven().isEmpty()) {
            HumanName name = patient.addName().setFamily(entity.getFamily());
            entity.getGiven().forEach(name::addGiven);
        }
        for (IdentifierValue identifier : entity.getIdentifiers()) {
            patient.addIdentifier(new Identifier().setSystem(identifier.getSystem()).setValue(identifier.getValue()));
        }
        for (TelecomValue telecom : entity.getTelecoms()) {
            ContactPoint contactPoint = patient.addTelecom().setValue(telecom.getValue());
            if (telecom.getSystem() != null) {
                contactPoint.setSystem(ContactPointSystem.fromCode(telecom.getSystem()));
            }
        }
        if (entity.getGender() != null) {
            patient.setGender(AdministrativeGender.fromCode(entity.getGender()));
        }
        if (entity.getBirthDate() != null) {
            patient.setBirthDateElement(new DateType(entity.getBirthDate().toString()));
        }
        return patient;
    }
}
