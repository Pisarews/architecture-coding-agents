package org.archagents.fhir.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.archagents.fhir.entity.EncounterEntity;
import org.archagents.fhir.repository.EncounterRepository;
import org.archagents.fhir.repository.EncounterSpecifications;
import org.archagents.fhir.repository.PatientRepository;
import org.archagents.fhir.repository.PractitionerRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EncounterService {

    private static final String RESOURCE_TYPE = "Encounter";

    private final EncounterRepository encounters;
    private final PatientRepository patients;
    private final PractitionerRepository practitioners;
    private final Clock clock;

    public EncounterService(EncounterRepository encounters, PatientRepository patients,
                            PractitionerRepository practitioners, Clock clock) {
        this.encounters = encounters;
        this.patients = patients;
        this.practitioners = practitioners;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EncounterEntity get(long id) {
        return encounters.findById(id).orElseThrow(() -> new ResourceNotFoundException(RESOURCE_TYPE, id));
    }

    public EncounterEntity create(EncounterEntity encounter) {
        validate(encounter);
        encounter.assignFirstVersion(now());
        return encounters.save(encounter);
    }

    public EncounterEntity update(long id, EncounterEntity encounter) {
        EncounterEntity current = get(id);
        validate(encounter);
        encounter.assignNextVersion(current, now());
        return encounters.save(encounter);
    }

    @Transactional(readOnly = true)
    public List<EncounterEntity> search(Map<String, List<String>> parameters) {
        List<Specification<EncounterEntity>> criteria = new ArrayList<>();
        parameters.forEach((name, values) -> values.forEach(value -> criteria.add(criterion(name, value))));
        return encounters.findAll(Specification.allOf(criteria), SearchParameters.BY_ID);
    }

    private Specification<EncounterEntity> criterion(String name, String value) {
        return switch (name) {
            case "patient" -> EncounterSpecifications.hasSubject(SearchParameters.referenceId(name, value, "Patient"));
            case "status" -> EncounterSpecifications.hasStatus(value);
            default -> throw new UnsupportedSearchParameterException(RESOURCE_TYPE, name);
        };
    }

    private void validate(EncounterEntity encounter) {
        if (encounter.getStatus() == null) {
            throw ResourceValidationException.missing("Encounter.status is required");
        }
        if (encounter.getSubjectReference() == null) {
            throw ResourceValidationException.missing("Encounter.subject is required");
        }
        encounter.setSubjectId(References.resolve(encounter.getSubjectReference(), "Patient", patients));
        if (encounter.getPractitionerReference() != null) {
            encounter.setPractitionerId(
                    References.resolve(encounter.getPractitionerReference(), "Practitioner", practitioners));
        }
        Instant start = encounter.getPeriodStart();
        Instant end = encounter.getPeriodEnd();
        if (start != null && end != null && end.isBefore(start)) {
            throw ResourceValidationException.businessRule("Encounter.period.end must not be before period.start");
        }
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
