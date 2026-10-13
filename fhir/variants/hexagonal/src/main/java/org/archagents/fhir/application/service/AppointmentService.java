package org.archagents.fhir.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.archagents.fhir.application.port.in.FindAppointmentsQuery;
import org.archagents.fhir.application.port.in.ResourceNotFoundException;
import org.archagents.fhir.application.port.in.SaveAppointmentUseCase;
import org.archagents.fhir.application.port.out.LoadAppointmentPort;
import org.archagents.fhir.application.port.out.SaveAppointmentPort;
import org.archagents.fhir.domain.Appointment;
import org.archagents.fhir.domain.AppointmentParticipant;
import org.archagents.fhir.domain.AppointmentSearchCriteria;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.ResourceType;
import org.archagents.fhir.domain.RuleViolationException;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.transaction.annotation.Transactional;

public class AppointmentService implements SaveAppointmentUseCase, FindAppointmentsQuery {

    private final LoadAppointmentPort loadAppointmentPort;
    private final SaveAppointmentPort saveAppointmentPort;
    private final ReferenceVerifier references;
    private final Clock clock;

    public AppointmentService(
            LoadAppointmentPort loadAppointmentPort,
            SaveAppointmentPort saveAppointmentPort,
            ReferenceVerifier references,
            Clock clock) {
        this.loadAppointmentPort = loadAppointmentPort;
        this.saveAppointmentPort = saveAppointmentPort;
        this.references = references;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StoredResource<Appointment> createAppointment(Appointment appointment) {
        validate(null, appointment);
        return saveAppointmentPort.createAppointment(appointment, ResourceMeta.initial(now()));
    }

    @Override
    @Transactional
    public StoredResource<Appointment> updateAppointment(long id, Appointment appointment) {
        StoredResource<Appointment> current = getAppointment(id);
        validate(id, appointment);
        StoredResource<Appointment> updated = current.revise(appointment, now());
        saveAppointmentPort.updateAppointment(updated);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public StoredResource<Appointment> getAppointment(long id) {
        return loadAppointmentPort.loadAppointment(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.APPOINTMENT, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredResource<Appointment>> searchAppointments(AppointmentSearchCriteria criteria) {
        return loadAppointmentPort.searchAppointments(criteria);
    }

    private void validate(Long appointmentId, Appointment appointment) {
        appointment.checkRequiredElements();
        appointment.checkTimeRange();
        for (AppointmentParticipant participant : appointment.participants()) {
            references.verify(participant.actor(), "Appointment.participant.actor",
                    ResourceType.PATIENT, ResourceType.PRACTITIONER);
        }
        checkPractitionersAreAvailable(appointmentId, appointment);
    }

    private void checkPractitionersAreAvailable(Long appointmentId, Appointment appointment) {
        if (!appointment.isBooked()) {
            return;
        }
        for (long practitionerId : appointment.practitionerIds()) {
            boolean doubleBooked = loadAppointmentPort
                    .searchAppointments(AppointmentSearchCriteria.bookedFor(practitionerId))
                    .stream()
                    .filter(other -> !Objects.equals(other.id(), appointmentId))
                    .anyMatch(other -> appointment.conflictsWith(other.content()));
            if (doubleBooked) {
                throw RuleViolationException.businessRule(
                        "Practitioner/" + practitionerId + " already has a booked appointment in this time slot");
            }
        }
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
