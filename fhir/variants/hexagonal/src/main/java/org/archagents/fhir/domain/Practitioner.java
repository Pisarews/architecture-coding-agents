package org.archagents.fhir.domain;

import java.util.List;

public record Practitioner(Boolean active, HumanName name, List<Identifier> identifiers) {

    public Practitioner {
        identifiers = List.copyOf(identifiers);
    }

    public void validate() {
        if (name == null || !name.hasFamily()) {
            throw RuleViolationException.required("Practitioner.name.family is required");
        }
    }
}
