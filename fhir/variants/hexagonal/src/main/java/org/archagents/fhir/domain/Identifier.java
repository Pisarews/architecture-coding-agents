package org.archagents.fhir.domain;

public record Identifier(String system, String value) {

    public boolean hasValue() {
        return value != null && !value.isBlank();
    }
}
