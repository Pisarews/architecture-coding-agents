package org.archagents.fhir.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.archagents.fhir.entity.ObservationEntity;
import org.archagents.fhir.repository.EncounterRepository;
import org.archagents.fhir.repository.ObservationRepository;
import org.archagents.fhir.repository.PatientRepository;
import org.archagents.fhir.util.FhirException;
import org.archagents.fhir.util.FhirUtil;
import org.archagents.fhir.util.ResourceRef;
import org.archagents.fhir.util.SearchParams;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
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
@RequestMapping("/fhir/Observation")
public class ObservationController {

    private static final Set<String> SEARCH_PARAMS = Set.of("patient", "code", "category");
    private static final List<String> STATUSES =
            List.of("registered", "preliminary", "final", "amended", "cancelled", "entered-in-error");

    private final ObservationRepository observationRepository;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;

    public ObservationController(ObservationRepository observationRepository,
                                 PatientRepository patientRepository,
                                 EncounterRepository encounterRepository) {
        this.observationRepository = observationRepository;
        this.patientRepository = patientRepository;
        this.encounterRepository = encounterRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<String> create(@RequestBody String body) {
        Observation observation = FhirUtil.parse(body, Observation.class);
        requireSupportedCodes(observation);
        validate(observation);

        ObservationEntity entity = new ObservationEntity();
        copyToEntity(observation, entity);
        entity.setVersion(1);
        entity.setLastUpdated(FhirUtil.now());
        observationRepository.save(entity);
        return FhirUtil.created(toFhir(entity));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> read(@PathVariable String id) {
        ObservationEntity entity = observationRepository.findById(FhirUtil.parseId("Observation", id))
                .orElseThrow(() -> FhirException.notFound("Observation", id));
        return FhirUtil.ok(toFhir(entity));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<String> update(@PathVariable String id, @RequestBody String body) {
        Observation observation = FhirUtil.parse(body, Observation.class);
        requireSupportedCodes(observation);
        FhirUtil.requireMatchingId(observation, id);
        ObservationEntity entity = observationRepository.findById(FhirUtil.parseId("Observation", id))
                .orElseThrow(() -> FhirException.notFound("Observation", id));
        validate(observation);

        copyToEntity(observation, entity);
        entity.setVersion(entity.getVersion() + 1);
        entity.setLastUpdated(FhirUtil.now());
        return FhirUtil.ok(toFhir(entity));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<String> search(@RequestParam MultiValueMap<String, String> params) {
        SearchParams.requireSupported(params, SEARCH_PARAMS);
        List<Specification<ObservationEntity>> specs = new ArrayList<>();
        params.forEach((name, values) -> {
            for (String value : values) {
                specs.add(switch (name) {
                    case "patient" -> {
                        long patientId = SearchParams.referenceId(name, value, "Patient");
                        yield (root, query, cb) -> cb.equal(root.get("subjectId"), patientId);
                    }
                    case "code" -> {
                        SearchParams.Token token = SearchParams.Token.parse(value);
                        yield (root, query, cb) -> token.toPredicate(cb, root.get("codeSystem"), root.get("codeCode"));
                    }
                    case "category" -> {
                        String code = SearchParams.Token.parse(value).code();
                        yield (root, query, cb) -> cb.equal(root.get("categoryCode"), code);
                    }
                    default -> throw FhirException.unsupportedParameter(name);
                });
            }
        });

        List<Observation> observations = observationRepository.findAll(Specification.allOf(specs), Sort.by("id")).stream()
                .map(ObservationController::toFhir)
                .toList();
        return FhirUtil.searchset(observations);
    }

    private void validate(Observation observation) {
        if (!observation.hasStatus()) {
            throw FhirException.required("Observation.status is required");
        }
        Coding code = observation.getCode().hasCoding() ? observation.getCode().getCoding().get(0) : null;
        if (code == null || !code.hasCode()) {
            throw FhirException.required("Observation.code.coding[0].code is required");
        }
        if (!ResourceRef.isPresent(observation.getSubject())) {
            throw FhirException.required("Observation.subject is required");
        }

        String subject = observation.getSubject().getReference();
        ResourceRef subjectRef = ResourceRef.parse(subject);
        if (subjectRef == null || !subjectRef.type().equals("Patient") || !patientRepository.existsById(subjectRef.id())) {
            throw FhirException.referenceNotFound(subject);
        }
        if (ResourceRef.isPresent(observation.getEncounter())) {
            String encounter = observation.getEncounter().getReference();
            ResourceRef encounterRef = ResourceRef.parse(encounter);
            if (encounterRef == null || !encounterRef.type().equals("Encounter") || !encounterRepository.existsById(encounterRef.id())) {
                throw FhirException.referenceNotFound(encounter);
            }
        }

        if (observation.hasValueQuantity() && !observation.getValueQuantity().hasValue()) {
            throw FhirException.required("Observation.valueQuantity.value is required when valueQuantity is present");
        }
    }

    private static void requireSupportedCodes(Observation observation) {
        FhirUtil.requireSupportedCode("Observation.status", observation.getStatusElement(), STATUSES);
    }

    private static void copyToEntity(Observation observation, ObservationEntity entity) {
        entity.setStatus(observation.getStatus().toCode());

        Coding category = observation.hasCategory() && observation.getCategory().get(0).hasCoding()
                ? observation.getCategory().get(0).getCoding().get(0)
                : new Coding();
        entity.setCategorySystem(category.getSystem());
        entity.setCategoryCode(category.getCode());

        Coding code = observation.getCode().getCoding().get(0);
        entity.setCodeSystem(code.getSystem());
        entity.setCodeCode(code.getCode());
        entity.setCodeDisplay(code.getDisplay());

        entity.setSubjectId(ResourceRef.parse(observation.getSubject().getReference()).id());
        entity.setEncounterId(ResourceRef.isPresent(observation.getEncounter())
                ? ResourceRef.parse(observation.getEncounter().getReference()).id()
                : null);
        entity.setEffective(observation.hasEffectiveDateTimeType()
                ? FhirUtil.toInstant(observation.getEffectiveDateTimeType())
                : null);

        Quantity quantity = observation.hasValueQuantity() ? observation.getValueQuantity() : new Quantity();
        entity.setValueValue(quantity.getValue());
        entity.setValueUnit(quantity.getUnit());
        entity.setValueSystem(quantity.getSystem());
        entity.setValueCode(quantity.getCode());
    }

    private static Observation toFhir(ObservationEntity entity) {
        Observation observation = new Observation();
        observation.setId(String.valueOf(entity.getId()));
        observation.setMeta(FhirUtil.meta(entity.getVersion(), entity.getLastUpdated()));
        observation.setStatus(Observation.ObservationStatus.fromCode(entity.getStatus()));
        if (entity.getCategorySystem() != null || entity.getCategoryCode() != null) {
            observation.addCategory(new CodeableConcept(
                    new Coding().setSystem(entity.getCategorySystem()).setCode(entity.getCategoryCode())));
        }
        observation.setCode(new CodeableConcept(new Coding()
                .setSystem(entity.getCodeSystem())
                .setCode(entity.getCodeCode())
                .setDisplay(entity.getCodeDisplay())));
        observation.setSubject(new ResourceRef("Patient", entity.getSubjectId()).toReference());
        if (entity.getEncounterId() != null) {
            observation.setEncounter(new ResourceRef("Encounter", entity.getEncounterId()).toReference());
        }
        if (entity.getEffective() != null) {
            observation.setEffective(FhirUtil.dateTime(entity.getEffective()));
        }
        if (entity.getValueValue() != null) {
            observation.setValue(new Quantity()
                    .setValue(FhirUtil.normalize(entity.getValueValue()))
                    .setUnit(entity.getValueUnit())
                    .setSystem(entity.getValueSystem())
                    .setCode(entity.getValueCode()));
        }
        return observation;
    }
}
