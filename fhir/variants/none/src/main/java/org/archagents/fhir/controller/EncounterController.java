package org.archagents.fhir.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.archagents.fhir.entity.EncounterEntity;
import org.archagents.fhir.repository.EncounterRepository;
import org.archagents.fhir.repository.PatientRepository;
import org.archagents.fhir.repository.PractitionerRepository;
import org.archagents.fhir.util.FhirException;
import org.archagents.fhir.util.FhirUtil;
import org.archagents.fhir.util.ResourceRef;
import org.archagents.fhir.util.SearchParams;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
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
@RequestMapping("/fhir/Encounter")
public class EncounterController {

    private static final Set<String> SEARCH_PARAMS = Set.of("patient", "status");
    private static final List<String> STATUSES = List.of("planned", "in-progress", "finished", "cancelled");

    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;

    public EncounterController(EncounterRepository encounterRepository,
                               PatientRepository patientRepository,
                               PractitionerRepository practitionerRepository) {
        this.encounterRepository = encounterRepository;
        this.patientRepository = patientRepository;
        this.practitionerRepository = practitionerRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<String> create(@RequestBody String body) {
        Encounter encounter = FhirUtil.parse(body, Encounter.class);
        requireSupportedCodes(encounter);
        validate(encounter);

        EncounterEntity entity = new EncounterEntity();
        copyToEntity(encounter, entity);
        entity.setVersion(1);
        entity.setLastUpdated(FhirUtil.now());
        encounterRepository.save(entity);
        return FhirUtil.created(toFhir(entity));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> read(@PathVariable String id) {
        EncounterEntity entity = encounterRepository.findById(FhirUtil.parseId("Encounter", id))
                .orElseThrow(() -> FhirException.notFound("Encounter", id));
        return FhirUtil.ok(toFhir(entity));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<String> update(@PathVariable String id, @RequestBody String body) {
        Encounter encounter = FhirUtil.parse(body, Encounter.class);
        requireSupportedCodes(encounter);
        FhirUtil.requireMatchingId(encounter, id);
        EncounterEntity entity = encounterRepository.findById(FhirUtil.parseId("Encounter", id))
                .orElseThrow(() -> FhirException.notFound("Encounter", id));
        validate(encounter);

        copyToEntity(encounter, entity);
        entity.setVersion(entity.getVersion() + 1);
        entity.setLastUpdated(FhirUtil.now());
        return FhirUtil.ok(toFhir(entity));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<String> search(@RequestParam MultiValueMap<String, String> params) {
        SearchParams.requireSupported(params, SEARCH_PARAMS);
        List<Specification<EncounterEntity>> specs = new ArrayList<>();
        params.forEach((name, values) -> {
            for (String value : values) {
                specs.add(switch (name) {
                    case "patient" -> {
                        long patientId = SearchParams.referenceId(name, value, "Patient");
                        yield (root, query, cb) -> cb.equal(root.get("subjectId"), patientId);
                    }
                    case "status" -> (root, query, cb) -> cb.equal(root.get("status"), value);
                    default -> throw FhirException.unsupportedParameter(name);
                });
            }
        });

        List<Encounter> encounters = encounterRepository.findAll(Specification.allOf(specs), Sort.by("id")).stream()
                .map(EncounterController::toFhir)
                .toList();
        return FhirUtil.searchset(encounters);
    }

    private void validate(Encounter encounter) {
        if (!encounter.hasStatus()) {
            throw FhirException.required("Encounter.status is required");
        }
        if (!ResourceRef.isPresent(encounter.getSubject())) {
            throw FhirException.required("Encounter.subject is required");
        }

        String subject = encounter.getSubject().getReference();
        ResourceRef subjectRef = ResourceRef.parse(subject);
        if (subjectRef == null || !subjectRef.type().equals("Patient") || !patientRepository.existsById(subjectRef.id())) {
            throw FhirException.referenceNotFound(subject);
        }
        Reference individual = participantIndividual(encounter);
        if (ResourceRef.isPresent(individual)) {
            ResourceRef practitionerRef = ResourceRef.parse(individual.getReference());
            if (practitionerRef == null || !practitionerRef.type().equals("Practitioner")
                    || !practitionerRepository.existsById(practitionerRef.id())) {
                throw FhirException.referenceNotFound(individual.getReference());
            }
        }

        Period period = encounter.getPeriod();
        if (period.hasStart() && period.hasEnd() && period.getEnd().before(period.getStart())) {
            throw FhirException.businessRule("Encounter.period.end must not be before period.start");
        }
    }

    private static Reference participantIndividual(Encounter encounter) {
        return encounter.hasParticipant() ? encounter.getParticipant().get(0).getIndividual() : null;
    }

    private static void requireSupportedCodes(Encounter encounter) {
        FhirUtil.requireSupportedCode("Encounter.status", encounter.getStatusElement(), STATUSES);
    }

    private static void copyToEntity(Encounter encounter, EncounterEntity entity) {
        entity.setStatus(encounter.getStatus().toCode());
        Coding encounterClass = encounter.getClass_();
        entity.setClassSystem(encounterClass.getSystem());
        entity.setClassCode(encounterClass.getCode());
        entity.setSubjectId(ResourceRef.parse(encounter.getSubject().getReference()).id());
        Reference individual = participantIndividual(encounter);
        entity.setPractitionerId(ResourceRef.isPresent(individual) ? ResourceRef.parse(individual.getReference()).id() : null);
        entity.setPeriodStart(FhirUtil.toInstant(encounter.getPeriod().getStartElement()));
        entity.setPeriodEnd(FhirUtil.toInstant(encounter.getPeriod().getEndElement()));
    }

    private static Encounter toFhir(EncounterEntity entity) {
        Encounter encounter = new Encounter();
        encounter.setId(String.valueOf(entity.getId()));
        encounter.setMeta(FhirUtil.meta(entity.getVersion(), entity.getLastUpdated()));
        encounter.setStatus(Encounter.EncounterStatus.fromCode(entity.getStatus()));
        if (entity.getClassSystem() != null || entity.getClassCode() != null) {
            encounter.setClass_(new Coding().setSystem(entity.getClassSystem()).setCode(entity.getClassCode()));
        }
        encounter.setSubject(new ResourceRef("Patient", entity.getSubjectId()).toReference());
        if (entity.getPractitionerId() != null) {
            encounter.addParticipant().setIndividual(new ResourceRef("Practitioner", entity.getPractitionerId()).toReference());
        }
        if (entity.getPeriodStart() != null || entity.getPeriodEnd() != null) {
            encounter.setPeriod(new Period()
                    .setStartElement(FhirUtil.dateTime(entity.getPeriodStart()))
                    .setEndElement(FhirUtil.dateTime(entity.getPeriodEnd())));
        }
        return encounter;
    }
}
