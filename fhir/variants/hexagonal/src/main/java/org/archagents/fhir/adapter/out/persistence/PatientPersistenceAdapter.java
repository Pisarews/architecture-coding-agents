package org.archagents.fhir.adapter.out.persistence;

import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.allOf;
import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.anyElement;
import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.matchesDate;
import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.matchesToken;
import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.startsWithIgnoringCase;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.archagents.fhir.application.port.out.LoadPatientPort;
import org.archagents.fhir.application.port.out.SavePatientPort;
import org.archagents.fhir.domain.Patient;
import org.archagents.fhir.domain.PatientSearchCriteria;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
class PatientPersistenceAdapter implements LoadPatientPort, SavePatientPort {

    private final PatientRepository patientRepository;
    private final PatientPersistenceMapper patientMapper;

    PatientPersistenceAdapter(PatientRepository patientRepository, PatientPersistenceMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
    }

    @Override
    public Optional<StoredResource<Patient>> loadPatient(long id) {
        return patientRepository.findById(id).map(patientMapper::toDomain);
    }

    @Override
    public List<StoredResource<Patient>> searchPatients(PatientSearchCriteria criteria) {
        return patientRepository.findAll(matching(criteria), Sort.by("id")).stream()
                .map(patientMapper::toDomain)
                .toList();
    }

    @Override
    public StoredResource<Patient> createPatient(Patient patient, ResourceMeta meta) {
        PatientJpaEntity entity = new PatientJpaEntity();
        patientMapper.copyToEntity(patient, meta, entity);
        return patientMapper.toDomain(patientRepository.save(entity));
    }

    @Override
    public void updatePatient(StoredResource<Patient> patient) {
        PatientJpaEntity entity = patientRepository.findById(patient.id())
                .orElseThrow(() -> new IllegalStateException("Patient/" + patient.id() + " vanished during update"));
        patientMapper.copyToEntity(patient.content(), patient.meta(), entity);
        patientRepository.save(entity);
    }

    private static Specification<PatientJpaEntity> matching(PatientSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (String family : criteria.familyPrefixes()) {
                predicates.add(startsWithIgnoringCase(cb, root.get("family"), family));
            }
            for (String given : criteria.givenPrefixes()) {
                predicates.add(anyElement(root, query, cb, "given",
                        (Join<PatientJpaEntity, String> name) -> startsWithIgnoringCase(cb, name, given)));
            }
            for (var identifier : criteria.identifiers()) {
                predicates.add(anyElement(root, query, cb, "identifiers",
                        (Join<PatientJpaEntity, EmbeddedIdentifier> element) ->
                                matchesToken(cb, element.get("system"), element.get("value"), identifier)));
            }
            for (String gender : criteria.genders()) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }
            for (var birthDate : criteria.birthDates()) {
                predicates.add(matchesDate(cb, root.get("birthDate"), birthDate));
            }
            return allOf(cb, predicates);
        };
    }
}
