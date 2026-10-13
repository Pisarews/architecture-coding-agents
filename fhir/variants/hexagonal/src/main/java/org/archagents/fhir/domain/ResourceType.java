package org.archagents.fhir.domain;

import java.util.Arrays;
import java.util.Optional;

public enum ResourceType {
    PATIENT("Patient"),
    PRACTITIONER("Practitioner"),
    ENCOUNTER("Encounter"),
    OBSERVATION("Observation"),
    APPOINTMENT("Appointment");

    private final String typeName;

    ResourceType(String typeName) {
        this.typeName = typeName;
    }

    public String typeName() {
        return typeName;
    }

    public static Optional<ResourceType> fromTypeName(String typeName) {
        return Arrays.stream(values()).filter(t -> t.typeName.equals(typeName)).findFirst();
    }
}
