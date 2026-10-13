package org.archagents.fhir.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AppointmentParticipant {

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "status")
    private String status;

    protected AppointmentParticipant() {
    }

    public AppointmentParticipant(String actorType, Long actorId, String status) {
        this.actorType = actorType;
        this.actorId = actorId;
        this.status = status;
    }

    public String getActorType() {
        return actorType;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getStatus() {
        return status;
    }
}
