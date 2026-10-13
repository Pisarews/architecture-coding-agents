package org.archagents.fhir.domain;

public class RuleViolationException extends RuntimeException {

    public enum Kind {
        REQUIRED,
        REFERENCE_NOT_FOUND,
        BUSINESS_RULE
    }

    private final Kind kind;

    public RuleViolationException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public static RuleViolationException required(String message) {
        return new RuleViolationException(Kind.REQUIRED, message);
    }

    public static RuleViolationException referenceNotFound(String message) {
        return new RuleViolationException(Kind.REFERENCE_NOT_FOUND, message);
    }

    public static RuleViolationException businessRule(String message) {
        return new RuleViolationException(Kind.BUSINESS_RULE, message);
    }

    public Kind kind() {
        return kind;
    }
}
