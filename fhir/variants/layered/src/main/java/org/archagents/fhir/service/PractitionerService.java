package org.archagents.fhir.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.archagents.fhir.entity.PractitionerEntity;
import org.archagents.fhir.repository.PractitionerRepository;
import org.archagents.fhir.repository.PractitionerSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class PractitionerService {

    private static final String RESOURCE_TYPE = "Practitioner";

    private final PractitionerRepository practitioners;
    private final Clock clock;

    public PractitionerService(PractitionerRepository practitioners, Clock clock) {
        this.practitioners = practitioners;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PractitionerEntity get(long id) {
        return practitioners.findById(id).orElseThrow(() -> new ResourceNotFoundException(RESOURCE_TYPE, id));
    }

    public PractitionerEntity create(PractitionerEntity practitioner) {
        validate(practitioner);
        practitioner.assignFirstVersion(now());
        return practitioners.save(practitioner);
    }

    public PractitionerEntity update(long id, PractitionerEntity practitioner) {
        PractitionerEntity current = get(id);
        validate(practitioner);
        practitioner.assignNextVersion(current, now());
        return practitioners.save(practitioner);
    }

    @Transactional(readOnly = true)
    public List<PractitionerEntity> search(Map<String, List<String>> parameters) {
        List<Specification<PractitionerEntity>> criteria = new ArrayList<>();
        parameters.forEach((name, values) -> values.forEach(value -> criteria.add(criterion(name, value))));
        return practitioners.findAll(Specification.allOf(criteria), SearchParameters.BY_ID);
    }

    private Specification<PractitionerEntity> criterion(String name, String value) {
        return switch (name) {
            case "family" -> PractitionerSpecifications.familyStartsWith(value);
            case "identifier" -> {
                SearchParameters.Token token = SearchParameters.token(value);
                yield PractitionerSpecifications.hasIdentifier(token.system(), token.code());
            }
            default -> throw new UnsupportedSearchParameterException(RESOURCE_TYPE, name);
        };
    }

    private void validate(PractitionerEntity practitioner) {
        if (!StringUtils.hasText(practitioner.getFamily())) {
            throw ResourceValidationException.missing("Practitioner.name.family is required");
        }
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
