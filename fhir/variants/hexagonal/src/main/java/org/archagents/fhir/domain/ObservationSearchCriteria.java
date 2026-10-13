package org.archagents.fhir.domain;

import java.util.List;

public record ObservationSearchCriteria(List<Long> patientIds, List<TokenCriterion> codes, List<String> categories) {

    public ObservationSearchCriteria {
        patientIds = List.copyOf(patientIds);
        codes = List.copyOf(codes);
        categories = List.copyOf(categories);
    }
}
