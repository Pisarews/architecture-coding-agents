package org.archagents.fhir.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.archagents.fhir.entity.ObservationEntity;
import org.archagents.fhir.repository.EncounterRepository;
import org.archagents.fhir.repository.ObservationRepository;
import org.archagents.fhir.repository.ObservationSpecifications;
import org.archagents.fhir.repository.PatientRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ObservationService {

    private static final String RESOURCE_TYPE = "Observation";

    private final ObservationRepository observations;
    private final PatientRepository patients;
    private final EncounterRepository encounters;
    private final Clock clock;

    public ObservationService(ObservationRepository observations, PatientRepository patients,
                              EncounterRepository encounters, Clock clock) {
        this.observations = observations;
        this.patients = patients;
        this.encounters = encounters;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ObservationEntity get(long id) {
        return observations.findById(id).orElseThrow(() -> new ResourceNotFoundException(RESOURCE_TYPE, id));
    }

    public ObservationEntity create(ObservationEntity observation) {
        validate(observation);
        observation.assignFirstVersion(now());
        return observations.save(observation);
    }

    public ObservationEntity update(long id, ObservationEntity observation) {
        ObservationEntity current = get(id);
        validate(observation);
        observation.assignNextVersion(current, now());
        return observations.save(observation);
    }

    @Transactional(readOnly = true)
    public List<ObservationEntity> search(Map<String, List<String>> parameters) {
        List<Specification<ObservationEntity>> criteria = new ArrayList<>();
        parameters.forEach((name, values) -> values.forEach(value -> criteria.add(criterion(name, value))));
        return observations.findAll(Specification.allOf(criteria), SearchParameters.BY_ID);
    }

    private Specification<ObservationEntity> criterion(String name, String value) {
        return switch (name) {
            case "patient" -> ObservationSpecifications.hasSubject(SearchParameters.referenceId(name, value, "Patient"));
            case "code" -> {
                SearchParameters.Token token = SearchParameters.token(value);
                yield ObservationSpecifications.hasCode(token.system(), token.code());
            }
            case "category" -> ObservationSpecifications.hasCategory(SearchParameters.token(value).code());
            default -> throw new UnsupportedSearchParameterException(RESOURCE_TYPE, name);
        };
    }

    private void validate(ObservationEntity observation) {
        if (observation.getStatus() == null) {
            throw ResourceValidationException.missing("Observation.status is required");
        }
        if (!StringUtils.hasText(observation.getCode())) {
            throw ResourceValidationException.missing("Observation.code.coding[0].code is required");
        }
        if (observation.getSubjectReference() == null) {
            throw ResourceValidationException.missing("Observation.subject is required");
        }
        observation.setSubjectId(References.resolve(observation.getSubjectReference(), "Patient", patients));
        if (observation.getEncounterReference() != null) {
            observation.setEncounterId(
                    References.resolve(observation.getEncounterReference(), "Encounter", encounters));
        }
        if (observation.hasValueQuantity() && observation.getValueQuantity() == null) {
            throw ResourceValidationException.missing("Observation.valueQuantity.value is required");
        }
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
