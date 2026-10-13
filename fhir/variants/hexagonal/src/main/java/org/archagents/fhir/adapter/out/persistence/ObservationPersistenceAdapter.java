package org.archagents.fhir.adapter.out.persistence;

import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.allOf;
import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.matchesToken;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.archagents.fhir.application.port.out.LoadObservationPort;
import org.archagents.fhir.application.port.out.SaveObservationPort;
import org.archagents.fhir.domain.Observation;
import org.archagents.fhir.domain.ObservationSearchCriteria;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.StoredResource;
import org.archagents.fhir.domain.TokenCriterion;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
class ObservationPersistenceAdapter implements LoadObservationPort, SaveObservationPort {

    private final ObservationRepository observationRepository;
    private final ObservationPersistenceMapper observationMapper;

    ObservationPersistenceAdapter(
            ObservationRepository observationRepository, ObservationPersistenceMapper observationMapper) {
        this.observationRepository = observationRepository;
        this.observationMapper = observationMapper;
    }

    @Override
    public Optional<StoredResource<Observation>> loadObservation(long id) {
        return observationRepository.findById(id).map(observationMapper::toDomain);
    }

    @Override
    public List<StoredResource<Observation>> searchObservations(ObservationSearchCriteria criteria) {
        return observationRepository.findAll(matching(criteria), Sort.by("id")).stream()
                .map(observationMapper::toDomain)
                .toList();
    }

    @Override
    public StoredResource<Observation> createObservation(Observation observation, ResourceMeta meta) {
        ObservationJpaEntity entity = new ObservationJpaEntity();
        observationMapper.copyToEntity(observation, meta, entity);
        return observationMapper.toDomain(observationRepository.save(entity));
    }

    @Override
    public void updateObservation(StoredResource<Observation> observation) {
        ObservationJpaEntity entity = observationRepository.findById(observation.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Observation/" + observation.id() + " vanished during update"));
        observationMapper.copyToEntity(observation.content(), observation.meta(), entity);
        observationRepository.save(entity);
    }

    private static Specification<ObservationJpaEntity> matching(ObservationSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Long patientId : criteria.patientIds()) {
                predicates.add(cb.equal(root.get("subjectId"), patientId));
            }
            for (TokenCriterion code : criteria.codes()) {
                predicates.add(matchesToken(cb, root.get("codeSystem"), root.get("codeCode"), code));
            }
            for (String category : criteria.categories()) {
                predicates.add(cb.equal(root.get("categoryCode"), category));
            }
            return allOf(cb, predicates);
        };
    }
}
