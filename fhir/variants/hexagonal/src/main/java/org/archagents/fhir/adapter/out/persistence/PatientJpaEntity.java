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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patient")
class PatientJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "patient_id")
    @SequenceGenerator(name = "patient_id", sequenceName = "resource_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private Instant lastUpdated;

    private Boolean active;

    private String family;

    private String gender;

    private LocalDate birthDate;

    @ElementCollection
    @CollectionTable(name = "patient_given", joinColumns = @JoinColumn(name = "patient_id"))
    @OrderColumn(name = "position")
    @Column(name = "given", nullable = false)
    private List<String> given = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "patient_identifier", joinColumns = @JoinColumn(name = "patient_id"))
    @OrderColumn(name = "position")
    private List<EmbeddedIdentifier> identifiers = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "patient_telecom", joinColumns = @JoinColumn(name = "patient_id"))
    @OrderColumn(name = "position")
    private List<EmbeddedTelecom> telecoms = new ArrayList<>();

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

    Boolean getActive() {
        return active;
    }

    void setActive(Boolean active) {
        this.active = active;
    }

    String getFamily() {
        return family;
    }

    void setFamily(String family) {
        this.family = family;
    }

    String getGender() {
        return gender;
    }

    void setGender(String gender) {
        this.gender = gender;
    }

    LocalDate getBirthDate() {
        return birthDate;
    }

    void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    List<String> getGiven() {
        return given;
    }

    List<EmbeddedIdentifier> getIdentifiers() {
        return identifiers;
    }

    List<EmbeddedTelecom> getTelecoms() {
        return telecoms;
    }
}
