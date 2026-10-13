package org.archagents.fhir.controller;

import java.util.Date;
import org.archagents.fhir.util.FhirUtil;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementKind;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r4.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetadataController {

    @GetMapping("/fhir/metadata")
    public ResponseEntity<String> capabilities() {
        CapabilityStatement statement = new CapabilityStatement()
                .setStatus(PublicationStatus.ACTIVE)
                .setDate(new Date())
                .setKind(CapabilityStatementKind.INSTANCE)
                .setFhirVersion(FHIRVersion._4_0_1);
        statement.addFormat("json");

        CapabilityStatementRestComponent rest = statement.addRest().setMode(RestfulCapabilityMode.SERVER);
        addResource(rest, "Patient", "family:string", "given:string", "identifier:token", "gender:token", "birthdate:date");
        addResource(rest, "Practitioner", "family:string", "identifier:token");
        addResource(rest, "Encounter", "patient:reference", "status:token");
        addResource(rest, "Observation", "patient:reference", "code:token", "category:token");
        addResource(rest, "Appointment", "patient:reference", "practitioner:reference", "status:token", "date:date");

        return FhirUtil.response(HttpStatus.OK, statement);
    }

    private static void addResource(CapabilityStatementRestComponent rest, String type, String... searchParams) {
        CapabilityStatementRestResourceComponent resource = rest.addResource().setType(type);
        resource.addInteraction().setCode(TypeRestfulInteraction.READ);
        resource.addInteraction().setCode(TypeRestfulInteraction.CREATE);
        resource.addInteraction().setCode(TypeRestfulInteraction.UPDATE);
        resource.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
        for (String param : searchParams) {
            String[] nameAndType = param.split(":");
            resource.addSearchParam().setName(nameAndType[0]).setType(SearchParamType.fromCode(nameAndType[1]));
        }
    }
}
