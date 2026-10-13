package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

/** T19: If-None-Match on read (304) and a Last-Modified header on every read. */
class T19Test {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    static Resp readWith(String path, String ifNoneMatch) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path)).GET()
                .header("Accept", FHIR_JSON).timeout(Duration.ofSeconds(30));
        if (ifNoneMatch != null) b.header("If-None-Match", ifNoneMatch);
        try {
            HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Resp(r.statusCode(), r);
        } catch (Exception e) {
            throw new AssertionError("HTTP call failed: " + e, e);
        }
    }

    /** Last-Modified must be an RFC 1123 date equal to meta.lastUpdated at second precision. */
    static void assertLastModified(Resp r, Resource body) {
        String lm = r.header("Last-Modified");
        assertNotNull(lm, "Last-Modified header missing");
        Instant parsed = ZonedDateTime.parse(lm, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        Instant lastUpdated = body.getMeta().getLastUpdated().toInstant();
        long diff = Math.abs(parsed.toEpochMilli() - lastUpdated.toEpochMilli());
        assertTrue(diff < 1000, () -> "Last-Modified " + lm + " does not match meta.lastUpdated " + lastUpdated);
    }

    static <T extends Resource> void conditionalReadWorks(String type, Class<T> cls, T v1, T v2) {
        String id = create(type, v1);
        String path = "/" + type + "/" + id;

        Resp plain = readWith(path, null);
        assertEquals(200, plain.status(), plain.body());
        assertLastModified(plain, plain.as(cls));

        Resp notModified = readWith(path, "W/\"1\"");
        assertEquals(304, notModified.status(), () -> type + ": expected 304 but got " + notModified.status() + " " + notModified.body());
        assertTrue(notModified.body() == null || notModified.body().isEmpty(), type + ": 304 must have no body");
        assertEquals("W/\"1\"", notModified.header("ETag"), type);

        v2.setId(id);
        assertEquals(200, put(path, v2).status());

        Resp changed = readWith(path, "W/\"1\"");
        assertEquals(200, changed.status(), () -> type + ": stale If-None-Match must give 200: " + changed.body());
        assertEquals("W/\"2\"", changed.header("ETag"), type);
        T body = changed.as(cls);
        assertEquals("2", body.getMeta().getVersionId(), type);
        assertLastModified(changed, body);

        Resp current = readWith(path, "W/\"2\"");
        assertEquals(304, current.status(), type);
        assertEquals("W/\"2\"", current.header("ETag"), type);
    }

    @Test
    void patient() {
        String u = uniq();
        conditionalReadWorks("Patient", Patient.class, PatientTest.patient("Cond" + u), PatientTest.patient("CondB" + u));
    }

    @Test
    void practitioner() {
        String u = uniq();
        conditionalReadWorks("Practitioner", Practitioner.class,
                PractitionerTest.practitioner("CondDoc" + u), PractitionerTest.practitioner("CondDocB" + u));
    }

    @Test
    void encounter() {
        String pat = EncounterTest.newPatient();
        Encounter v2 = EncounterTest.encounter(pat);
        v2.setStatus(Encounter.EncounterStatus.PLANNED);
        conditionalReadWorks("Encounter", Encounter.class, EncounterTest.encounter(pat), v2);
    }

    @Test
    void observation() {
        String pat = EncounterTest.newPatient();
        conditionalReadWorks("Observation", Observation.class,
                ObservationTest.observation(pat, "8867-4", "vital-signs"), ObservationTest.observation(pat, "8867-4", "laboratory"));
    }

    @Test
    void appointment() {
        String pat = EncounterTest.newPatient();
        String doc = EncounterTest.newPractitioner();
        Function<String, Appointment> appt = d -> {
            Appointment a = AppointmentTest.appointment(pat, doc, "2034-05-01T10:00:00Z", "2034-05-01T10:30:00Z",
                    Appointment.AppointmentStatus.PROPOSED);
            a.setDescription(d);
            return a;
        };
        conditionalReadWorks("Appointment", Appointment.class, appt.apply("Cond v1"), appt.apply("Cond v2"));
    }

    @Test
    void otherVersionGives200WithBody() {
        String fam = "CondOther" + uniq();
        String id = create("Patient", PatientTest.patient(fam));
        Resp r = readWith("/Patient/" + id, "W/\"5\"");
        assertEquals(200, r.status(), r.body());
        assertEquals("W/\"1\"", r.header("ETag"));
        assertEquals(fam, r.as(Patient.class).getNameFirstRep().getFamily());
    }
}
