package org.archagents.fhir.web;

import org.archagents.fhir.entity.EncounterEntity;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Period;
import org.springframework.stereotype.Component;

@Component
public class EncounterMapper {

    public EncounterEntity toEntity(Encounter encounter) {
        EncounterEntity entity = new EncounterEntity();
        entity.setStatus(encounter.hasStatus() ? encounter.getStatus().toCode() : null);
        if (encounter.hasClass_()) {
            entity.setClassSystem(encounter.getClass_().getSystem());
            entity.setClassCode(encounter.getClass_().getCode());
        }
        entity.setSubjectReference(FhirMapping.referenceString(encounter.getSubject()));
        if (encounter.hasParticipant()) {
            entity.setPractitionerReference(FhirMapping.referenceString(encounter.getParticipant().getFirst().getIndividual()));
        }
        if (encounter.hasPeriod()) {
            entity.setPeriodStart(FhirMapping.toInstant(encounter.getPeriod().getStartElement()));
            entity.setPeriodEnd(FhirMapping.toInstant(encounter.getPeriod().getEndElement()));
        }
        return entity;
    }

    public Encounter toResource(EncounterEntity entity) {
        Encounter encounter = new Encounter();
        FhirMapping.setIdAndMeta(encounter, entity);
        encounter.setStatus(EncounterStatus.fromCode(entity.getStatus()));
        if (entity.getClassSystem() != null || entity.getClassCode() != null) {
            encounter.setClass_(new Coding().setSystem(entity.getClassSystem()).setCode(entity.getClassCode()));
        }
        encounter.setSubject(FhirMapping.reference("Patient", entity.getSubjectId()));
        if (entity.getPractitionerId() != null) {
            encounter.addParticipant().setIndividual(FhirMapping.reference("Practitioner", entity.getPractitionerId()));
        }
        if (entity.getPeriodStart() != null || entity.getPeriodEnd() != null) {
            encounter.setPeriod(new Period()
                    .setStartElement(FhirMapping.dateTime(entity.getPeriodStart()))
                    .setEndElement(FhirMapping.dateTime(entity.getPeriodEnd())));
        }
        return encounter;
    }
}
