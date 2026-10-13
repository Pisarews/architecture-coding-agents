package org.archagents.fhir.domain;

import java.util.Arrays;

public enum ObservationStatus {
    REGISTERED("registered"),
    PRELIMINARY("preliminary"),
    FINAL("final"),
    AMENDED("amended"),
    CANCELLED("cancelled"),
    ENTERED_IN_ERROR("entered-in-error");

    private final String code;

    ObservationStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ObservationStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ObservationStatus code: " + code));
    }
}
