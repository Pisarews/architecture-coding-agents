package org.archagents.fhir.util;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
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
public class FhirExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FhirExceptionHandler.class);

    @ExceptionHandler(FhirException.class)
    public ResponseEntity<String> handleFhirException(FhirException e) {
        return outcome(e.getStatus(), e.getIssueType(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleUnreadableBody(HttpMessageNotReadableException e) {
        return outcome(HttpStatus.BAD_REQUEST, IssueType.STRUCTURE, "Request body is missing or unreadable");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return outcome(HttpStatus.METHOD_NOT_ALLOWED, IssueType.NOTSUPPORTED, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResource(NoResourceFoundException e) {
        return outcome(HttpStatus.NOT_FOUND, IssueType.NOTSUPPORTED, "Unsupported resource type or interaction: /" + e.getResourcePath());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return outcome(HttpStatus.INTERNAL_SERVER_ERROR, IssueType.EXCEPTION, "Internal server error");
    }

    private static ResponseEntity<String> outcome(HttpStatus status, IssueType type, String message) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(IssueSeverity.ERROR).setCode(type).setDiagnostics(message);
        return FhirUtil.response(status, outcome);
    }
}
