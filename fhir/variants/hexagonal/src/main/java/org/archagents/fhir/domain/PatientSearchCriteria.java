package org.archagents.fhir.domain;

import java.util.List;

public record PatientSearchCriteria(
        List<String> familyPrefixes,
        List<String> givenPrefixes,
        List<TokenCriterion> identifiers,
        List<String> genders,
        List<DateCriterion> birthDates) {

    public PatientSearchCriteria {
        familyPrefixes = List.copyOf(familyPrefixes);
        givenPrefixes = List.copyOf(givenPrefixes);
        identifiers = List.copyOf(identifiers);
        genders = List.copyOf(genders);
        birthDates = List.copyOf(birthDates);
    }
}
