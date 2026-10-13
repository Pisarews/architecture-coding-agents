package org.archagents.fhir.adapter.in.web;

import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Answers requests that no resource controller handles: unknown types and unsupported methods. */
@RestController
class UnsupportedInteractionController {

    @RequestMapping({"/fhir/{type}", "/fhir/{type}/{id}"})
    void unsupported(@PathVariable String type) {
        if (SupportedResource.byTypeName(type).isPresent()) {
            throw new FhirRequestException(HttpStatus.METHOD_NOT_ALLOWED, IssueType.NOTSUPPORTED,
                    "Interaction is not supported for " + type);
        }
        throw new FhirRequestException(HttpStatus.NOT_FOUND, IssueType.NOTSUPPORTED,
                "Resource type " + type + " is not supported");
    }
}
