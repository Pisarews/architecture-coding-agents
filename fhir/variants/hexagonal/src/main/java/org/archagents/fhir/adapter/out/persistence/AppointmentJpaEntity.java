package org.archagents.fhir.adapter.out.persistence;

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
class AppointmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "appointment_id")
    @SequenceGenerator(name = "appointment_id", sequenceName = "resource_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private Instant lastUpdated;

    @Column(nullable = false)
    private String status;

    private String description;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @ElementCollection
    @CollectionTable(name = "appointment_participant", joinColumns = @JoinColumn(name = "appointment_id"))
    @OrderColumn(name = "position")
    private List<EmbeddedParticipant> participants = new ArrayList<>();

    Long getId() {
        return id;
    }

    int getVersion() {
        return version;
    }

    void setVersion(int version) {
        this.version = version;
    }

    Instant getLastUpdated() {
        return lastUpdated;
    }

    void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    Instant getStartTime() {
        return startTime;
    }

    void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    Instant getEndTime() {
        return endTime;
    }

    void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    List<EmbeddedParticipant> getParticipants() {
        return participants;
    }
}
