package org.archagents.fhir.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

@Embeddable
public class AppointmentParticipant {

    public static final String PATIENT = "Patient";
    public static final String PRACTITIONER = "Practitioner";

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "status")
    private String status;

    @Transient
    private String actorReference;

    protected AppointmentParticipant() {
    }

    public AppointmentParticipant(String actorReference, String status) {
        this.actorReference = actorReference;
        this.status = status;
    }

    public void resolveActor(String actorType, Long actorId) {
        this.actorType = actorType;
        this.actorId = actorId;
    }

    public String getActorReference() {
        return actorReference;
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
