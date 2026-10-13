package org.archagents.fhir.adapter.out.persistence;

import org.archagents.fhir.domain.Coding;
import org.archagents.fhir.domain.Encounter;
import org.archagents.fhir.domain.EncounterStatus;
import org.archagents.fhir.domain.Period;
import org.archagents.fhir.domain.Reference;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.ResourceType;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.stereotype.Component;

@Component
class EncounterPersistenceMapper {

    StoredResource<Encounter> toDomain(EncounterJpaEntity entity) {
        Coding encounterClass = entity.getClassSystem() == null && entity.getClassCode() == null
                ? null
                : new Coding(entity.getClassSystem(), entity.getClassCode(), null);
        Period period = entity.getPeriodStart() == null && entity.getPeriodEnd() == null
                ? null
                : new Period(entity.getPeriodStart(), entity.getPeriodEnd());
        Encounter encounter = new Encounter(
                EncounterStatus.fromCode(entity.getStatus()),
                encounterClass,
                Reference.to(ResourceType.PATIENT, entity.getSubjectId()),
                entity.getPractitionerId() == null
                        ? null
                        : Reference.to(ResourceType.PRACTITIONER, entity.getPractitionerId()),
                period);
        return new StoredResource<>(
                entity.getId(), new ResourceMeta(entity.getVersion(), entity.getLastUpdated()), encounter);
    }

    void copyToEntity(Encounter encounter, ResourceMeta meta, EncounterJpaEntity entity) {
        entity.setVersion(meta.versionId());
        entity.setLastUpdated(meta.lastUpdated());
        entity.setStatus(encounter.status().code());
        Coding encounterClass = encounter.encounterClass();
        entity.setClassSystem(encounterClass == null ? null : encounterClass.system());
        entity.setClassCode(encounterClass == null ? null : encounterClass.code());
        entity.setSubjectId(encounter.subject().idAsLong());
        entity.setPractitionerId(encounter.practitioner() == null ? null : encounter.practitioner().idAsLong());
        Period period = encounter.period();
        entity.setPeriodStart(period == null ? null : period.start());
        entity.setPeriodEnd(period == null ? null : period.end());
    }
}
