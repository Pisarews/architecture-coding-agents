package org.archagents.fhir.service;

public class ResourceValidationException extends RuntimeException {

    public enum Violation {
        REQUIRED_ELEMENT_MISSING,
        REFERENCE_NOT_FOUND,
        BUSINESS_RULE
    }

    private final Violation violation;

    public ResourceValidationException(Violation violation, String message) {
        super(message);
        this.violation = violation;
    }

    public static ResourceValidationException missing(String message) {
        return new ResourceValidationException(Violation.REQUIRED_ELEMENT_MISSING, message);
    }

    public static ResourceValidationException referenceNotFound(String reference) {
        return new ResourceValidationException(Violation.REFERENCE_NOT_FOUND, "Reference '" + reference + "' does not point to an existing resource of the expected type");
    }

    public static ResourceValidationException businessRule(String message) {
        return new ResourceValidationException(Violation.BUSINESS_RULE, message);
    }

    public Violation getViolation() {
        return violation;
    }
}
