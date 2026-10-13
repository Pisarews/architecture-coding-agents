package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

/** T15: the Prefer header (return=minimal | representation | OperationOutcome) on create and update. */
class T15Test {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    static Resp send(String method, String path, Resource r, String prefer) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path))
                .method(method, HttpRequest.BodyPublishers.ofString(json(r)))
                .header("Content-Type", FHIR_JSON)
                .header("Accept", FHIR_JSON)
                .timeout(Duration.ofSeconds(30));
        if (prefer != null) b.header("Prefer", prefer);
        try {
            HttpResponse<String> resp = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Resp(resp.statusCode(), resp);
        } catch (Exception e) {
            throw new AssertionError("HTTP call failed: " + e, e);
        }
    }

    /** Id from a Location header ending in /fhir/{type}/{id}/_history/{vid}. */
    static String idFromLocation(Resp r, String type, String vid) {
        String loc = r.header("Location");
        assertNotNull(loc, "Location header missing");
        Matcher m = Pattern.compile(".*/fhir/" + type + "/(\\d+)/_history/" + vid + "$").matcher(loc);
        assertTrue(m.matches(), "unexpected Location " + loc);
        return m.group(1);
    }

    static void assertEmptyBody(Resp r) {
        assertTrue(r.body() == null || r.body().isEmpty(), () -> "body must be empty but was: " + r.body());
    }

    /** create with return=minimal, update with return=minimal, both persisted. */
    static <T extends Resource> void minimalWorks(String type, Class<T> cls, T v1, T v2, Function<T, String> marker) {
        Resp created = send("POST", "/" + type, v1, "return=minimal");
        assertEquals(201, created.status(), () -> type + ": " + created.body());
        assertEmptyBody(created);
        assertEquals("W/\"1\"", created.header("ETag"), type);
        String id = idFromLocation(created, type, "1");
        assertEquals(marker.apply(v1), marker.apply(read(type, id, cls)), type);

        v2.setId(id);
        Resp updated = send("PUT", "/" + type + "/" + id, v2, "return=minimal");
        assertEquals(200, updated.status(), () -> type + ": " + updated.body());
        assertEmptyBody(updated);
        assertEquals("W/\"2\"", updated.header("ETag"), type);
        T now = read(type, id, cls);
        assertEquals("2", now.getMeta().getVersionId(), type);
        assertEquals(marker.apply(v2), marker.apply(now), type);
    }

    @Test
    void minimalForAllResourceTypes() {
        String u = uniq();
        minimalWorks("Patient", Patient.class, PatientTest.patient("Min" + u), PatientTest.patient("MinB" + u),
                p -> p.getNameFirstRep().getFamily());
        minimalWorks("Practitioner", Practitioner.class, PractitionerTest.practitioner("MinDoc" + u),
                PractitionerTest.practitioner("MinDocB" + u), p -> p.getNameFirstRep().getFamily());

        String pat = EncounterTest.newPatient();
        Encounter e2 = EncounterTest.encounter(pat);
        e2.setStatus(Encounter.EncounterStatus.PLANNED);
        minimalWorks("Encounter", Encounter.class, EncounterTest.encounter(pat), e2, e -> e.getStatus().toCode());

        minimalWorks("Observation", Observation.class,
                ObservationTest.observation(pat, "8867-4", "vital-signs"), ObservationTest.observation(pat, "8867-4", "laboratory"),
                o -> o.getCategoryFirstRep().getCodingFirstRep().getCode());

        String doc = EncounterTest.newPractitioner();
        Appointment a1 = AppointmentTest.appointment(pat, doc, "2034-02-01T10:00:00Z", "2034-02-01T10:30:00Z",
                Appointment.AppointmentStatus.PROPOSED);
        Appointment a2 = AppointmentTest.appointment(pat, doc, "2034-02-01T10:00:00Z", "2034-02-01T10:30:00Z",
                Appointment.AppointmentStatus.PROPOSED);
        a2.setDescription("Minimal v2 " + u);
        minimalWorks("Appointment", Appointment.class, a1, a2, Appointment::getDescription);
    }

    @Test
    void representationAndNoHeaderReturnTheResource() {
        String u = uniq();
        for (String prefer : new String[] {"return=representation", null}) {
            Resp created = send("POST", "/Patient", PatientTest.patient("Rep" + u), prefer);
            assertEquals(201, created.status(), created.body());
            Patient body = created.as(Patient.class);
            String id = body.getIdElement().getIdPart();
            assertEquals("Rep" + u, body.getNameFirstRep().getFamily());
            assertEquals("1", body.getMeta().getVersionId());
            assertEquals("W/\"1\"", created.header("ETag"));
            assertEquals(id, idFromLocation(created, "Patient", "1"));

            Patient v2 = PatientTest.patient("RepB" + u);
            v2.setId(id);
            Resp updated = send("PUT", "/Patient/" + id, v2, prefer);
            assertEquals(200, updated.status(), updated.body());
            Patient ub = updated.as(Patient.class);
            assertEquals("RepB" + u, ub.getNameFirstRep().getFamily());
            assertEquals("2", ub.getMeta().getVersionId());
            assertEquals("W/\"2\"", updated.header("ETag"));
        }
    }

    static void assertInformationOutcome(Resp r) {
        OperationOutcome oo = r.as(OperationOutcome.class);
        assertEquals(1, oo.getIssue().size(), r.body());
        assertEquals(OperationOutcome.IssueSeverity.INFORMATION, oo.getIssueFirstRep().getSeverity(), r.body());
    }

    @Test
    void operationOutcomeIsReturnedOnRequest() {
        String u = uniq();
        Resp created = send("POST", "/Patient", PatientTest.patient("Oo" + u), "return=OperationOutcome");
        assertEquals(201, created.status(), created.body());
        assertInformationOutcome(created);
        assertEquals("W/\"1\"", created.header("ETag"));
        String id = idFromLocation(created, "Patient", "1");
        assertEquals("Oo" + u, read("Patient", id, Patient.class).getNameFirstRep().getFamily());

        Patient v2 = PatientTest.patient("OoB" + u);
        v2.setId(id);
        Resp updated = send("PUT", "/Patient/" + id, v2, "return=OperationOutcome");
        assertEquals(200, updated.status(), updated.body());
        assertInformationOutcome(updated);
        assertEquals("W/\"2\"", updated.header("ETag"));
        assertEquals("OoB" + u, read("Patient", id, Patient.class).getNameFirstRep().getFamily());

        String pat = EncounterTest.newPatient();
        Resp obs = send("POST", "/Observation", ObservationTest.observation(pat, "8867-4", "vital-signs"), "return=OperationOutcome");
        assertEquals(201, obs.status(), obs.body());
        assertInformationOutcome(obs);
    }
}
