package org.archagents.fhir.web;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementKind;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r4.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetadataController {

    private record SearchParam(String name, SearchParamType type) {
    }

    private static final Map<String, List<SearchParam>> SEARCH_PARAMS = new LinkedHashMap<>();

    static {
        SEARCH_PARAMS.put("Patient", List.of(
                new SearchParam("family", SearchParamType.STRING),
                new SearchParam("given", SearchParamType.STRING),
                new SearchParam("identifier", SearchParamType.TOKEN),
                new SearchParam("gender", SearchParamType.TOKEN),
                new SearchParam("birthdate", SearchParamType.DATE)));
        SEARCH_PARAMS.put("Practitioner", List.of(
                new SearchParam("family", SearchParamType.STRING),
                new SearchParam("identifier", SearchParamType.TOKEN)));
        SEARCH_PARAMS.put("Encounter", List.of(
                new SearchParam("patient", SearchParamType.REFERENCE),
                new SearchParam("status", SearchParamType.TOKEN)));
        SEARCH_PARAMS.put("Observation", List.of(
                new SearchParam("patient", SearchParamType.REFERENCE),
                new SearchParam("code", SearchParamType.TOKEN),
                new SearchParam("category", SearchParamType.TOKEN)));
        SEARCH_PARAMS.put("Appointment", List.of(
                new SearchParam("patient", SearchParamType.REFERENCE),
                new SearchParam("practitioner", SearchParamType.REFERENCE),
                new SearchParam("status", SearchParamType.TOKEN),
                new SearchParam("date", SearchParamType.DATE)));
    }

    private final Date started = new Date();

    static boolean isSupportedResourceType(String resourceType) {
        return SEARCH_PARAMS.containsKey(resourceType);
    }

    @GetMapping("/fhir/metadata")
    public ResponseEntity<CapabilityStatement> metadata() {
        CapabilityStatement statement = new CapabilityStatement();
        statement.setStatus(PublicationStatus.ACTIVE);
        statement.setDate(started);
        statement.setKind(CapabilityStatementKind.INSTANCE);
        statement.setFhirVersion(FHIRVersion._4_0_1);
        statement.addFormat("json");

        CapabilityStatementRestComponent rest = statement.addRest().setMode(RestfulCapabilityMode.SERVER);
        SEARCH_PARAMS.forEach((type, params) -> {
            CapabilityStatementRestResourceComponent resource = rest.addResource().setType(type);
            for (TypeRestfulInteraction interaction : List.of(TypeRestfulInteraction.READ, TypeRestfulInteraction.CREATE,
                    TypeRestfulInteraction.UPDATE, TypeRestfulInteraction.SEARCHTYPE)) {
                resource.addInteraction().setCode(interaction);
            }
            params.forEach(p -> resource.addSearchParam().setName(p.name()).setType(p.type()));
        });
        return ResponseEntity.ok().contentType(FhirJsonMessageConverter.FHIR_JSON).body(statement);
    }
}
