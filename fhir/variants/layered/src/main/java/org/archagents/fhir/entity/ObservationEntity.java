package org.archagents.fhir.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "observation")
public class ObservationEntity extends ResourceEntity {

    @Column(nullable = false)
    private String status;

    @Column(name = "category_system")
    private String categorySystem;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "code_system")
    private String codeSystem;

    @Column(name = "code_code", nullable = false)
    private String code;

    @Column(name = "code_display")
    private String codeDisplay;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Transient
    private String subjectReference;

    @Transient
    private String encounterReference;

    private Instant effective;

    @Column(name = "value_value", precision = 19, scale = 6)
    private BigDecimal valueQuantity;

    @Column(name = "value_unit")
    private String valueUnit;

    @Column(name = "value_system")
    private String valueSystem;

    @Column(name = "value_code")
    private String valueCode;

    public boolean hasValueQuantity() {
        return valueQuantity != null || valueUnit != null || valueSystem != null || valueCode != null;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategorySystem() {
        return categorySystem;
    }

    public void setCategorySystem(String categorySystem) {
        this.categorySystem = categorySystem;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCodeSystem() {
        return codeSystem;
    }

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeDisplay() {
        return codeDisplay;
    }

    public void setCodeDisplay(String codeDisplay) {
        this.codeDisplay = codeDisplay;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(Long encounterId) {
        this.encounterId = encounterId;
    }

    public String getSubjectReference() {
        return subjectReference;
    }

    public void setSubjectReference(String subjectReference) {
        this.subjectReference = subjectReference;
    }

    public String getEncounterReference() {
        return encounterReference;
    }

    public void setEncounterReference(String encounterReference) {
        this.encounterReference = encounterReference;
    }

    public Instant getEffective() {
        return effective;
    }

    public void setEffective(Instant effective) {
        this.effective = effective;
    }

    public BigDecimal getValueQuantity() {
        return valueQuantity;
    }

    public void setValueQuantity(BigDecimal valueQuantity) {
        this.valueQuantity = valueQuantity;
    }

    public String getValueUnit() {
        return valueUnit;
    }

    public void setValueUnit(String valueUnit) {
        this.valueUnit = valueUnit;
    }

    public String getValueSystem() {
        return valueSystem;
    }

    public void setValueSystem(String valueSystem) {
        this.valueSystem = valueSystem;
    }

    public String getValueCode() {
        return valueCode;
    }

    public void setValueCode(String valueCode) {
        this.valueCode = valueCode;
    }
}
