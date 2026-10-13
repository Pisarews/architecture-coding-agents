package org.archagents.fhir.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "observation")
public class ObservationEntity {

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

    @Column(name = "category_system")
    private String categorySystem;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "code_system")
    private String codeSystem;

    @Column(name = "code_code", nullable = false)
    private String codeCode;

    @Column(name = "code_display")
    private String codeDisplay;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "encounter_id")
    private Long encounterId;

    private Instant effective;

    @Column(name = "value_value", precision = 19, scale = 6)
    private BigDecimal valueValue;

    @Column(name = "value_unit")
    private String valueUnit;

    @Column(name = "value_system")
    private String valueSystem;

    @Column(name = "value_code")
    private String valueCode;

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

    public String getCodeCode() {
        return codeCode;
    }

    public void setCodeCode(String codeCode) {
        this.codeCode = codeCode;
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

    public Instant getEffective() {
        return effective;
    }

    public void setEffective(Instant effective) {
        this.effective = effective;
    }

    public BigDecimal getValueValue() {
        return valueValue;
    }

    public void setValueValue(BigDecimal valueValue) {
        this.valueValue = valueValue;
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
