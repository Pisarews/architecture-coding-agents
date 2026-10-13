package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

/** T22: vread GET /fhir/{type}/{id}/_history/{vid} for all resource types. */
class T22Test {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    static Resp getAbsolute(String url) {
        try {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).GET().header("Accept", FHIR_JSON)
                    .timeout(Duration.ofSeconds(30)).build(), HttpResponse.BodyHandlers.ofString());
            return new Resp(r.statusCode(), r);
        } catch (Exception e) {
            throw new AssertionError("HTTP call failed: " + e, e);
        }
    }

    /** Creates v1, updates to v2 and v3, then vreads each version and checks content, meta and ETag. */
    static <T extends Resource> void vreadWorks(String type, Class<T> cls, List<T> versions, Function<T, String> marker) {
        Resp created = post(type, versions.get(0));
        assertEquals(201, created.status(), created.body());
        T first = created.as(cls);
        String id = first.getIdElement().getIdPart();
        List<T> stored = new ArrayList<>(List.of(first));
        for (int i = 1; i < versions.size(); i++) {
            T v = versions.get(i);
            v.setId(id);
            Resp r = put("/" + type + "/" + id, v);
            assertEquals(200, r.status(), r.body());
            stored.add(r.as(cls));
        }

        for (int i = 0; i < stored.size(); i++) {
            String vid = String.valueOf(i + 1);
            T expected = stored.get(i);
            Resp r = get("/" + type + "/" + id + "/_history/" + vid);
            assertEquals(200, r.status(), () -> type + " vread " + vid + ": " + r.body());
            assertEquals("W/\"" + vid + "\"", r.header("ETag"), type + " vread " + vid);
            T got = r.as(cls);
            assertEquals(id, got.getIdElement().getIdPart(), type);
            assertEquals(vid, got.getMeta().getVersionId(), type + " vread " + vid);
            assertEquals(expected.getMeta().getLastUpdated(), got.getMeta().getLastUpdated(), type + " vread " + vid + " lastUpdated");
            assertEquals(marker.apply(expected), marker.apply(got), type + " vread " + vid);
        }

        T current = read(type, id, cls);
        assertEquals(String.valueOf(stored.size()), current.getMeta().getVersionId(), type + ": read must still return the latest version");
        assertOutcome(get("/" + type + "/" + id + "/_history/" + (stored.size() + 1)), 404, "not-found");
        assertOutcome(get("/" + type + "/" + id + "/_history/99"), 404, "not-found");

        Resp loc = getAbsolute(created.header("Location"));
        assertEquals(200, loc.status(), () -> type + ": Location " + created.header("Location") + " must resolve: " + loc.body());
        assertEquals("1", loc.as(cls).getMeta().getVersionId());
        assertEquals(marker.apply(stored.get(0)), marker.apply(loc.as(cls)));
    }

    @Test
    void patient() {
        String u = uniq();
        vreadWorks("Patient", Patient.class,
                List.of(PatientTest.patient("Hist" + u, "A"), PatientTest.patient("HistB" + u, "B"), PatientTest.patient("HistC" + u)),
                p -> p.getNameFirstRep().getFamily() + "|" + p.getNameFirstRep().getGiven().stream().map(StringType::getValue).toList());
    }

    @Test
    void practitioner() {
        String u = uniq();
        vreadWorks("Practitioner", Practitioner.class,
                List.of(PractitionerTest.practitioner("HistDoc" + u), PractitionerTest.practitioner("HistDocB" + u)),
                p -> p.getNameFirstRep().getFamily());
    }

    @Test
    void encounter() {
        String pat = EncounterTest.newPatient();
        Encounter v2 = EncounterTest.encounter(pat);
        v2.setStatus(Encounter.EncounterStatus.PLANNED);
        Encounter v3 = EncounterTest.encounter(pat);
        v3.setStatus(Encounter.EncounterStatus.CANCELLED);
        vreadWorks("Encounter", Encounter.class, List.of(EncounterTest.encounter(pat), v2, v3), e -> e.getStatus().toCode());
    }

    @Test
    void observation() {
        String pat = EncounterTest.newPatient();
        vreadWorks("Observation", Observation.class,
                List.of(ObservationTest.observation(pat, "8867-4", "vital-signs"), ObservationTest.observation(pat, "8867-4", "laboratory")),
                o -> o.getCategoryFirstRep().getCodingFirstRep().getCode());
    }

    @Test
    void appointment() {
        String pat = EncounterTest.newPatient();
        String doc = EncounterTest.newPractitioner();
        Function<String, Appointment> appt = d -> {
            Appointment a = AppointmentTest.appointment(pat, doc, "2034-07-01T10:00:00Z", "2034-07-01T10:30:00Z",
                    Appointment.AppointmentStatus.PROPOSED);
            a.setDescription(d);
            return a;
        };
        vreadWorks("Appointment", Appointment.class, List.of(appt.apply("Hist v1"), appt.apply("Hist v2")),
                Appointment::getDescription);
    }

    @Test
    void unknownResourceIs404() {
        assertOutcome(get("/Patient/987654321/_history/1"), 404, "not-found");
    }

    @Test
    void capabilityStatementListsVread() {
        CapabilityStatement cs = get("/metadata").as(CapabilityStatement.class);
        Map<String, Set<String>> interactions = cs.getRestFirstRep().getResource().stream()
                .collect(Collectors.toMap(r -> r.getType(),
                        r -> r.getInteraction().stream().map(i -> i.getCode().toCode()).collect(Collectors.toSet())));
        for (String type : List.of("Patient", "Practitioner", "Encounter", "Observation", "Appointment")) {
            assertTrue(interactions.get(type).containsAll(Set.of("read", "vread", "create", "update", "search-type")),
                    type + ": " + interactions.get(type));
        }
    }
}
