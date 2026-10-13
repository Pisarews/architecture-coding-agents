package org.archagents.fhir.domain;

/**
 * Matches a coded value or identifier. A {@code null} system matches any system, an empty system matches
 * only values without a system.
 */
public record TokenCriterion(String system, String code) {

    public boolean anySystem() {
        return system == null;
    }

    public boolean requiresAbsentSystem() {
        return system != null && system.isEmpty();
    }

    public static TokenCriterion anySystem(String code) {
        return new TokenCriterion(null, code);
    }
}
