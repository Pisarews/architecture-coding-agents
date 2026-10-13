package org.archagents.fhir.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.archagents.fhir.entity.AppointmentEntity;
import org.archagents.fhir.entity.AppointmentParticipant;
import org.archagents.fhir.repository.AppointmentRepository;
import org.archagents.fhir.repository.AppointmentSpecifications;
import org.archagents.fhir.repository.PatientRepository;
import org.archagents.fhir.repository.PractitionerRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppointmentService {

    private static final String RESOURCE_TYPE = "Appointment";

    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final PractitionerRepository practitioners;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointments, PatientRepository patients,
                              PractitionerRepository practitioners, Clock clock) {
        this.appointments = appointments;
        this.patients = patients;
        this.practitioners = practitioners;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AppointmentEntity get(long id) {
        return appointments.findById(id).orElseThrow(() -> new ResourceNotFoundException(RESOURCE_TYPE, id));
    }

    public AppointmentEntity create(AppointmentEntity appointment) {
        validate(appointment, null);
        appointment.assignFirstVersion(now());
        return appointments.save(appointment);
    }

    public AppointmentEntity update(long id, AppointmentEntity appointment) {
        AppointmentEntity current = get(id);
        validate(appointment, id);
        appointment.assignNextVersion(current, now());
        return appointments.save(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentEntity> search(Map<String, List<String>> parameters) {
        List<Specification<AppointmentEntity>> criteria = new ArrayList<>();
        parameters.forEach((name, values) -> values.forEach(value -> criteria.add(criterion(name, value))));
        return appointments.findAll(Specification.allOf(criteria), SearchParameters.BY_ID);
    }

    private Specification<AppointmentEntity> criterion(String name, String value) {
        return switch (name) {
            case "patient" -> AppointmentSpecifications.hasParticipant(AppointmentParticipant.PATIENT,
                    SearchParameters.referenceId(name, value, AppointmentParticipant.PATIENT));
            case "practitioner" -> AppointmentSpecifications.hasParticipant(AppointmentParticipant.PRACTITIONER,
                    SearchParameters.referenceId(name, value, AppointmentParticipant.PRACTITIONER));
            case "status" -> AppointmentSpecifications.hasStatus(value);
            case "date" -> AppointmentSpecifications.startDate(SearchParameters.date(name, value));
            default -> throw new UnsupportedSearchParameterException(RESOURCE_TYPE, name);
        };
    }

    private void validate(AppointmentEntity appointment, Long appointmentId) {
        if (appointment.getStatus() == null) {
            throw ResourceValidationException.missing("Appointment.status is required");
        }
        if (appointment.getStart() == null || appointment.getEnd() == null) {
            throw ResourceValidationException.missing("Appointment.start and Appointment.end are required");
        }
        if (appointment.getParticipants().isEmpty()
                || appointment.getParticipants().stream().anyMatch(p -> p.getActorReference() == null)) {
            throw ResourceValidationException.missing("Appointment requires at least one participant and every participant needs an actor");
        }
        if (!appointment.getEnd().isAfter(appointment.getStart())) {
            throw ResourceValidationException.businessRule("Appointment.end must be after Appointment.start");
        }
        for (AppointmentParticipant participant : appointment.getParticipants()) {
            References.Target actor = References.parse(participant.getActorReference())
                    .filter(this::actorExists)
                    .orElseThrow(() -> ResourceValidationException.referenceNotFound(participant.getActorReference()));
            participant.resolveActor(actor.resourceType(), actor.id());
        }
        if (AppointmentEntity.STATUS_BOOKED.equals(appointment.getStatus())) {
            checkPractitionersAvailable(appointment, appointmentId);
        }
    }

    private boolean actorExists(References.Target actor) {
        return switch (actor.resourceType()) {
            case AppointmentParticipant.PATIENT -> patients.existsById(actor.id());
            case AppointmentParticipant.PRACTITIONER -> practitioners.existsById(actor.id());
            default -> false;
        };
    }

    private void checkPractitionersAvailable(AppointmentEntity appointment, Long appointmentId) {
        appointment.getParticipants().stream()
                .filter(p -> AppointmentParticipant.PRACTITIONER.equals(p.getActorType()))
                .forEach(p -> {
                    long overlapping = appointments.countOverlappingBookings(
                            p.getActorId(), appointment.getStart(), appointment.getEnd(), appointmentId);
                    if (overlapping > 0) {
                        throw ResourceValidationException.businessRule(
                                "Practitioner/" + p.getActorId() + " already has a booked appointment in this time slot");
                    }
                });
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
