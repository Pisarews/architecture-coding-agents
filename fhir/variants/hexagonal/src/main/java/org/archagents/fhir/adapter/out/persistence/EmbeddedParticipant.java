package org.archagents.fhir.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.archagents.fhir.domain.AppointmentParticipant;
import org.archagents.fhir.domain.ParticipationStatus;
import org.archagents.fhir.domain.Reference;

@Embeddable
class EmbeddedParticipant {

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "status")
    private String status;

    protected EmbeddedParticipant() {
    }

    private EmbeddedParticipant(String actorType, Long actorId, String status) {
        this.actorType = actorType;
        this.actorId = actorId;
        this.status = status;
    }

    static EmbeddedParticipant from(AppointmentParticipant participant) {
        Reference actor = participant.actor();
        String status = participant.status() == null ? null : participant.status().code();
        return new EmbeddedParticipant(actor.type(), actor.idAsLong(), status);
    }

    AppointmentParticipant toDomain() {
        return new AppointmentParticipant(
                new Reference(actorType, actorId.toString()),
                status == null ? null : ParticipationStatus.fromCode(status));
    }
}
