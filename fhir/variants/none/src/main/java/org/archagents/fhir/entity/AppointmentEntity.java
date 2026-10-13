package org.archagents.fhir.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "appointment")
public class AppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_id")
    @SequenceGenerator(name = "resource_id", sequenceName = "resource_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private int version;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Column(nullable = false)
    private String status;

    private String description;

    @Column(name = "start_time", nullable = false)
    private Instant start;

    @Column(name = "end_time", nullable = false)
    private Instant end;

    @ElementCollection
    @CollectionTable(name = "appointment_participant", joinColumns = @JoinColumn(name = "appointment_id"))
    @OrderColumn(name = "position")
    private List<AppointmentParticipant> participants = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

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
}
