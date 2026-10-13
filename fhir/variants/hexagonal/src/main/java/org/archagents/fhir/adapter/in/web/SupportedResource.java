package org.archagents.fhir.adapter.in.web;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

enum SupportedResource {
    PATIENT("Patient", Map.of(
            "family", SearchParamType.STRING,
            "given", SearchParamType.STRING,
            "identifier", SearchParamType.TOKEN,
            "gender", SearchParamType.TOKEN,
            "birthdate", SearchParamType.DATE)),
    PRACTITIONER("Practitioner", Map.of(
            "family", SearchParamType.STRING,
            "identifier", SearchParamType.TOKEN)),
    ENCOUNTER("Encounter", Map.of(
            "patient", SearchParamType.REFERENCE,
            "status", SearchParamType.TOKEN)),
    OBSERVATION("Observation", Map.of(
            "patient", SearchParamType.REFERENCE,
            "code", SearchParamType.TOKEN,
            "category", SearchParamType.TOKEN)),
    APPOINTMENT("Appointment", Map.of(
            "patient", SearchParamType.REFERENCE,
            "practitioner", SearchParamType.REFERENCE,
            "status", SearchParamType.TOKEN,
            "date", SearchParamType.DATE));

    private final String typeName;
    private final Map<String, SearchParamType> searchParameters;

    SupportedResource(String typeName, Map<String, SearchParamType> searchParameters) {
        this.typeName = typeName;
        this.searchParameters = new LinkedHashMap<>(searchParameters);
    }

    String typeName() {
        return typeName;
    }

    Map<String, SearchParamType> searchParameters() {
        return searchParameters;
    }

    static Optional<SupportedResource> byTypeName(String typeName) {
        return Arrays.stream(values()).filter(r -> r.typeName.equals(typeName)).findFirst();
    }
}
