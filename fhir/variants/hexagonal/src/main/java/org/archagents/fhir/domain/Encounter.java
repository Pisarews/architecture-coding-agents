package org.archagents.fhir.domain;

public record Encounter(
        EncounterStatus status,
        Coding encounterClass,
        Reference subject,
        Reference practitioner,
        Period period) {

    public void checkRequiredElements() {
        if (status == null) {
            throw RuleViolationException.required("Encounter.status is required");
        }
        if (subject == null) {
            throw RuleViolationException.required("Encounter.subject is required");
        }
    }

    public void checkPeriod() {
        if (period != null && period.endsBeforeStart()) {
            throw RuleViolationException.businessRule("Encounter.period.end must not be before period.start");
        }
    }
}
