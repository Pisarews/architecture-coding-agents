package org.archagents.fhir.domain;

import java.util.Arrays;

public enum ParticipationStatus {
    ACCEPTED("accepted"),
    DECLINED("declined"),
    TENTATIVE("tentative"),
    NEEDS_ACTION("needs-action");

    private final String code;

    ParticipationStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ParticipationStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ParticipationStatus code: " + code));
    }
}
