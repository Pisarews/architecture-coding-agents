package org.archagents.fhir.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "appointment")
public class AppointmentEntity extends ResourceEntity {

    public static final String STATUS_BOOKED = "booked";

    @Column(nullable = false)
    private String status;

    private String description;

    @Column(name = "start_time", nullable = false)
    private Instant start;

    @Column(name = "end_time", nullable = false)
    private Instant end;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "appointment_participant", joinColumns = @JoinColumn(name = "appointment_id"))
    @OrderColumn(name = "position")
    private List<AppointmentParticipant> participants = new ArrayList<>();

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStart() {
        return start;
    }

    public void setStart(Instant start) {
        this.start = start;
    }

    public Instant getEnd() {
        return end;
    }

    public void setEnd(Instant end) {
        this.end = end;
    }

    public List<AppointmentParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<AppointmentParticipant> participants) {
        this.participants = new ArrayList<>(participants);
    }
}
