package org.archagents.fhir.adapter.out.persistence;

import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.allOf;
import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.anyElement;
import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.matchesToken;
import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.startsWithIgnoringCase;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.archagents.fhir.application.port.out.LoadPractitionerPort;
import org.archagents.fhir.application.port.out.SavePractitionerPort;
import org.archagents.fhir.domain.Practitioner;
import org.archagents.fhir.domain.PractitionerSearchCriteria;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
class PractitionerPersistenceAdapter implements LoadPractitionerPort, SavePractitionerPort {

    private final PractitionerRepository practitionerRepository;
    private final PractitionerPersistenceMapper practitionerMapper;

    PractitionerPersistenceAdapter(
            PractitionerRepository practitionerRepository, PractitionerPersistenceMapper practitionerMapper) {
        this.practitionerRepository = practitionerRepository;
        this.practitionerMapper = practitionerMapper;
    }

    @Override
    public Optional<StoredResource<Practitioner>> loadPractitioner(long id) {
        return practitionerRepository.findById(id).map(practitionerMapper::toDomain);
    }

    @Override
    public List<StoredResource<Practitioner>> searchPractitioners(PractitionerSearchCriteria criteria) {
        return practitionerRepository.findAll(matching(criteria), Sort.by("id")).stream()
                .map(practitionerMapper::toDomain)
                .toList();
    }

    @Override
    public StoredResource<Practitioner> createPractitioner(Practitioner practitioner, ResourceMeta meta) {
        PractitionerJpaEntity entity = new PractitionerJpaEntity();
        practitionerMapper.copyToEntity(practitioner, meta, entity);
        return practitionerMapper.toDomain(practitionerRepository.save(entity));
    }

    @Override
    public void updatePractitioner(StoredResource<Practitioner> practitioner) {
        PractitionerJpaEntity entity = practitionerRepository.findById(practitioner.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Practitioner/" + practitioner.id() + " vanished during update"));
        practitionerMapper.copyToEntity(practitioner.content(), practitioner.meta(), entity);
        practitionerRepository.save(entity);
    }

    private static Specification<PractitionerJpaEntity> matching(PractitionerSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (String family : criteria.familyPrefixes()) {
                predicates.add(startsWithIgnoringCase(cb, root.get("family"), family));
            }
            for (var identifier : criteria.identifiers()) {
                predicates.add(anyElement(root, query, cb, "identifiers",
                        (Join<PractitionerJpaEntity, EmbeddedIdentifier> element) ->
                                matchesToken(cb, element.get("system"), element.get("value"), identifier)));
            }
            return allOf(cb, predicates);
        };
    }
}
