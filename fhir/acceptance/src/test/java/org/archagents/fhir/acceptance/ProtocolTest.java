package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.Test;

/** Capability statement, content negotiation and generic error handling. */
class ProtocolTest {

    static final Map<String, Set<String>> SEARCH_PARAMS = Map.of(
            "Patient", Set.of("family:string", "given:string", "identifier:token", "gender:token", "birthdate:date"),
            "Practitioner", Set.of("family:string", "identifier:token"),
            "Encounter", Set.of("patient:reference", "status:token"),
            "Observation", Set.of("patient:reference", "code:token", "category:token"),
            "Appointment", Set.of("patient:reference", "practitioner:reference", "status:token", "date:date"));

    @Test
    void capabilityStatementDescribesServer() {
        Resp resp = get("/metadata");
        assertEquals(200, resp.status(), resp.body());
        CapabilityStatement cs = resp.as(CapabilityStatement.class);
        assertEquals("active", cs.getStatus().toCode());
        assertEquals("instance", cs.getKind().toCode());
        assertEquals("4.0.1", cs.getFhirVersion().toCode());
        assertEquals(Set.of("json"), cs.getFormat().stream().map(f -> f.getValue()).collect(Collectors.toSet()));
        assertEquals(1, cs.getRest().size());
        assertEquals("server", cs.getRestFirstRep().getMode().toCode());

        Map<String, CapabilityStatementRestResourceComponent> byType = cs.getRestFirstRep().getResource().stream()
                .collect(Collectors.toMap(r -> r.getType(), r -> r));
        assertEquals(SEARCH_PARAMS.keySet(), byType.keySet());
        for (var e : SEARCH_PARAMS.entrySet()) {
            var res = byType.get(e.getKey());
            assertEquals(Set.of("read", "create", "update", "search-type"),
                    res.getInteraction().stream().map(i -> i.getCode().toCode()).collect(Collectors.toSet()), e.getKey());
            assertEquals(e.getValue(),
                    res.getSearchParam().stream().map(p -> p.getName() + ":" + p.getType().toCode()).collect(Collectors.toSet()),
                    e.getKey());
        }
    }

    @Test
    void plainApplicationJsonIsAccepted() {
        Resp resp = postRaw("Patient", json(PatientTest.patient("Json" + uniq())), "application/json");
        assertEquals(201, resp.status(), resp.body());
        resp.as(Patient.class);
    }

    @Test
    void unparseableBodyIs400Structure() {
        assertOutcome(postRaw("Patient", "{not json", FHIR_JSON), 400, "structure");
    }

    @Test
    void nonFhirJsonIs400Structure() {
        assertOutcome(postRaw("Patient", "{\"hello\":\"world\"}", FHIR_JSON), 400, "structure");
    }

    @Test
    void wrongResourceTypeForUrlIs400Structure() {
        assertOutcome(post("Patient", PractitionerTest.practitioner("Wrong" + uniq())), 400, "structure");
    }

    @Test
    void wrongResourceTypeOnUpdateIs400Structure() {
        String id = create("Patient", PatientTest.patient("WrongUpd" + uniq()));
        Practitioner p = PractitionerTest.practitioner("X");
        p.setId(id);
        assertOutcome(put("/Patient/" + id, p), 400, "structure");
    }

    @Test
    void unknownResourceTypeIs404NotSupported() {
        assertOutcome(get("/Medication/1"), 404, "not-supported");
        assertOutcome(get("/Medication"), 404, "not-supported");
    }
}
