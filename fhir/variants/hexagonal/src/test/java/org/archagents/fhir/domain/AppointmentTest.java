package org.archagents.fhir.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppointmentTest {

    private static final Reference PATIENT = Reference.to(ResourceType.PATIENT, 1);
    private static final Reference DOCTOR = Reference.to(ResourceType.PRACTITIONER, 2);

    @Test
    void bookedAppointmentsOfSamePractitionerConflictOnlyWhenIntervalsOverlap() {
        Appointment morning = booked(DOCTOR, "2031-05-01T10:00:00Z", "2031-05-01T11:00:00Z");

        assertThat(morning.conflictsWith(booked(DOCTOR, "2031-05-01T10:30:00Z", "2031-05-01T11:30:00Z"))).isTrue();
        assertThat(morning.conflictsWith(booked(DOCTOR, "2031-05-01T11:00:00Z", "2031-05-01T12:00:00Z"))).isFalse();
        assertThat(morning.conflictsWith(booked(Reference.to(ResourceType.PRACTITIONER, 3),
                "2031-05-01T10:30:00Z", "2031-05-01T11:30:00Z"))).isFalse();
    }

    @Test
    void participantWithoutActorIsRejectedAsMissingRequiredElement() {
        Appointment appointment = new Appointment(AppointmentStatus.BOOKED, null,
                Instant.parse("2031-05-01T10:00:00Z"), Instant.parse("2031-05-01T11:00:00Z"),
                List.of(new AppointmentParticipant(PATIENT, null), new AppointmentParticipant(null, null)));

        assertThatThrownBy(appointment::checkRequiredElements)
                .isInstanceOfSatisfying(RuleViolationException.class,
                        e -> assertThat(e.kind()).isEqualTo(RuleViolationException.Kind.REQUIRED));
    }

    private static Appointment booked(Reference practitioner, String start, String end) {
        return new Appointment(AppointmentStatus.BOOKED, "Check-up", Instant.parse(start), Instant.parse(end),
                List.of(new AppointmentParticipant(PATIENT, ParticipationStatus.ACCEPTED),
                        new AppointmentParticipant(practitioner, ParticipationStatus.ACCEPTED)));
    }
}
