package org.archagents.fhir.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.archagents.fhir.application.port.out.LoadAppointmentPort;
import org.archagents.fhir.application.port.out.ResourceExistencePort;
import org.archagents.fhir.application.port.out.SaveAppointmentPort;
import org.archagents.fhir.domain.Appointment;
import org.archagents.fhir.domain.AppointmentParticipant;
import org.archagents.fhir.domain.AppointmentSearchCriteria;
import org.archagents.fhir.domain.AppointmentStatus;
import org.archagents.fhir.domain.Reference;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.ResourceType;
import org.archagents.fhir.domain.RuleViolationException;
import org.archagents.fhir.domain.StoredResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppointmentServiceTest {

    private static final Instant NOW = Instant.parse("2031-01-01T00:00:00Z");

    private final LoadAppointmentPort loadPort = mock(LoadAppointmentPort.class);
    private final SaveAppointmentPort savePort = mock(SaveAppointmentPort.class);
    private final ResourceExistencePort existencePort = mock(ResourceExistencePort.class);
    private final AppointmentService service = new AppointmentService(
            loadPort, savePort, new ReferenceVerifier(existencePort), Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void allReferencedResourcesExist() {
        when(existencePort.exists(any(), any(Long.class))).thenReturn(true);
    }

    @Test
    void rejectsBookingThatOverlapsAnotherBookedAppointmentOfThePractitioner() {
        StoredResource<Appointment> existing = stored(20, booked("10:00", "11:00"));
        when(loadPort.searchAppointments(AppointmentSearchCriteria.bookedFor(7))).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.createAppointment(booked("10:30", "11:30")))
                .hasFieldOrPropertyWithValue("kind", RuleViolationException.Kind.BUSINESS_RULE);
        verify(savePort, never()).createAppointment(any(), any());
    }

    @Test
    void updateIgnoresTheAppointmentBeingUpdatedAndIncrementsVersion() {
        StoredResource<Appointment> current = stored(20, booked("10:00", "11:00"));
        when(loadPort.loadAppointment(20)).thenReturn(Optional.of(current));
        when(loadPort.searchAppointments(AppointmentSearchCriteria.bookedFor(7))).thenReturn(List.of(current));

        StoredResource<Appointment> updated = service.updateAppointment(20, booked("10:00", "11:15"));

        assertThat(updated.meta()).isEqualTo(new ResourceMeta(2, NOW));
        verify(savePort).updateAppointment(updated);
    }

    private static Appointment booked(String start, String end) {
        return new Appointment(AppointmentStatus.BOOKED, null,
                Instant.parse("2031-05-01T" + start + ":00Z"), Instant.parse("2031-05-01T" + end + ":00Z"),
                List.of(new AppointmentParticipant(Reference.to(ResourceType.PATIENT, 1), null),
                        new AppointmentParticipant(Reference.to(ResourceType.PRACTITIONER, 7), null)));
    }

    private static StoredResource<Appointment> stored(long id, Appointment appointment) {
        return new StoredResource<>(id, ResourceMeta.initial(NOW.minusSeconds(60)), appointment);
    }
}
