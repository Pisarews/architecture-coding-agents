package org.archagents.fhir.adapter.in.web;

import static org.archagents.fhir.adapter.in.web.FhirConversions.applyMeta;
import static org.archagents.fhir.adapter.in.web.FhirConversions.code;
import static org.archagents.fhir.adapter.in.web.FhirConversions.toReference;

import org.archagents.fhir.domain.Appointment;
import org.archagents.fhir.domain.AppointmentParticipant;
import org.archagents.fhir.domain.AppointmentStatus;
import org.archagents.fhir.domain.ParticipationStatus;
import org.archagents.fhir.domain.StoredResource;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.springframework.stereotype.Component;

@Component
class AppointmentResourceMapper {

    Appointment toDomain(org.hl7.fhir.r4.model.Appointment resource) {
        return new Appointment(
                resource.hasStatus() ? code(resource.getStatus().toCode(), AppointmentStatus::fromCode) : null,
                resource.getDescription(),
                FhirConversions.toInstant(resource.getStartElement()),
                FhirConversions.toInstant(resource.getEndElement()),
                resource.getParticipant().stream().map(AppointmentResourceMapper::toParticipant).toList());
    }

    org.hl7.fhir.r4.model.Appointment toResource(StoredResource<Appointment> stored) {
        Appointment appointment = stored.content();
        org.hl7.fhir.r4.model.Appointment resource = new org.hl7.fhir.r4.model.Appointment();
        applyMeta(resource, stored);
        resource.setStatus(
                org.hl7.fhir.r4.model.Appointment.AppointmentStatus.fromCode(appointment.status().code()));
        resource.setDescription(appointment.description());
        resource.setStartElement(FhirConversions.toInstantType(appointment.start()));
        resource.setEndElement(FhirConversions.toInstantType(appointment.end()));
        for (AppointmentParticipant participant : appointment.participants()) {
            AppointmentParticipantComponent component = resource.addParticipant()
                    .setActor(FhirConversions.toFhirReference(participant.actor()));
            if (participant.status() != null) {
                component.setStatus(org.hl7.fhir.r4.model.Appointment.ParticipationStatus.fromCode(
                        participant.status().code()));
            }
        }
        return resource;
    }

    private static AppointmentParticipant toParticipant(AppointmentParticipantComponent component) {
        return new AppointmentParticipant(
                toReference(component.getActor()),
                component.hasStatus() ? code(component.getStatus().toCode(), ParticipationStatus::fromCode) : null);
    }
}
