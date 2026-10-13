package org.archagents.fhir.entity;

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
@Table(name = "practitioner")
public class PractitionerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_id")
    @SequenceGenerator(name = "resource_id", sequenceName = "resource_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private int version;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    private Boolean active;

    private String family;

    @ElementCollection
    @CollectionTable(name = "practitioner_given", joinColumns = @JoinColumn(name = "practitioner_id"))
    @OrderColumn(name = "position")
    @Column(name = "given", nullable = false)
    private List<String> given = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "practitioner_identifier", joinColumns = @JoinColumn(name = "practitioner_id"))
    @OrderColumn(name = "position")
    private List<IdentifierValue> identifiers = new ArrayList<>();

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

    public List<IdentifierValue> getIdentifiers() {
        return identifiers;
    }
}
