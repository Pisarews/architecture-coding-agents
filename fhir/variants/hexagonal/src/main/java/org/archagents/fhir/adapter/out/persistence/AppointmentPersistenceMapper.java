package org.archagents.fhir.adapter.out.persistence;

import org.archagents.fhir.domain.Appointment;
import org.archagents.fhir.domain.AppointmentStatus;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.stereotype.Component;

@Component
class AppointmentPersistenceMapper {

    StoredResource<Appointment> toDomain(AppointmentJpaEntity entity) {
        Appointment appointment = new Appointment(
                AppointmentStatus.fromCode(entity.getStatus()),
                entity.getDescription(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getParticipants().stream().map(EmbeddedParticipant::toDomain).toList());
        return new StoredResource<>(
                entity.getId(), new ResourceMeta(entity.getVersion(), entity.getLastUpdated()), appointment);
    }

    void copyToEntity(Appointment appointment, ResourceMeta meta, AppointmentJpaEntity entity) {
        entity.setVersion(meta.versionId());
        entity.setLastUpdated(meta.lastUpdated());
        entity.setStatus(appointment.status().code());
        entity.setDescription(appointment.description());
        entity.setStartTime(appointment.start());
        entity.setEndTime(appointment.end());
        entity.getParticipants().clear();
        appointment.participants()
                .forEach(participant -> entity.getParticipants().add(EmbeddedParticipant.from(participant)));
    }
}
