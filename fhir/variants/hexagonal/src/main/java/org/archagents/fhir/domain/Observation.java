package org.archagents.fhir.domain;

import java.time.Instant;

public record Observation(
        ObservationStatus status,
        Coding category,
        Coding code,
        Reference subject,
        Reference encounter,
        Instant effective,
        Quantity value) {

    public void checkRequiredElements() {
        if (status == null) {
            throw RuleViolationException.required("Observation.status is required");
        }
        if (code == null || !code.hasCode()) {
            throw RuleViolationException.required("Observation.code.coding.code is required");
        }
        if (subject == null) {
            throw RuleViolationException.required("Observation.subject is required");
        }
    }

    public void checkValue() {
        if (value != null && value.value() == null) {
            throw RuleViolationException.required("Observation.valueQuantity.value is required");
        }
    }
}
