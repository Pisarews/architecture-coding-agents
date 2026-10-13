package org.archagents.fhir.web;

import org.archagents.fhir.service.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Resource;

final class ResourceIds {

    private ResourceIds() {
    }

    static long parse(String resourceType, String id) {
        if (!id.matches("\\d{1,18}")) {
            throw new ResourceNotFoundException(resourceType, id);
        }
        return Long.parseLong(id);
    }

    /**
     * The id in an update body must be present and equal to the id in the URL.
     */
    static long requireMatching(String resourceType, String pathId, Resource body) {
        String bodyId = body.getIdElement().getIdPart();
        if (bodyId == null || !bodyId.equals(pathId)) {
            throw new ResourceIdMismatchException(pathId, bodyId);
        }
        return parse(resourceType, pathId);
    }
}
