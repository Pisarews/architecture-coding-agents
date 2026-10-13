package org.archagents.fhir.domain;

import java.time.Instant;
import java.util.List;

public record Appointment(
        AppointmentStatus status,
        String description,
        Instant start,
        Instant end,
        List<AppointmentParticipant> participants) {

    public Appointment {
        participants = List.copyOf(participants);
    }

    public void checkRequiredElements() {
        if (status == null) {
            throw RuleViolationException.required("Appointment.status is required");
        }
        if (start == null || end == null) {
            throw RuleViolationException.required("Appointment.start and Appointment.end are required");
        }
        if (participants.isEmpty() || participants.stream().anyMatch(p -> p.actor() == null)) {
            throw RuleViolationException.required("Appointment requires at least one participant, each with an actor");
        }
    }

    public void checkTimeRange() {
        if (!end.isAfter(start)) {
            throw RuleViolationException.businessRule("Appointment.end must be after Appointment.start");
        }
    }

    public boolean isBooked() {
        return status == AppointmentStatus.BOOKED;
    }

    public List<Long> practitionerIds() {
        return participants.stream()
                .map(AppointmentParticipant::actor)
                .filter(actor -> actor.pointsTo(ResourceType.PRACTITIONER))
                .map(Reference::idAsLong)
                .toList();
    }

    public boolean overlaps(Appointment other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** Two booked appointments conflict when they share a practitioner in overlapping time slots. */
    public boolean conflictsWith(Appointment other) {
        return isBooked()
                && other.isBooked()
                && overlaps(other)
                && practitionerIds().stream().anyMatch(other.practitionerIds()::contains);
    }
}
