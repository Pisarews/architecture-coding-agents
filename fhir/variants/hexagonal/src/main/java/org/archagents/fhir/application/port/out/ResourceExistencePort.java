package org.archagents.fhir.application.port.out;

import org.archagents.fhir.domain.ResourceType;

public interface ResourceExistencePort {

    boolean exists(ResourceType type, long id);
}
