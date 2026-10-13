package org.archagents.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.archagents.fhir.entity.AppointmentEntity;
import org.archagents.fhir.entity.AppointmentParticipant;
import org.archagents.fhir.entity.PractitionerEntity;
import org.archagents.fhir.repository.PractitionerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({AppointmentService.class, AppointmentServiceTest.FixedClock.class})
class AppointmentServiceTest {

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    AppointmentService appointmentService;

    @Autowired
    PractitionerRepository practitionerRepository;

    private long practitionerId;

    @BeforeEach
    void createPractitioner() {
        PractitionerEntity practitioner = new PractitionerEntity();
        practitioner.setFamily("Lekarz");
        practitioner.assignFirstVersion(Instant.parse("2030-01-01T00:00:00Z"));
        practitionerId = practitionerRepository.save(practitioner).getId();
    }

    @Test
    void rejectsOverlappingBookingForSamePractitioner() {
        appointmentService.create(booked("2031-05-01T10:00:00Z", "2031-05-01T11:00:00Z"));

        assertThatThrownBy(() -> appointmentService.create(booked("2031-05-01T10:59:00Z", "2031-05-01T11:30:00Z")))
                .isInstanceOf(ResourceValidationException.class)
                .extracting(e -> ((ResourceValidationException) e).getViolation())
                .isEqualTo(ResourceValidationException.Violation.BUSINESS_RULE);
    }

    @Test
    void updateDoesNotConflictWithItself() {
        AppointmentEntity created = appointmentService.create(booked("2031-05-02T10:00:00Z", "2031-05-02T11:00:00Z"));

        AppointmentEntity updated = appointmentService.update(created.getId(),
                booked("2031-05-02T10:30:00Z", "2031-05-02T11:30:00Z"));

        assertThat(updated.getVersion()).isEqualTo(2);
        assertThat(updated.getStart()).isEqualTo(Instant.parse("2031-05-02T10:30:00Z"));
    }

    private AppointmentEntity booked(String start, String end) {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setStatus(AppointmentEntity.STATUS_BOOKED);
        appointment.setStart(Instant.parse(start));
        appointment.setEnd(Instant.parse(end));
        appointment.setParticipants(List.of(
                new AppointmentParticipant("Practitioner/" + practitionerId, "accepted")));
        return appointment;
    }
}
