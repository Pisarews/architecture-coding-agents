package org.archagents.fhir.util;

import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.springframework.http.HttpStatus;

public class FhirException extends RuntimeException {

    private final HttpStatus status;
    private final IssueType issueType;

    public FhirException(HttpStatus status, IssueType issueType, String message) {
        super(message);
        this.status = status;
        this.issueType = issueType;
    }

    public static FhirException structure(String message) {
        return new FhirException(HttpStatus.BAD_REQUEST, IssueType.STRUCTURE, message);
    }

    public static FhirException invalid(String message) {
        return new FhirException(HttpStatus.BAD_REQUEST, IssueType.INVALID, message);
    }

    public static FhirException unsupportedParameter(String name) {
        return new FhirException(HttpStatus.BAD_REQUEST, IssueType.NOTSUPPORTED, "Unsupported search parameter: " + name);
    }

    public static FhirException notFound(String type, String id) {
        return new FhirException(HttpStatus.NOT_FOUND, IssueType.NOTFOUND, type + "/" + id + " not found");
    }

    public static FhirException unsupportedResource(String message) {
        return new FhirException(HttpStatus.NOT_FOUND, IssueType.NOTSUPPORTED, message);
    }

    public static FhirException methodNotAllowed(String message) {
        return new FhirException(HttpStatus.METHOD_NOT_ALLOWED, IssueType.NOTSUPPORTED, message);
    }

    public static FhirException required(String message) {
        return new FhirException(HttpStatus.UNPROCESSABLE_ENTITY, IssueType.REQUIRED, message);
    }

    public static FhirException referenceNotFound(String reference) {
        return new FhirException(HttpStatus.UNPROCESSABLE_ENTITY, IssueType.NOTFOUND, "Referenced resource not found: " + reference);
    }

    public static FhirException businessRule(String message) {
        return new FhirException(HttpStatus.UNPROCESSABLE_ENTITY, IssueType.BUSINESSRULE, message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public IssueType getIssueType() {
        return issueType;
    }
}
