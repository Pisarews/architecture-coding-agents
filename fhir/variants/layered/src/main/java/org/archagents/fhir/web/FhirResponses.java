package org.archagents.fhir.web;

import java.net.URI;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Builds the HTTP responses shared by all resource endpoints.
 */
final class FhirResponses {

    private FhirResponses() {
    }

    static <T extends Resource> ResponseEntity<T> ok(T resource) {
        return ResponseEntity.ok()
                .contentType(FhirJsonMessageConverter.FHIR_JSON)
                .eTag(etag(resource))
                .body(resource);
    }

    static <T extends Resource> ResponseEntity<T> created(T resource) {
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/fhir/{type}/{id}/_history/{version}")
                .buildAndExpand(resource.fhirType(), resource.getIdElement().getIdPart(), resource.getMeta().getVersionId())
                .toUri();
        return ResponseEntity.created(location)
                .contentType(FhirJsonMessageConverter.FHIR_JSON)
                .eTag(etag(resource))
                .body(resource);
    }

    static ResponseEntity<Bundle> searchSet(List<? extends Resource> resources) {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().path("/fhir").toUriString();
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(resources.size());
        for (Resource resource : resources) {
            bundle.addEntry()
                    .setFullUrl(base + "/" + resource.fhirType() + "/" + resource.getIdElement().getIdPart())
                    .setResource(resource)
                    .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
        }
        return ResponseEntity.ok().contentType(FhirJsonMessageConverter.FHIR_JSON).body(bundle);
    }

    private static String etag(Resource resource) {
        return "W/\"" + resource.getMeta().getVersionId() + "\"";
    }
}
