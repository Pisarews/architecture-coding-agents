package org.archagents.fhir.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;

@Entity
@Table(name = "encounter")
public class EncounterEntity extends ResourceEntity {

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

    @Transient
    private String subjectReference;

    @Transient
    private String practitionerReference;

    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

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

    public String getSubjectReference() {
        return subjectReference;
    }

    public void setSubjectReference(String subjectReference) {
        this.subjectReference = subjectReference;
    }

    public String getPractitionerReference() {
        return practitionerReference;
    }

    public void setPractitionerReference(String practitionerReference) {
        this.practitionerReference = practitionerReference;
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
