package org.archagents.fhir.adapter.in.web;

import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementKind;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r4.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CapabilityStatementController {

    private static final List<TypeRestfulInteraction> INTERACTIONS = List.of(
            TypeRestfulInteraction.READ,
            TypeRestfulInteraction.CREATE,
            TypeRestfulInteraction.UPDATE,
            TypeRestfulInteraction.SEARCHTYPE);

    private final FhirResponses responses;

    CapabilityStatementController(FhirResponses responses) {
        this.responses = responses;
    }

    @GetMapping("/fhir/metadata")
    ResponseEntity<String> metadata() {
        CapabilityStatement statement = new CapabilityStatement();
        statement.setStatus(PublicationStatus.ACTIVE);
        statement.setDate(new Date());
        statement.setKind(CapabilityStatementKind.INSTANCE);
        statement.setFhirVersion(FHIRVersion._4_0_1);
        statement.addFormat("json");
        CapabilityStatementRestComponent rest = statement.addRest().setMode(RestfulCapabilityMode.SERVER);
        for (SupportedResource supported : SupportedResource.values()) {
            CapabilityStatementRestResourceComponent resource = rest.addResource().setType(supported.typeName());
            INTERACTIONS.forEach(interaction -> resource.addInteraction().setCode(interaction));
            supported.searchParameters().forEach((name, type) -> resource.addSearchParam().setName(name).setType(type));
        }
        return responses.ok(statement);
    }
}
