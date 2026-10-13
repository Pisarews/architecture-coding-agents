package org.archagents.fhir.controller;

import jakarta.persistence.criteria.Join;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.archagents.fhir.entity.AppointmentEntity;
import org.archagents.fhir.entity.AppointmentParticipant;
import org.archagents.fhir.repository.AppointmentRepository;
import org.archagents.fhir.repository.PatientRepository;
import org.archagents.fhir.repository.PractitionerRepository;
import org.archagents.fhir.util.DateParam;
import org.archagents.fhir.util.FhirException;
import org.archagents.fhir.util.FhirUtil;
import org.archagents.fhir.util.ResourceRef;
import org.archagents.fhir.util.SearchParams;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
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
@RequestMapping("/fhir/Appointment")
public class AppointmentController {

    private static final Set<String> SEARCH_PARAMS = Set.of("patient", "practitioner", "status", "date");
    private static final List<String> STATUSES = List.of("proposed", "booked", "cancelled", "fulfilled", "noshow");

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;

    public AppointmentController(AppointmentRepository appointmentRepository,
                                 PatientRepository patientRepository,
                                 PractitionerRepository practitionerRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.practitionerRepository = practitionerRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<String> create(@RequestBody String body) {
        Appointment appointment = FhirUtil.parse(body, Appointment.class);
        requireSupportedCodes(appointment);
        validate(appointment, null);

        AppointmentEntity entity = new AppointmentEntity();
        copyToEntity(appointment, entity);
        entity.setVersion(1);
        entity.setLastUpdated(FhirUtil.now());
        appointmentRepository.save(entity);
        return FhirUtil.created(toFhir(entity));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> read(@PathVariable String id) {
        AppointmentEntity entity = appointmentRepository.findById(FhirUtil.parseId("Appointment", id))
                .orElseThrow(() -> FhirException.notFound("Appointment", id));
        return FhirUtil.ok(toFhir(entity));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<String> update(@PathVariable String id, @RequestBody String body) {
        Appointment appointment = FhirUtil.parse(body, Appointment.class);
        requireSupportedCodes(appointment);
        FhirUtil.requireMatchingId(appointment, id);
        AppointmentEntity entity = appointmentRepository.findById(FhirUtil.parseId("Appointment", id))
                .orElseThrow(() -> FhirException.notFound("Appointment", id));
        validate(appointment, entity.getId());

        copyToEntity(appointment, entity);
        entity.setVersion(entity.getVersion() + 1);
        entity.setLastUpdated(FhirUtil.now());
        return FhirUtil.ok(toFhir(entity));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<String> search(@RequestParam MultiValueMap<String, String> params) {
        SearchParams.requireSupported(params, SEARCH_PARAMS);
        List<Specification<AppointmentEntity>> specs = new ArrayList<>();
        params.forEach((name, values) -> {
            for (String value : values) {
                specs.add(switch (name) {
                    case "patient" -> hasActor("Patient", SearchParams.referenceId(name, value, "Patient"));
                    case "practitioner" -> hasActor("Practitioner", SearchParams.referenceId(name, value, "Practitioner"));
                    case "status" -> (root, query, cb) -> cb.equal(root.get("status"), value);
                    case "date" -> {
                        DateParam date = DateParam.parse(name, value);
                        yield (root, query, cb) -> date.toPredicateOnUtcDay(cb, root.get("start"));
                    }
                    default -> throw FhirException.unsupportedParameter(name);
                });
            }
        });

        List<Appointment> appointments = appointmentRepository.findAll(Specification.allOf(specs), Sort.by("id")).stream()
                .map(AppointmentController::toFhir)
                .toList();
        return FhirUtil.searchset(appointments);
    }

    private static Specification<AppointmentEntity> hasActor(String type, long id) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<AppointmentEntity, AppointmentParticipant> participant = root.join("participants");
            return cb.and(cb.equal(participant.get("actorType"), type), cb.equal(participant.get("actorId"), id));
        };
    }

    private void validate(Appointment appointment, Long currentId) {
        if (!appointment.hasStatus()) {
            throw FhirException.required("Appointment.status is required");
        }
        if (!appointment.hasStart()) {
            throw FhirException.required("Appointment.start is required");
        }
        if (!appointment.hasEnd()) {
            throw FhirException.required("Appointment.end is required");
        }
        if (!appointment.hasParticipant()
                || !appointment.getParticipant().stream().allMatch(participant -> ResourceRef.isPresent(participant.getActor()))) {
            throw FhirException.required("Appointment must have at least one participant and every participant needs an actor");
        }

        Instant start = FhirUtil.toInstant(appointment.getStartElement());
        Instant end = FhirUtil.toInstant(appointment.getEndElement());
        if (!end.isAfter(start)) {
            throw FhirException.businessRule("Appointment.end must be after Appointment.start");
        }

        List<Long> practitionerIds = new ArrayList<>();
        for (AppointmentParticipantComponent participant : appointment.getParticipant()) {
            String actor = participant.getActor().getReference();
            ResourceRef ref = ResourceRef.parse(actor);
            boolean exists = ref != null && switch (ref.type()) {
                case "Patient" -> patientRepository.existsById(ref.id());
                case "Practitioner" -> practitionerRepository.existsById(ref.id());
                default -> false;
            };
            if (!exists) {
                throw FhirException.referenceNotFound(actor);
            }
            if (ref.type().equals("Practitioner")) {
                practitionerIds.add(ref.id());
            }
        }

        if (appointment.getStatus() == Appointment.AppointmentStatus.BOOKED) {
            for (Long practitionerId : practitionerIds) {
                if (appointmentRepository.existsOverlappingBooking(practitionerId, start, end, currentId)) {
                    throw FhirException.businessRule(
                            "Practitioner/" + practitionerId + " already has a booked appointment overlapping this time");
                }
            }
        }
    }

    private static void requireSupportedCodes(Appointment appointment) {
        FhirUtil.requireSupportedCode("Appointment.status", appointment.getStatusElement(), STATUSES);
    }

    private static void copyToEntity(Appointment appointment, AppointmentEntity entity) {
        entity.setStatus(appointment.getStatus().toCode());
        entity.setDescription(appointment.getDescription());
        entity.setStart(FhirUtil.toInstant(appointment.getStartElement()));
        entity.setEnd(FhirUtil.toInstant(appointment.getEndElement()));
        entity.getParticipants().clear();
        for (AppointmentParticipantComponent participant : appointment.getParticipant()) {
            ResourceRef actor = ResourceRef.parse(participant.getActor().getReference());
            String status = participant.hasStatus() ? participant.getStatus().toCode() : null;
            entity.getParticipants().add(new AppointmentParticipant(actor.type(), actor.id(), status));
        }
    }

    private static Appointment toFhir(AppointmentEntity entity) {
        Appointment appointment = new Appointment();
        appointment.setId(String.valueOf(entity.getId()));
        appointment.setMeta(FhirUtil.meta(entity.getVersion(), entity.getLastUpdated()));
        appointment.setStatus(Appointment.AppointmentStatus.fromCode(entity.getStatus()));
        appointment.setDescription(entity.getDescription());
        appointment.setStartElement(FhirUtil.instant(entity.getStart()));
        appointment.setEndElement(FhirUtil.instant(entity.getEnd()));
        for (AppointmentParticipant participant : entity.getParticipants()) {
            AppointmentParticipantComponent component = appointment.addParticipant()
                    .setActor(new ResourceRef(participant.getActorType(), participant.getActorId()).toReference());
            if (participant.getStatus() != null) {
                component.setStatus(Appointment.ParticipationStatus.fromCode(participant.getStatus()));
            }
        }
        return appointment;
    }
}
