package org.archagents.fhir.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import java.time.Instant;

@MappedSuperclass
public abstract class ResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_id_seq")
    @SequenceGenerator(name = "resource_id_seq", sequenceName = "resource_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private int version;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    public void assignFirstVersion(Instant now) {
        this.id = null;
        this.version = 1;
        this.lastUpdated = now;
    }

    public void assignNextVersion(ResourceEntity current, Instant now) {
        this.id = current.getId();
        this.version = current.getVersion() + 1;
        this.lastUpdated = now;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
