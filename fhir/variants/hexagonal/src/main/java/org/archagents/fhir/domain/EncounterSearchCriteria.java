package org.archagents.fhir.domain;

import java.util.List;

public record EncounterSearchCriteria(List<Long> patientIds, List<String> statuses) {

    public EncounterSearchCriteria {
        patientIds = List.copyOf(patientIds);
        statuses = List.copyOf(statuses);
    }
}
