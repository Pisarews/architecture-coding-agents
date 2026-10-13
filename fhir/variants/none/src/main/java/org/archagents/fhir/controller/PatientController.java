package org.archagents.fhir.controller;

import jakarta.persistence.criteria.Join;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.archagents.fhir.entity.IdentifierValue;
import org.archagents.fhir.entity.PatientEntity;
import org.archagents.fhir.entity.Telecom;
import org.archagents.fhir.repository.PatientRepository;
import org.archagents.fhir.util.DateParam;
import org.archagents.fhir.util.FhirException;
import org.archagents.fhir.util.FhirUtil;
import org.archagents.fhir.util.SearchParams;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
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
@RequestMapping("/fhir/Patient")
public class PatientController {

    private static final Set<String> SEARCH_PARAMS = Set.of("family", "given", "identifier", "gender", "birthdate");
    private static final List<String> TELECOM_SYSTEMS = List.of("phone", "email");

    private final PatientRepository patientRepository;

    public PatientController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<String> create(@RequestBody String body) {
        Patient patient = FhirUtil.parse(body, Patient.class);
        requireSupportedCodes(patient);
        validate(patient);

        PatientEntity entity = new PatientEntity();
        copyToEntity(patient, entity);
        entity.setVersion(1);
        entity.setLastUpdated(FhirUtil.now());
        patientRepository.save(entity);
        return FhirUtil.created(toFhir(entity));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> read(@PathVariable String id) {
        PatientEntity entity = patientRepository.findById(FhirUtil.parseId("Patient", id))
                .orElseThrow(() -> FhirException.notFound("Patient", id));
        return FhirUtil.ok(toFhir(entity));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<String> update(@PathVariable String id, @RequestBody String body) {
        Patient patient = FhirUtil.parse(body, Patient.class);
        requireSupportedCodes(patient);
        FhirUtil.requireMatchingId(patient, id);
        PatientEntity entity = patientRepository.findById(FhirUtil.parseId("Patient", id))
                .orElseThrow(() -> FhirException.notFound("Patient", id));
        validate(patient);

        copyToEntity(patient, entity);
        entity.setVersion(entity.getVersion() + 1);
        entity.setLastUpdated(FhirUtil.now());
        return FhirUtil.ok(toFhir(entity));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<String> search(@RequestParam MultiValueMap<String, String> params) {
        SearchParams.requireSupported(params, SEARCH_PARAMS);
        List<Specification<PatientEntity>> specs = new ArrayList<>();
        params.forEach((name, values) -> {
            for (String value : values) {
                specs.add(switch (name) {
                    case "family" -> (root, query, cb) ->
                            cb.like(cb.lower(root.get("family")), SearchParams.likePrefix(value), '\\');
                    case "given" -> (root, query, cb) -> {
                        query.distinct(true);
                        Join<PatientEntity, String> given = root.join("given");
                        return cb.like(cb.lower(given), SearchParams.likePrefix(value), '\\');
                    };
                    case "identifier" -> {
                        SearchParams.Token token = SearchParams.Token.parse(value);
                        yield (root, query, cb) -> {
                            query.distinct(true);
                            Join<PatientEntity, IdentifierValue> identifier = root.join("identifiers");
                            return token.toPredicate(cb, identifier.get("system"), identifier.get("value"));
                        };
                    }
                    case "gender" -> (root, query, cb) -> cb.equal(root.get("gender"), value);
                    case "birthdate" -> {
                        DateParam date = DateParam.parse(name, value);
                        yield (root, query, cb) -> date.toPredicate(cb, root.get("birthDate"));
                    }
                    default -> throw FhirException.unsupportedParameter(name);
                });
            }
        });

        List<Patient> patients = patientRepository.findAll(Specification.allOf(specs), Sort.by("id")).stream()
                .map(PatientController::toFhir)
                .toList();
        return FhirUtil.searchset(patients);
    }

    private static void validate(Patient patient) {
        boolean hasFamily = patient.hasName() && patient.getName().get(0).hasFamily();
        boolean hasIdentifier = patient.getIdentifier().stream().anyMatch(Identifier::hasValue);
        if (!hasFamily && !hasIdentifier) {
            throw FhirException.required("Patient must have name[0].family or an identifier with a value");
        }
        if (patient.hasBirthDate() && FhirUtil.toLocalDate(patient.getBirthDateElement()).isAfter(LocalDate.now(ZoneOffset.UTC))) {
            throw FhirException.businessRule("Patient.birthDate must not be in the future");
        }
    }

    private static void requireSupportedCodes(Patient patient) {
        for (ContactPoint telecom : patient.getTelecom()) {
            FhirUtil.requireSupportedCode("Patient.telecom.system", telecom.getSystemElement(), TELECOM_SYSTEMS);
        }
    }

    private static void copyToEntity(Patient patient, PatientEntity entity) {
        entity.setActive(patient.hasActive() ? patient.getActive() : null);

        HumanName name = patient.hasName() ? patient.getName().get(0) : null;
        entity.setFamily(name != null && name.hasFamily() ? name.getFamily() : null);
        entity.getGiven().clear();
        if (name != null) {
            name.getGiven().stream().filter(StringType::hasValue).map(StringType::getValue).forEach(entity.getGiven()::add);
        }

        entity.getIdentifiers().clear();
        for (Identifier identifier : patient.getIdentifier()) {
            entity.getIdentifiers().add(new IdentifierValue(identifier.getSystem(), identifier.getValue()));
        }

        entity.getTelecoms().clear();
        for (ContactPoint telecom : patient.getTelecom()) {
            String system = telecom.hasSystem() ? telecom.getSystem().toCode() : null;
            entity.getTelecoms().add(new Telecom(system, telecom.getValue()));
        }

        entity.setGender(patient.hasGender() ? patient.getGender().toCode() : null);
        entity.setBirthDate(patient.hasBirthDate() ? FhirUtil.toLocalDate(patient.getBirthDateElement()) : null);
    }

    private static Patient toFhir(PatientEntity entity) {
        Patient patient = new Patient();
        patient.setId(String.valueOf(entity.getId()));
        patient.setMeta(FhirUtil.meta(entity.getVersion(), entity.getLastUpdated()));
        for (IdentifierValue identifier : entity.getIdentifiers()) {
            patient.addIdentifier().setSystem(identifier.getSystem()).setValue(identifier.getValue());
        }
        if (entity.getActive() != null) {
            patient.setActive(entity.getActive());
        }
        if (entity.getFamily() != null || !entity.getGiven().isEmpty()) {
            HumanName name = patient.addName().setFamily(entity.getFamily());
            entity.getGiven().forEach(name::addGiven);
        }
        for (Telecom telecom : entity.getTelecoms()) {
            ContactPoint contactPoint = patient.addTelecom().setValue(telecom.getValue());
            if (telecom.getSystem() != null) {
                contactPoint.setSystem(ContactPoint.ContactPointSystem.fromCode(telecom.getSystem()));
            }
        }
        if (entity.getGender() != null) {
            patient.setGender(AdministrativeGender.fromCode(entity.getGender()));
        }
        patient.setBirthDateElement(FhirUtil.date(entity.getBirthDate()));
        return patient;
    }
}
