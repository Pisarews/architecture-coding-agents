package org.archagents.fhir.web;

public class ResourceIdMismatchException extends RuntimeException {

    public ResourceIdMismatchException(String pathId, String bodyId) {
        super(bodyId == null
                ? "Resource id is required in the body of an update"
                : "Resource id '" + bodyId + "' does not match id '" + pathId + "' in the URL");
    }
}
