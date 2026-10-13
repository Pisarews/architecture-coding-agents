package org.archagents.fhir.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class IdentifierValue {

    @Column(name = "ident_system")
    private String system;

    @Column(name = "ident_value")
    private String value;

    protected IdentifierValue() {
    }

    public IdentifierValue(String system, String value) {
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
