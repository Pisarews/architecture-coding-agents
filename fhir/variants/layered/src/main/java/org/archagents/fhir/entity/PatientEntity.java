package org.archagents.fhir.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patient")
public class PatientEntity extends ResourceEntity {

    private Boolean active;

    private String family;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "patient_given", joinColumns = @JoinColumn(name = "patient_id"))
    @OrderColumn(name = "position")
    @Column(name = "given", nullable = false)
    private List<String> given = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "patient_identifier", joinColumns = @JoinColumn(name = "patient_id"))
    @OrderColumn(name = "position")
    private List<IdentifierValue> identifiers = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "patient_telecom", joinColumns = @JoinColumn(name = "patient_id"))
    @OrderColumn(name = "position")
    private List<TelecomValue> telecoms = new ArrayList<>();

    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public List<String> getGiven() {
        return given;
    }

    public void setGiven(List<String> given) {
        this.given = new ArrayList<>(given);
    }

    public List<IdentifierValue> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<IdentifierValue> identifiers) {
        this.identifiers = new ArrayList<>(identifiers);
    }

    public List<TelecomValue> getTelecoms() {
        return telecoms;
    }

    public void setTelecoms(List<TelecomValue> telecoms) {
        this.telecoms = new ArrayList<>(telecoms);
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}
