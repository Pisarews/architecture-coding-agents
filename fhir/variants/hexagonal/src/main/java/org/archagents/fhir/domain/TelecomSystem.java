package org.archagents.fhir.domain;

import java.util.Arrays;

public enum TelecomSystem {
    PHONE("phone"),
    EMAIL("email");

    private final String code;

    TelecomSystem(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static TelecomSystem fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown TelecomSystem code: " + code));
    }
}
