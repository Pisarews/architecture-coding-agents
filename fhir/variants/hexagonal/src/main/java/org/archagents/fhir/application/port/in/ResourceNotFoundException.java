package org.archagents.fhir.application.port.in;

import org.archagents.fhir.domain.ResourceType;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(ResourceType type, long id) {
        super(type.typeName() + "/" + id + " does not exist");
    }
}
