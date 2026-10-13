package org.archagents.fhir.controller;

import jakarta.persistence.criteria.Join;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.archagents.fhir.entity.IdentifierValue;
import org.archagents.fhir.entity.PractitionerEntity;
import org.archagents.fhir.repository.PractitionerRepository;
import org.archagents.fhir.util.FhirException;
import org.archagents.fhir.util.FhirUtil;
import org.archagents.fhir.util.SearchParams;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir/Practitioner")
public class PractitionerController {

    private static final Set<String> SEARCH_PARAMS = Set.of("family", "identifier");

    private final PractitionerRepository practitionerRepository;

    public PractitionerController(PractitionerRepository practitionerRepository) {
        this.practitionerRepository = practitionerRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<String> create(@RequestBody String body) {
        Practitioner practitioner = FhirUtil.parse(body, Practitioner.class);
        validate(practitioner);

        PractitionerEntity entity = new PractitionerEntity();
        copyToEntity(practitioner, entity);
        entity.setVersion(1);
        entity.setLastUpdated(FhirUtil.now());
        practitionerRepository.save(entity);
        return FhirUtil.created(toFhir(entity));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> read(@PathVariable String id) {
        PractitionerEntity entity = practitionerRepository.findById(FhirUtil.parseId("Practitioner", id))
                .orElseThrow(() -> FhirException.notFound("Practitioner", id));
        return FhirUtil.ok(toFhir(entity));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<String> update(@PathVariable String id, @RequestBody String body) {
        Practitioner practitioner = FhirUtil.parse(body, Practitioner.class);
        FhirUtil.requireMatchingId(practitioner, id);
        PractitionerEntity entity = practitionerRepository.findById(FhirUtil.parseId("Practitioner", id))
                .orElseThrow(() -> FhirException.notFound("Practitioner", id));
        validate(practitioner);

        copyToEntity(practitioner, entity);
        entity.setVersion(entity.getVersion() + 1);
        entity.setLastUpdated(FhirUtil.now());
        return FhirUtil.ok(toFhir(entity));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<String> search(@RequestParam MultiValueMap<String, String> params) {
        SearchParams.requireSupported(params, SEARCH_PARAMS);
        List<Specification<PractitionerEntity>> specs = new ArrayList<>();
        params.forEach((name, values) -> {
            for (String value : values) {
                specs.add(switch (name) {
                    case "family" -> (root, query, cb) ->
                            cb.like(cb.lower(root.get("family")), SearchParams.likePrefix(value), '\\');
                    case "identifier" -> {
                        SearchParams.Token token = SearchParams.Token.parse(value);
                        yield (root, query, cb) -> {
                            query.distinct(true);
                            Join<PractitionerEntity, IdentifierValue> identifier = root.join("identifiers");
                            return token.toPredicate(cb, identifier.get("system"), identifier.get("value"));
                        };
                    }
                    default -> throw FhirException.unsupportedParameter(name);
                });
            }
        });

        List<Practitioner> practitioners = practitionerRepository.findAll(Specification.allOf(specs), Sort.by("id")).stream()
                .map(PractitionerController::toFhir)
                .toList();
        return FhirUtil.searchset(practitioners);
    }

    private static void validate(Practitioner practitioner) {
        if (!practitioner.hasName() || !practitioner.getName().get(0).hasFamily()) {
            throw FhirException.required("Practitioner.name[0].family is required");
        }
    }

    private static void copyToEntity(Practitioner practitioner, PractitionerEntity entity) {
        entity.setActive(practitioner.hasActive() ? practitioner.getActive() : null);

        HumanName name = practitioner.getName().get(0);
        entity.setFamily(name.getFamily());
        entity.getGiven().clear();
        name.getGiven().stream().filter(StringType::hasValue).map(StringType::getValue).forEach(entity.getGiven()::add);

        entity.getIdentifiers().clear();
        for (Identifier identifier : practitioner.getIdentifier()) {
            entity.getIdentifiers().add(new IdentifierValue(identifier.getSystem(), identifier.getValue()));
        }
    }

    private static Practitioner toFhir(PractitionerEntity entity) {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(String.valueOf(entity.getId()));
        practitioner.setMeta(FhirUtil.meta(entity.getVersion(), entity.getLastUpdated()));
        for (IdentifierValue identifier : entity.getIdentifiers()) {
            practitioner.addIdentifier().setSystem(identifier.getSystem()).setValue(identifier.getValue());
        }
        if (entity.getActive() != null) {
            practitioner.setActive(entity.getActive());
        }
        HumanName name = practitioner.addName().setFamily(entity.getFamily());
        entity.getGiven().forEach(name::addGiven);
        return practitioner;
    }
}
