package org.archagents.fhir.adapter.in.web;

import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.springframework.http.HttpStatus;

class FhirRequestException extends RuntimeException {

    private final HttpStatus status;
    private final IssueType issueType;

    FhirRequestException(HttpStatus status, IssueType issueType, String message) {
        super(message);
        this.status = status;
        this.issueType = issueType;
    }

    static FhirRequestException structure(String message) {
        return new FhirRequestException(HttpStatus.BAD_REQUEST, IssueType.STRUCTURE, message);
    }

    static FhirRequestException invalid(String message) {
        return new FhirRequestException(HttpStatus.BAD_REQUEST, IssueType.INVALID, message);
    }

    static FhirRequestException notFound(String message) {
        return new FhirRequestException(HttpStatus.NOT_FOUND, IssueType.NOTFOUND, message);
    }

    HttpStatus status() {
        return status;
    }

    IssueType issueType() {
        return issueType;
    }
}
