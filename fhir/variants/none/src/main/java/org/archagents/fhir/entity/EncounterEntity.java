package org.archagents.fhir.entity;

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
public class EncounterEntity {

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

    @Column(name = "class_system")
    private String classSystem;

    @Column(name = "class_code")
    private String classCode;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "practitioner_id")
    private Long practitionerId;

    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

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

    public String getClassSystem() {
        return classSystem;
    }

    public void setClassSystem(String classSystem) {
        this.classSystem = classSystem;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getPractitionerId() {
        return practitionerId;
    }

    public void setPractitionerId(Long practitionerId) {
        this.practitionerId = practitionerId;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Instant periodStart) {
        this.periodStart = periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Instant periodEnd) {
        this.periodEnd = periodEnd;
    }
}
