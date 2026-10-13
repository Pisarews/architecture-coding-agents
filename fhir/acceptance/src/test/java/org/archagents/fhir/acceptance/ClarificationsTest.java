package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

/** SPEC §8 clarifications C1–C8. */
class ClarificationsTest {

    @Test
    void c1_participantWithoutActorIs422Required() {
        Appointment a = AppointmentTest.booked(EncounterTest.newPatient(), EncounterTest.newPractitioner(),
                "2031-07-01T10:00:00Z", "2031-07-01T10:30:00Z");
        a.addParticipant().setStatus(Appointment.ParticipationStatus.ACCEPTED);
        assertOutcome(post("Appointment", a), 422, "required");
    }

    @Test
    void c2_invalidEnumCodeIs400Structure() {
        String pat = EncounterTest.newPatient();
        String body = "{\"resourceType\":\"Encounter\",\"status\":\"bogus\",\"subject\":{\"reference\":\"Patient/" + pat + "\"}}";
        assertOutcome(postRaw("Encounter", body, FHIR_JSON), 400, "structure");
    }

    @Test
    void c3_putChecksParseThenIdThenExistence() {
        assertOutcome(putRaw("/Patient/987654321", "{broken"), 400, "structure");
        Patient p = PatientTest.patient("Order");
        p.setId("123");
        assertOutcome(put("/Patient/987654321", p), 400, "invalid");
        p.setId("987654321");
        p.getName().clear();
        assertOutcome(put("/Patient/987654321", p), 404, "not-found");
    }

    @Test
    void c4_malformedReferenceSearchValueIs400Invalid() {
        assertOutcome(get("/Encounter?patient=abc"), 400, "invalid");
        assertOutcome(get("/Encounter?patient=Practitioner/1"), 400, "invalid");
    }

    @Test
    void c5_wrongTypeReferenceInBodyIs422NotFound() {
        String doc = EncounterTest.newPractitioner();
        Encounter e = EncounterTest.encounter("1");
        e.setSubject(new Reference("Practitioner/" + doc));
        assertOutcome(post("Encounter", e), 422, "not-found");
    }

    @Test
    void c6_categorySearchIgnoresSystem() {
        String pat = EncounterTest.newPatient();
        String o = create("Observation", ObservationTest.observation(pat, "8867-4", "vital-signs"));
        assertEquals(List.of(o), ids(search("Observation", "patient=" + pat, "category=" + ObservationTest.CAT + "|vital-signs")));
        assertEquals(List.of(o), ids(search("Observation", "patient=" + pat, "category=urn:other|vital-signs")));
    }

    @Test
    void c7_unsupportedMethodOnKnownTypeIs405() throws Exception {
        String id = create("Patient", PatientTest.patient("Del" + uniq()));
        HttpResponse<String> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(BASE + "/Patient/" + id)).DELETE().header("Accept", FHIR_JSON).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(405, r.statusCode(), r.body());
        OperationOutcome oo = CTX.newJsonParser().parseResource(OperationOutcome.class, r.body());
        assertEquals("not-supported", oo.getIssueFirstRep().getCode().toCode());
        read("Patient", id, Patient.class);
    }

    @Test
    void c8_unknownTypeAnyMethodIs404() {
        assertOutcome(postRaw("Medication", "{\"resourceType\":\"Medication\"}", FHIR_JSON), 404, "not-supported");
    }

    @Test
    void c9_requiredRulesComeBeforeReferenceRules() {
        Encounter e = EncounterTest.encounter("1");
        e.setStatus(null);
        e.setSubject(new Reference("Practitioner/" + EncounterTest.newPractitioner()));
        assertOutcome(post("Encounter", e), 422, "required");
    }

    @Test
    void c9_nonNumericReferenceIs422NotFound() {
        assertOutcome(post("Encounter", EncounterTest.encounter("abc")), 422, "not-found");
    }

    @Test
    void c9_putToUnknownIdIs404EvenWithBadReference() {
        Encounter e = EncounterTest.encounter("abc");
        e.setId("987654321");
        assertOutcome(put("/Encounter/987654321", e), 404, "not-found");
    }

    @Test
    void c10_emptySystemTokenMatchesOnlyRowsWithoutSystem() {
        String v = "NS-" + uniq();
        Patient a = PatientTest.patient("Tok");
        a.addIdentifier().setValue(v);
        String noSys = create("Patient", a);
        Patient b = PatientTest.patient("Tok");
        b.addIdentifier().setSystem("urn:sys:x").setValue(v);
        create("Patient", b);
        assertEquals(List.of(noSys), ids(search("Patient", "identifier=|" + v)));
    }

    @Test
    void c11_codesOutsideSupportedSubsetAre400Structure() {
        String pat = EncounterTest.newPatient();
        assertOutcome(postRaw("Encounter", "{\"resourceType\":\"Encounter\",\"status\":\"arrived\",\"subject\":{\"reference\":\"Patient/" + pat + "\"}}", FHIR_JSON),
                400, "structure");
        assertOutcome(postRaw("Patient", "{\"resourceType\":\"Patient\",\"name\":[{\"family\":\"Fax\"}],\"telecom\":[{\"system\":\"fax\",\"value\":\"1\"}]}", FHIR_JSON),
                400, "structure");
        Patient p = PatientTest.patient("C11");
        p.setId("987654321");
        String body = json(p).replace("\"resourceType\":\"Patient\"", "\"resourceType\":\"Patient\",\"gender\":\"robot\"");
        assertOutcome(putRaw("/Patient/987654321", body), 400, "structure");
    }

    @Test
    void c12_nonNumericUrlIdIs404() {
        assertOutcome(get("/Patient/abc"), 404, "not-found");
        Patient p = PatientTest.patient("C12");
        p.setId("abc");
        assertOutcome(put("/Patient/abc", p), 404, "not-found");
    }

    @Test
    void c13_unknownTokenValueGivesEmptyResult() {
        assertEquals(0, search("Patient", "gender=bogus").getTotal());
        assertEquals(0, search("Encounter", "status=bogus").getTotal());
    }
}
