package org.archagents.fhir.domain;

public record AppointmentParticipant(Reference actor, ParticipationStatus status) {
}
