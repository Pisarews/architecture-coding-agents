package org.archagents.fhir.adapter.in.web;

import java.net.URI;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
class FhirResponses {

    static final MediaType FHIR_JSON = MediaType.parseMediaType("application/fhir+json;charset=UTF-8");

    private final FhirJsonCodec codec;

    FhirResponses(FhirJsonCodec codec) {
        this.codec = codec;
    }

    ResponseEntity<String> ok(IBaseResource resource) {
        return body(ResponseEntity.ok(), resource);
    }

    ResponseEntity<String> read(Resource resource) {
        return body(ResponseEntity.ok().eTag(eTag(resource)), resource);
    }

    ResponseEntity<String> created(Resource resource) {
        URI location = URI.create(resourceUrl(resource) + "/_history/" + resource.getMeta().getVersionId());
        return body(ResponseEntity.created(location).eTag(eTag(resource)), resource);
    }

    ResponseEntity<String> searchSet(List<? extends Resource> resources) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(resources.size());
        for (Resource resource : resources) {
            bundle.addEntry()
                    .setFullUrl(resourceUrl(resource))
                    .setResource(resource)
                    .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
        }
        return ok(bundle);
    }

    ResponseEntity<String> outcome(HttpStatus status, IssueType issueType, String message) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(IssueSeverity.ERROR).setCode(issueType).setDiagnostics(message);
        return body(ResponseEntity.status(status), outcome);
    }

    private ResponseEntity<String> body(ResponseEntity.BodyBuilder builder, IBaseResource resource) {
        return builder.contentType(FHIR_JSON).body(codec.encode(resource));
    }

    private static String eTag(Resource resource) {
        return "W/\"" + resource.getMeta().getVersionId() + "\"";
    }

    private static String resourceUrl(Resource resource) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .pathSegment("fhir", resource.fhirType(), resource.getIdPart())
                .toUriString();
    }
}
