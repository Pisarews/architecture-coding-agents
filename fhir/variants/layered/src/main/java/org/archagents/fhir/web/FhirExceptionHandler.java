package org.archagents.fhir.web;

import org.archagents.fhir.service.InvalidSearchParameterException;
import org.archagents.fhir.service.ResourceNotFoundException;
import org.archagents.fhir.service.ResourceValidationException;
import org.archagents.fhir.service.UnsupportedSearchParameterException;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into FHIR {@link OperationOutcome} responses.
 */
@RestControllerAdvice
public class FhirExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FhirExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<OperationOutcome> unreadableBody(HttpMessageNotReadableException e) {
        return outcome(HttpStatus.BAD_REQUEST, IssueType.STRUCTURE, e.getMessage());
    }

    @ExceptionHandler(ResourceIdMismatchException.class)
    public ResponseEntity<OperationOutcome> idMismatch(ResourceIdMismatchException e) {
        return outcome(HttpStatus.BAD_REQUEST, IssueType.INVALID, e.getMessage());
    }

    @ExceptionHandler(UnsupportedSearchParameterException.class)
    public ResponseEntity<OperationOutcome> unsupportedSearchParameter(UnsupportedSearchParameterException e) {
        return outcome(HttpStatus.BAD_REQUEST, IssueType.NOTSUPPORTED, e.getMessage());
    }

    @ExceptionHandler(InvalidSearchParameterException.class)
    public ResponseEntity<OperationOutcome> invalidSearchParameter(InvalidSearchParameterException e) {
        return outcome(HttpStatus.BAD_REQUEST, IssueType.INVALID, e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<OperationOutcome> notFound(ResourceNotFoundException e) {
        return outcome(HttpStatus.NOT_FOUND, IssueType.NOTFOUND, e.getMessage());
    }

    @ExceptionHandler(UnsupportedResourceTypeException.class)
    public ResponseEntity<OperationOutcome> unsupportedResourceType(UnsupportedResourceTypeException e) {
        return outcome(HttpStatus.NOT_FOUND, IssueType.NOTSUPPORTED, e.getMessage());
    }

    @ExceptionHandler(ResourceValidationException.class)
    public ResponseEntity<OperationOutcome> validationFailed(ResourceValidationException e) {
        IssueType type = switch (e.getViolation()) {
            case REQUIRED_ELEMENT_MISSING -> IssueType.REQUIRED;
            case REFERENCE_NOT_FOUND -> IssueType.NOTFOUND;
            case BUSINESS_RULE -> IssueType.BUSINESSRULE;
        };
        return outcome(HttpStatus.UNPROCESSABLE_ENTITY, type, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OperationOutcome> unexpected(Exception e) {
        if (e instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            return outcome(errorResponse.getStatusCode(), IssueType.NOTSUPPORTED, e.getMessage());
        }
        log.error("Unhandled exception", e);
        return outcome(HttpStatus.INTERNAL_SERVER_ERROR, IssueType.EXCEPTION, "Internal server error");
    }

    private static ResponseEntity<OperationOutcome> outcome(HttpStatusCode status, IssueType type, String diagnostics) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(IssueSeverity.ERROR)
                .setCode(type)
                .setDiagnostics(diagnostics);
        return ResponseEntity.status(status).contentType(FhirJsonMessageConverter.FHIR_JSON).body(outcome);
    }
}
