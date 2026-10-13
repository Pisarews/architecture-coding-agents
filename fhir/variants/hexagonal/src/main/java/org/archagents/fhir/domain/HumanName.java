package org.archagents.fhir.domain;

import java.util.List;

public record HumanName(String family, List<String> given) {

    public HumanName {
        given = List.copyOf(given);
    }

    public boolean hasFamily() {
        return family != null && !family.isBlank();
    }
}
