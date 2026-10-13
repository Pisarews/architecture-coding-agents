package org.archagents.fhir.adapter.out.persistence;

import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.allOf;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.archagents.fhir.application.port.out.LoadEncounterPort;
import org.archagents.fhir.application.port.out.SaveEncounterPort;
import org.archagents.fhir.domain.Encounter;
import org.archagents.fhir.domain.EncounterSearchCriteria;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
class EncounterPersistenceAdapter implements LoadEncounterPort, SaveEncounterPort {

    private final EncounterRepository encounterRepository;
    private final EncounterPersistenceMapper encounterMapper;

    EncounterPersistenceAdapter(EncounterRepository encounterRepository, EncounterPersistenceMapper encounterMapper) {
        this.encounterRepository = encounterRepository;
        this.encounterMapper = encounterMapper;
    }

    @Override
    public Optional<StoredResource<Encounter>> loadEncounter(long id) {
        return encounterRepository.findById(id).map(encounterMapper::toDomain);
    }

    @Override
    public List<StoredResource<Encounter>> searchEncounters(EncounterSearchCriteria criteria) {
        return encounterRepository.findAll(matching(criteria), Sort.by("id")).stream()
                .map(encounterMapper::toDomain)
                .toList();
    }

    @Override
    public StoredResource<Encounter> createEncounter(Encounter encounter, ResourceMeta meta) {
        EncounterJpaEntity entity = new EncounterJpaEntity();
        encounterMapper.copyToEntity(encounter, meta, entity);
        return encounterMapper.toDomain(encounterRepository.save(entity));
    }

    @Override
    public void updateEncounter(StoredResource<Encounter> encounter) {
        EncounterJpaEntity entity = encounterRepository.findById(encounter.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Encounter/" + encounter.id() + " vanished during update"));
        encounterMapper.copyToEntity(encounter.content(), encounter.meta(), entity);
        encounterRepository.save(entity);
    }

    private static Specification<EncounterJpaEntity> matching(EncounterSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Long patientId : criteria.patientIds()) {
                predicates.add(cb.equal(root.get("subjectId"), patientId));
            }
            for (String status : criteria.statuses()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return allOf(cb, predicates);
        };
    }
}
