package org.archagents.fhir.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "encounter")
class EncounterJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "encounter_id")
    @SequenceGenerator(name = "encounter_id", sequenceName = "resource_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private Instant lastUpdated;

    @Column(nullable = false)
    private String status;

    private String classSystem;

    private String classCode;

    @Column(nullable = false)
    private Long subjectId;

    private Long practitionerId;

    private Instant periodStart;

    private Instant periodEnd;

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

    String getClassSystem() {
        return classSystem;
    }

    void setClassSystem(String classSystem) {
        this.classSystem = classSystem;
    }

    String getClassCode() {
        return classCode;
    }

    void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    Long getSubjectId() {
        return subjectId;
    }

    void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    Long getPractitionerId() {
        return practitionerId;
    }

    void setPractitionerId(Long practitionerId) {
        this.practitionerId = practitionerId;
    }

    Instant getPeriodStart() {
        return periodStart;
    }

    void setPeriodStart(Instant periodStart) {
        this.periodStart = periodStart;
    }

    Instant getPeriodEnd() {
        return periodEnd;
    }

    void setPeriodEnd(Instant periodEnd) {
        this.periodEnd = periodEnd;
    }
}
