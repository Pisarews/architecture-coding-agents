package org.archagents.fhir.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.archagents.fhir.entity.PatientEntity;
import org.archagents.fhir.repository.PatientRepository;
import org.archagents.fhir.repository.PatientSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class PatientService {

    private static final String RESOURCE_TYPE = "Patient";

    private final PatientRepository patients;
    private final Clock clock;

    public PatientService(PatientRepository patients, Clock clock) {
        this.patients = patients;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PatientEntity get(long id) {
        return patients.findById(id).orElseThrow(() -> new ResourceNotFoundException(RESOURCE_TYPE, id));
    }

    public PatientEntity create(PatientEntity patient) {
        validate(patient);
        patient.assignFirstVersion(now());
        return patients.save(patient);
    }

    public PatientEntity update(long id, PatientEntity patient) {
        PatientEntity current = get(id);
        validate(patient);
        patient.assignNextVersion(current, now());
        return patients.save(patient);
    }

    @Transactional(readOnly = true)
    public List<PatientEntity> search(Map<String, List<String>> parameters) {
        List<Specification<PatientEntity>> criteria = new ArrayList<>();
        parameters.forEach((name, values) -> values.forEach(value -> criteria.add(criterion(name, value))));
        return patients.findAll(Specification.allOf(criteria), SearchParameters.BY_ID);
    }

    private Specification<PatientEntity> criterion(String name, String value) {
        return switch (name) {
            case "family" -> PatientSpecifications.familyStartsWith(value);
            case "given" -> PatientSpecifications.givenStartsWith(value);
            case "identifier" -> {
                SearchParameters.Token token = SearchParameters.token(value);
                yield PatientSpecifications.hasIdentifier(token.system(), token.code());
            }
            case "gender" -> PatientSpecifications.hasGender(value);
            case "birthdate" -> PatientSpecifications.birthDate(SearchParameters.date(name, value));
            default -> throw new UnsupportedSearchParameterException(RESOURCE_TYPE, name);
        };
    }

    private void validate(PatientEntity patient) {
        boolean hasIdentifierValue = patient.getIdentifiers().stream().anyMatch(i -> StringUtils.hasText(i.getValue()));
        if (!StringUtils.hasText(patient.getFamily()) && !hasIdentifierValue) {
            throw ResourceValidationException.missing("Patient requires name.family or an identifier with a value");
        }
        if (patient.getBirthDate() != null && patient.getBirthDate().isAfter(LocalDate.now(clock))) {
            throw ResourceValidationException.businessRule("Patient.birthDate must not be in the future");
        }
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
