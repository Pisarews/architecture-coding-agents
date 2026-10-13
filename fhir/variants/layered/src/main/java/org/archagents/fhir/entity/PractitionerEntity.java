package org.archagents.fhir.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "practitioner")
public class PractitionerEntity extends ResourceEntity {

    private Boolean active;

    private String family;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "practitioner_given", joinColumns = @JoinColumn(name = "practitioner_id"))
    @OrderColumn(name = "position")
    @Column(name = "given", nullable = false)
    private List<String> given = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "practitioner_identifier", joinColumns = @JoinColumn(name = "practitioner_id"))
    @OrderColumn(name = "position")
    private List<IdentifierValue> identifiers = new ArrayList<>();

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
}
