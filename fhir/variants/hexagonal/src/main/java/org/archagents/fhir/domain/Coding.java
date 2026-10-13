package org.archagents.fhir.domain;

public record Coding(String system, String code, String display) {

    public boolean hasCode() {
        return code != null && !code.isBlank();
    }
}
