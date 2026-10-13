package org.archagents.fhir.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Telecom {

    @Column(name = "telecom_system")
    private String system;

    @Column(name = "telecom_value")
    private String value;

    protected Telecom() {
    }

    public Telecom(String system, String value) {
        this.system = system;
        this.value = value;
    }

    public String getSystem() {
        return system;
    }

    public String getValue() {
        return value;
    }
}
