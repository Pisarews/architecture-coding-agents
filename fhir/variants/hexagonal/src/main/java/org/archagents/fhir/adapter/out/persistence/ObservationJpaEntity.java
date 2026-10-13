package org.archagents.fhir.adapter.out.persistence;

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
class ObservationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "observation_id")
    @SequenceGenerator(name = "observation_id", sequenceName = "resource_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private Instant lastUpdated;

    @Column(nullable = false)
    private String status;

    private String categorySystem;

    private String categoryCode;

    private String codeSystem;

    @Column(nullable = false)
    private String codeCode;

    private String codeDisplay;

    @Column(nullable = false)
    private Long subjectId;

    private Long encounterId;

    private Instant effective;

    @Column(name = "value_value", precision = 19, scale = 6)
    private BigDecimal valueValue;

    private String valueUnit;

    private String valueSystem;

    private String valueCode;

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

    String getCategorySystem() {
        return categorySystem;
    }

    void setCategorySystem(String categorySystem) {
        this.categorySystem = categorySystem;
    }

    String getCategoryCode() {
        return categoryCode;
    }

    void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    String getCodeSystem() {
        return codeSystem;
    }

    void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

    String getCodeCode() {
        return codeCode;
    }

    void setCodeCode(String codeCode) {
        this.codeCode = codeCode;
    }

    String getCodeDisplay() {
        return codeDisplay;
    }

    void setCodeDisplay(String codeDisplay) {
        this.codeDisplay = codeDisplay;
    }

    Long getSubjectId() {
        return subjectId;
    }

    void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    Long getEncounterId() {
        return encounterId;
    }

    void setEncounterId(Long encounterId) {
        this.encounterId = encounterId;
    }

    Instant getEffective() {
        return effective;
    }

    void setEffective(Instant effective) {
        this.effective = effective;
    }

    BigDecimal getValueValue() {
        return valueValue;
    }

    void setValueValue(BigDecimal valueValue) {
        this.valueValue = valueValue;
    }

    String getValueUnit() {
        return valueUnit;
    }

    void setValueUnit(String valueUnit) {
        this.valueUnit = valueUnit;
    }

    String getValueSystem() {
        return valueSystem;
    }

    void setValueSystem(String valueSystem) {
        this.valueSystem = valueSystem;
    }

    String getValueCode() {
        return valueCode;
    }

    void setValueCode(String valueCode) {
        this.valueCode = valueCode;
    }
}
