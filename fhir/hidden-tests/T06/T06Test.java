package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

/** T06: DELETE /fhir/Patient/{id}. */
class T06Test {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    static Resp delete(String path) {
        try {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(BASE + path)).DELETE()
                    .header("Accept", FHIR_JSON).timeout(Duration.ofSeconds(30)).build(),
                    HttpResponse.BodyHandlers.ofString());
            return new Resp(r.statusCode(), r);
        } catch (Exception e) {
            throw new AssertionError("HTTP call failed: " + e, e);
        }
    }

    static Patient patient(String fam) {
        Patient p = PatientTest.patient(fam, "Del");
        p.addIdentifier().setSystem("urn:sys:del").setValue("DEL-" + fam);
        return p;
    }

    @Test
    void unreferencedPatientIsDeleted() {
        String fam = "Gone" + uniq();
        String id = create("Patient", patient(fam));
        String keep = create("Patient", patient(fam + "k"));

        Resp resp = delete("/Patient/" + id);
        assertEquals(204, resp.status(), resp.body());

        assertOutcome(get("/Patient/" + id), 410, "deleted");
        assertEquals(List.of(keep), ids(search("Patient", "family=" + fam)));
        assertEquals(List.of(), ids(search("Patient", "identifier=urn:sys:del|DEL-" + fam)));
        assertFalse(ids(search("Patient")).contains(id));
        read("Patient", keep, Patient.class);
    }

    static void assertNotDeleted(String pat) {
        assertOutcome(delete("/Patient/" + pat), 409, "conflict");
        read("Patient", pat, Patient.class);
        assertTrue(ids(search("Patient", "identifier=" + read("Patient", pat, Patient.class).getIdentifierFirstRep().getValue()))
                .contains(pat));
    }

    @Test
    void patientReferencedByEncounterIsNotDeleted() {
        String pat = create("Patient", patient("RefEnc" + uniq()));
        create("Encounter", EncounterTest.encounter(pat));
        assertNotDeleted(pat);
    }

    @Test
    void patientReferencedByObservationIsNotDeleted() {
        String pat = create("Patient", patient("RefObs" + uniq()));
        create("Observation", ObservationTest.observation(pat, "8867-4", "vital-signs"));
        assertNotDeleted(pat);
    }

    @Test
    void patientReferencedByAppointmentIsNotDeleted() {
        String pat = create("Patient", patient("RefApp" + uniq()));
        create("Appointment", AppointmentTest.appointment(pat, EncounterTest.newPractitioner(),
                "2032-03-03T10:00:00Z", "2032-03-03T10:30:00Z", Appointment.AppointmentStatus.PROPOSED));
        assertNotDeleted(pat);
    }

    @Test
    void unknownIdIs404() {
        assertOutcome(delete("/Patient/987654321"), 404, "not-found");
    }

    @Test
    void capabilityStatementListsDeleteForPatientOnly() {
        CapabilityStatement cs = get("/metadata").as(CapabilityStatement.class);
        Map<String, Set<String>> interactions = cs.getRestFirstRep().getResource().stream()
                .collect(Collectors.toMap(r -> r.getType(),
                        r -> r.getInteraction().stream().map(i -> i.getCode().toCode()).collect(Collectors.toSet())));
        assertTrue(interactions.get("Patient").containsAll(Set.of("read", "create", "update", "search-type", "delete")),
                interactions.toString());
        for (String type : List.of("Practitioner", "Encounter", "Observation", "Appointment")) {
            assertFalse(interactions.get(type).contains("delete"), type + ": " + interactions.get(type));
        }
    }
}
