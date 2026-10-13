package org.archagents.fhir.adapter.in.web;

import static org.archagents.fhir.adapter.in.web.FhirConversions.applyMeta;
import static org.archagents.fhir.adapter.in.web.FhirConversions.code;

import org.archagents.fhir.domain.ContactPoint;
import org.archagents.fhir.domain.Gender;
import org.archagents.fhir.domain.Patient;
import org.archagents.fhir.domain.StoredResource;
import org.archagents.fhir.domain.TelecomSystem;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.springframework.stereotype.Component;

@Component
class PatientResourceMapper {

    Patient toDomain(org.hl7.fhir.r4.model.Patient resource) {
        return new Patient(
                resource.hasActive() ? resource.getActive() : null,
                FhirConversions.toHumanName(resource.getName()),
                FhirConversions.toIdentifiers(resource.getIdentifier()),
                resource.getTelecom().stream().map(PatientResourceMapper::toContactPoint).toList(),
                resource.hasGender() ? code(resource.getGender().toCode(), Gender::fromCode) : null,
                resource.hasBirthDate() ? FhirConversions.toLocalDate(resource.getBirthDateElement()) : null);
    }

    org.hl7.fhir.r4.model.Patient toResource(StoredResource<Patient> stored) {
        Patient patient = stored.content();
        org.hl7.fhir.r4.model.Patient resource = new org.hl7.fhir.r4.model.Patient();
        applyMeta(resource, stored);
        patient.identifiers().forEach(i -> resource.addIdentifier(FhirConversions.toFhirIdentifier(i)));
        if (patient.active() != null) {
            resource.setActive(patient.active());
        }
        if (patient.name() != null) {
            resource.addName(FhirConversions.toFhirHumanName(patient.name()));
        }
        for (ContactPoint telecom : patient.telecoms()) {
            org.hl7.fhir.r4.model.ContactPoint contactPoint = resource.addTelecom().setValue(telecom.value());
            if (telecom.system() != null) {
                contactPoint.setSystem(ContactPointSystem.fromCode(telecom.system().code()));
            }
        }
        if (patient.gender() != null) {
            resource.setGender(AdministrativeGender.fromCode(patient.gender().code()));
        }
        resource.setBirthDateElement(FhirConversions.toDateType(patient.birthDate()));
        return resource;
    }

    private static ContactPoint toContactPoint(org.hl7.fhir.r4.model.ContactPoint contactPoint) {
        TelecomSystem system = contactPoint.hasSystem()
                ? code(contactPoint.getSystem().toCode(), TelecomSystem::fromCode)
                : null;
        return new ContactPoint(system, contactPoint.getValue());
    }
}
