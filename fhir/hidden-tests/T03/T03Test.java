package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Function;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

/** T03: optimistic locking with If-Match on every PUT. */
class T03Test {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    static Resp putIfMatch(String path, Resource r, String ifMatch) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path))
                .PUT(HttpRequest.BodyPublishers.ofString(json(r)))
                .header("Content-Type", FHIR_JSON)
                .header("Accept", FHIR_JSON)
                .timeout(Duration.ofSeconds(30));
        if (ifMatch != null) b.header("If-Match", ifMatch);
        try {
            HttpResponse<String> resp = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Resp(resp.statusCode(), resp);
        } catch (Exception e) {
            throw new AssertionError("HTTP call failed: " + e, e);
        }
    }

    /**
     * Creates {@code v1}, updates it to {@code v2} with the matching If-Match, then tries a stale
     * and a future If-Match with {@code v3}; both must be 412 and leave version 2 untouched.
     */
    static <T extends Resource> void lockingWorks(String type, Class<T> cls, T v1, T v2, T v3, Function<T, String> marker) {
        String id = create(type, v1);
        v2.setId(id);
        v3.setId(id);

        Resp ok = putIfMatch("/" + type + "/" + id, v2, "W/\"1\"");
        assertEquals(200, ok.status(), () -> type + " update with matching If-Match failed: " + ok.body());
        assertEquals("W/\"2\"", ok.header("ETag"));

        assertOutcome(putIfMatch("/" + type + "/" + id, v3, "W/\"1\""), 412, "conflict");
        assertOutcome(putIfMatch("/" + type + "/" + id, v3, "W/\"7\""), 412, "conflict");

        T now = read(type, id, cls);
        assertEquals("2", now.getMeta().getVersionId(), type + " must be unchanged after 412");
        assertEquals(marker.apply(v2), marker.apply(now), type + " must be unchanged after 412");

        Resp again = putIfMatch("/" + type + "/" + id, v3, "W/\"2\"");
        assertEquals(200, again.status(), () -> type + " update with current If-Match failed: " + again.body());
        assertEquals("W/\"3\"", again.header("ETag"));
        assertEquals(marker.apply(v3), marker.apply(read(type, id, cls)));
    }

    @Test
    void patient() {
        String u = uniq();
        lockingWorks("Patient", Patient.class,
                PatientTest.patient("Lock" + u), PatientTest.patient("LockA" + u), PatientTest.patient("LockB" + u),
                p -> p.getNameFirstRep().getFamily());
    }

    @Test
    void practitioner() {
        String u = uniq();
        lockingWorks("Practitioner", Practitioner.class,
                PractitionerTest.practitioner("LockDoc" + u), PractitionerTest.practitioner("LockDocA" + u),
                PractitionerTest.practitioner("LockDocB" + u),
                p -> p.getNameFirstRep().getFamily());
    }

    @Test
    void encounter() {
        String pat = EncounterTest.newPatient();
        Encounter v2 = EncounterTest.encounter(pat);
        v2.setStatus(Encounter.EncounterStatus.PLANNED);
        Encounter v3 = EncounterTest.encounter(pat);
        v3.setStatus(Encounter.EncounterStatus.CANCELLED);
        lockingWorks("Encounter", Encounter.class, EncounterTest.encounter(pat), v2, v3, e -> e.getStatus().toCode());
    }

    @Test
    void observation() {
        String pat = EncounterTest.newPatient();
        Observation v2 = ObservationTest.observation(pat, "8867-4", "laboratory");
        Observation v3 = ObservationTest.observation(pat, "8867-4", "survey");
        lockingWorks("Observation", Observation.class, ObservationTest.observation(pat, "8867-4", "vital-signs"), v2, v3,
                o -> o.getCategoryFirstRep().getCodingFirstRep().getCode());
    }

    @Test
    void appointment() {
        String pat = EncounterTest.newPatient();
        String doc = EncounterTest.newPractitioner();
        Appointment[] v = new Appointment[3];
        for (int i = 0; i < 3; i++) {
            v[i] = AppointmentTest.appointment(pat, doc, "2032-02-02T10:00:00Z", "2032-02-02T10:30:00Z",
                    Appointment.AppointmentStatus.PROPOSED);
            v[i].setDescription("Lock v" + (i + 1));
        }
        lockingWorks("Appointment", Appointment.class, v[0], v[1], v[2], Appointment::getDescription);
    }

    @Test
    void withoutIfMatchUpdateWorksAsBefore() {
        String u = uniq();
        String id = create("Patient", PatientTest.patient("NoLock" + u));
        for (int i = 2; i <= 3; i++) {
            Patient p = PatientTest.patient("NoLock" + u + i);
            p.setId(id);
            Resp r = putIfMatch("/Patient/" + id, p, null);
            assertEquals(200, r.status(), r.body());
            assertEquals("W/\"" + i + "\"", r.header("ETag"));
        }
        assertEquals("NoLock" + u + "3", read("Patient", id, Patient.class).getNameFirstRep().getFamily());
    }
}
