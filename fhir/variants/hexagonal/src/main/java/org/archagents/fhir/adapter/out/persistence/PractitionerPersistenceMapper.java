package org.archagents.fhir.adapter.out.persistence;

import org.archagents.fhir.domain.HumanName;
import org.archagents.fhir.domain.Practitioner;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.stereotype.Component;

@Component
class PractitionerPersistenceMapper {

    StoredResource<Practitioner> toDomain(PractitionerJpaEntity entity) {
        HumanName name = entity.getFamily() == null && entity.getGiven().isEmpty()
                ? null
                : new HumanName(entity.getFamily(), entity.getGiven());
        Practitioner practitioner = new Practitioner(
                entity.getActive(),
                name,
                entity.getIdentifiers().stream().map(EmbeddedIdentifier::toDomain).toList());
        return new StoredResource<>(
                entity.getId(), new ResourceMeta(entity.getVersion(), entity.getLastUpdated()), practitioner);
    }

    void copyToEntity(Practitioner practitioner, ResourceMeta meta, PractitionerJpaEntity entity) {
        entity.setVersion(meta.versionId());
        entity.setLastUpdated(meta.lastUpdated());
        entity.setActive(practitioner.active());
        entity.setFamily(practitioner.name() == null ? null : practitioner.name().family());
        entity.getGiven().clear();
        if (practitioner.name() != null) {
            entity.getGiven().addAll(practitioner.name().given());
        }
        entity.getIdentifiers().clear();
        practitioner.identifiers()
                .forEach(identifier -> entity.getIdentifiers().add(EmbeddedIdentifier.from(identifier)));
    }
}
