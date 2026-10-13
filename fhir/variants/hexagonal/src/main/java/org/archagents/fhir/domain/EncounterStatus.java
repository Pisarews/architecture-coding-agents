package org.archagents.fhir.domain;

import java.util.Arrays;

public enum EncounterStatus {
    PLANNED("planned"),
    IN_PROGRESS("in-progress"),
    FINISHED("finished"),
    CANCELLED("cancelled");

    private final String code;

    EncounterStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static EncounterStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown EncounterStatus code: " + code));
    }
}
