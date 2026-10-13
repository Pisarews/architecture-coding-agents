package org.archagents.fhir.adapter.out.persistence;

import org.archagents.fhir.domain.Coding;
import org.archagents.fhir.domain.Observation;
import org.archagents.fhir.domain.ObservationStatus;
import org.archagents.fhir.domain.Quantity;
import org.archagents.fhir.domain.Reference;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.ResourceType;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.stereotype.Component;

@Component
class ObservationPersistenceMapper {

    StoredResource<Observation> toDomain(ObservationJpaEntity entity) {
        Coding category = entity.getCategorySystem() == null && entity.getCategoryCode() == null
                ? null
                : new Coding(entity.getCategorySystem(), entity.getCategoryCode(), null);
        Quantity value = entity.getValueValue() == null
                ? null
                : new Quantity(entity.getValueValue(), entity.getValueUnit(), entity.getValueSystem(),
                        entity.getValueCode());
        Observation observation = new Observation(
                ObservationStatus.fromCode(entity.getStatus()),
                category,
                new Coding(entity.getCodeSystem(), entity.getCodeCode(), entity.getCodeDisplay()),
                Reference.to(ResourceType.PATIENT, entity.getSubjectId()),
                entity.getEncounterId() == null ? null : Reference.to(ResourceType.ENCOUNTER, entity.getEncounterId()),
                entity.getEffective(),
                value);
        return new StoredResource<>(
                entity.getId(), new ResourceMeta(entity.getVersion(), entity.getLastUpdated()), observation);
    }

    void copyToEntity(Observation observation, ResourceMeta meta, ObservationJpaEntity entity) {
        entity.setVersion(meta.versionId());
        entity.setLastUpdated(meta.lastUpdated());
        entity.setStatus(observation.status().code());
        Coding category = observation.category();
        entity.setCategorySystem(category == null ? null : category.system());
        entity.setCategoryCode(category == null ? null : category.code());
        entity.setCodeSystem(observation.code().system());
        entity.setCodeCode(observation.code().code());
        entity.setCodeDisplay(observation.code().display());
        entity.setSubjectId(observation.subject().idAsLong());
        entity.setEncounterId(observation.encounter() == null ? null : observation.encounter().idAsLong());
        entity.setEffective(observation.effective());
        Quantity value = observation.value();
        entity.setValueValue(value == null ? null : value.value());
        entity.setValueUnit(value == null ? null : value.unit());
        entity.setValueSystem(value == null ? null : value.system());
        entity.setValueCode(value == null ? null : value.code());
    }
}
