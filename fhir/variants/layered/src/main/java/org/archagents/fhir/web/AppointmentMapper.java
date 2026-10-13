package org.archagents.fhir.web;

import org.archagents.fhir.entity.AppointmentEntity;
import org.archagents.fhir.entity.AppointmentParticipant;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.Appointment.ParticipationStatus;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentEntity toEntity(Appointment appointment) {
        AppointmentEntity entity = new AppointmentEntity();
        entity.setStatus(appointment.hasStatus() ? appointment.getStatus().toCode() : null);
        entity.setDescription(appointment.getDescription());
        entity.setStart(FhirMapping.toInstant(appointment.getStartElement()));
        entity.setEnd(FhirMapping.toInstant(appointment.getEndElement()));
        entity.setParticipants(appointment.getParticipant().stream()
                .map(AppointmentMapper::toParticipant)
                .toList());
        return entity;
    }

    public Appointment toResource(AppointmentEntity entity) {
        Appointment appointment = new Appointment();
        FhirMapping.setIdAndMeta(appointment, entity);
        appointment.setStatus(AppointmentStatus.fromCode(entity.getStatus()));
        appointment.setDescription(entity.getDescription());
        appointment.setStartElement(FhirMapping.instant(entity.getStart()));
        appointment.setEndElement(FhirMapping.instant(entity.getEnd()));
        for (AppointmentParticipant participant : entity.getParticipants()) {
            AppointmentParticipantComponent component = appointment.addParticipant()
                    .setActor(FhirMapping.reference(participant.getActorType(), participant.getActorId()));
            if (participant.getStatus() != null) {
                component.setStatus(ParticipationStatus.fromCode(participant.getStatus()));
            }
        }
        return appointment;
    }

    private static AppointmentParticipant toParticipant(AppointmentParticipantComponent component) {
        String status = component.hasStatus() ? component.getStatus().toCode() : null;
        return new AppointmentParticipant(FhirMapping.referenceString(component.getActor()), status);
    }
}
