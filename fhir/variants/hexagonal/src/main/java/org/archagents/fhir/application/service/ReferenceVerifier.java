package org.archagents.fhir.application.service;

import java.util.Arrays;
import org.archagents.fhir.application.port.out.ResourceExistencePort;
import org.archagents.fhir.domain.Reference;
import org.archagents.fhir.domain.ResourceType;
import org.archagents.fhir.domain.RuleViolationException;

public class ReferenceVerifier {

    private final ResourceExistencePort resourceExistencePort;

    public ReferenceVerifier(ResourceExistencePort resourceExistencePort) {
        this.resourceExistencePort = resourceExistencePort;
    }

    public void verify(Reference reference, String element, ResourceType... allowedTypes) {
        boolean resolvable = Arrays.stream(allowedTypes)
                .filter(reference::pointsTo)
                .anyMatch(type -> resourceExistencePort.exists(type, reference.idAsLong()));
        if (!resolvable) {
            throw RuleViolationException.referenceNotFound(
                    element + " refers to " + reference + ", which does not exist");
        }
    }
}
