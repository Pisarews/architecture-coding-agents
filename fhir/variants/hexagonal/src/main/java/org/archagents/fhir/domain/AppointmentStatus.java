package org.archagents.fhir.domain;

import java.util.Arrays;

public enum AppointmentStatus {
    PROPOSED("proposed"),
    BOOKED("booked"),
    CANCELLED("cancelled"),
    FULFILLED("fulfilled"),
    NOSHOW("noshow");

    private final String code;

    AppointmentStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AppointmentStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AppointmentStatus code: " + code));
    }
}
