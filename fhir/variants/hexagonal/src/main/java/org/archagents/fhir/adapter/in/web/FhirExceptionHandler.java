package org.archagents.fhir.adapter.in.web;

import org.archagents.fhir.application.port.in.ResourceNotFoundException;
import org.archagents.fhir.domain.RuleViolationException;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
class FhirExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FhirExceptionHandler.class);

    private final FhirResponses responses;

    FhirExceptionHandler(FhirResponses responses) {
        this.responses = responses;
    }

    @ExceptionHandler
    ResponseEntity<String> handle(FhirRequestException e) {
        return responses.outcome(e.status(), e.issueType(), e.getMessage());
    }

    @ExceptionHandler
    ResponseEntity<String> handle(ResourceNotFoundException e) {
        return responses.outcome(HttpStatus.NOT_FOUND, IssueType.NOTFOUND, e.getMessage());
    }

    @ExceptionHandler
    ResponseEntity<String> handle(RuleViolationException e) {
        IssueType issueType = switch (e.kind()) {
            case REQUIRED -> IssueType.REQUIRED;
            case REFERENCE_NOT_FOUND -> IssueType.NOTFOUND;
            case BUSINESS_RULE -> IssueType.BUSINESSRULE;
        };
        return responses.outcome(HttpStatus.UNPROCESSABLE_ENTITY, issueType, e.getMessage());
    }

    @ExceptionHandler
    ResponseEntity<String> handle(HttpMessageNotReadableException e) {
        return responses.outcome(HttpStatus.BAD_REQUEST, IssueType.STRUCTURE, "Request body is missing or unreadable");
    }

    @ExceptionHandler
    ResponseEntity<String> handle(HttpRequestMethodNotSupportedException e) {
        return responses.outcome(HttpStatus.METHOD_NOT_ALLOWED, IssueType.NOTSUPPORTED, e.getMessage());
    }

    @ExceptionHandler
    ResponseEntity<String> handle(NoResourceFoundException e) {
        return responses.outcome(HttpStatus.NOT_FOUND, IssueType.NOTSUPPORTED, "Unsupported path /" + e.getResourcePath());
    }

    @ExceptionHandler
    ResponseEntity<String> handle(Exception e) {
        log.error("Unexpected error", e);
        return responses.outcome(HttpStatus.INTERNAL_SERVER_ERROR, IssueType.EXCEPTION, "Internal server error");
    }
}
