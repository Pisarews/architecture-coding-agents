package org.archagents.fhir.adapter.in.web;

import static org.archagents.fhir.adapter.in.web.FhirConversions.applyMeta;
import static org.archagents.fhir.adapter.in.web.FhirConversions.code;
import static org.archagents.fhir.adapter.in.web.FhirConversions.toReference;

import org.archagents.fhir.domain.Coding;
import org.archagents.fhir.domain.Encounter;
import org.archagents.fhir.domain.EncounterStatus;
import org.archagents.fhir.domain.Period;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.stereotype.Component;

@Component
class EncounterResourceMapper {

    Encounter toDomain(org.hl7.fhir.r4.model.Encounter resource) {
        Coding encounterClass = resource.hasClass_()
                ? new Coding(resource.getClass_().getSystem(), resource.getClass_().getCode(), null)
                : null;
        Period period = resource.hasPeriod()
                ? new Period(
                        FhirConversions.toInstant(resource.getPeriod().getStartElement()),
                        FhirConversions.toInstant(resource.getPeriod().getEndElement()))
                : null;
        return new Encounter(
                resource.hasStatus() ? code(resource.getStatus().toCode(), EncounterStatus::fromCode) : null,
                encounterClass,
                toReference(resource.getSubject()),
                resource.hasParticipant() ? toReference(resource.getParticipantFirstRep().getIndividual()) : null,
                period);
    }

    org.hl7.fhir.r4.model.Encounter toResource(StoredResource<Encounter> stored) {
        Encounter encounter = stored.content();
        org.hl7.fhir.r4.model.Encounter resource = new org.hl7.fhir.r4.model.Encounter();
        applyMeta(resource, stored);
        resource.setStatus(org.hl7.fhir.r4.model.Encounter.EncounterStatus.fromCode(encounter.status().code()));
        if (encounter.encounterClass() != null) {
            resource.setClass_(new org.hl7.fhir.r4.model.Coding(
                    encounter.encounterClass().system(), encounter.encounterClass().code(), null));
        }
        resource.setSubject(FhirConversions.toFhirReference(encounter.subject()));
        if (encounter.practitioner() != null) {
            resource.addParticipant().setIndividual(FhirConversions.toFhirReference(encounter.practitioner()));
        }
        if (encounter.period() != null) {
            resource.getPeriod()
                    .setStartElement(FhirConversions.toDateTime(encounter.period().start()))
                    .setEndElement(FhirConversions.toDateTime(encounter.period().end()));
        }
        return resource;
    }
}
