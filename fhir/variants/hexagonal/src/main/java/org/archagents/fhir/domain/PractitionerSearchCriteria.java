package org.archagents.fhir.domain;

import java.util.List;

public record PractitionerSearchCriteria(List<String> familyPrefixes, List<TokenCriterion> identifiers) {

    public PractitionerSearchCriteria {
        familyPrefixes = List.copyOf(familyPrefixes);
        identifiers = List.copyOf(identifiers);
    }
}
