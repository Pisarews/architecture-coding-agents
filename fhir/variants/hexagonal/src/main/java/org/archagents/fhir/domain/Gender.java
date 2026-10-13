package org.archagents.fhir.domain;

import java.util.Arrays;

public enum Gender {
    MALE("male"),
    FEMALE("female"),
    OTHER("other"),
    UNKNOWN("unknown");

    private final String code;

    Gender(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Gender fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Gender code: " + code));
    }
}
