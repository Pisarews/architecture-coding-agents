package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * T20: change-feed POSTs to fhir.events.webhook-url after every successful create/update/delete.
 * The app is started with --fhir.events.webhook-url=http://localhost:{mock.port}/events.
 * On each event the mock reads the resource back from the server: if the change were sent before it
 * is committed, the read would not yet show the event's version. Mode "fail" answers 500, "drop"
 * closes the connection. Requests to any other path (other tasks' outbound calls) get 503.
 */
class T20Test {

    static final String PATH = "/events";

    /** One received event; {@code visibleVersion} is the versionId the server returned at receipt (null = not readable). */
    record Event(String type, String id, String versionId, String action, String visibleVersion, String raw) {}

    static final List<Event> EVENTS = new CopyOnWriteArrayList<>();
    static final ObjectMapper JSON = new ObjectMapper();
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    static volatile String mode = "ok";
    static HttpServer server;
    static ExecutorService pool;

    static String text(JsonNode n, String field) {
        JsonNode v = n == null ? null : n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    /** versionId currently visible on the server, or null if the resource cannot be read. */
    static String visibleVersion(String type, String id) {
        if (type == null || id == null || !type.matches("[A-Za-z]+") || !id.matches("\\d+")) return null;
        try {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(BASE + "/" + type + "/" + id)).GET()
                    .header("Accept", FHIR_JSON).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) return null;
            return JSON.readTree(r.body()).path("meta").path("versionId").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    @BeforeAll
    static void startMock() throws Exception {
        int port = Integer.getInteger("mock.port", 18080);
        long deadline = System.currentTimeMillis() + 20_000;
        while (true) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                break;
            } catch (BindException e) {
                if (System.currentTimeMillis() > deadline) throw e;
                Thread.sleep(200);
            }
        }
        server.createContext("/", ex -> {
            String body;
            try (InputStream in = ex.getRequestBody()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            int status = 503;
            if (PATH.equals(ex.getRequestURI().getPath()) && "POST".equals(ex.getRequestMethod())) {
                JsonNode n;
                try {
                    n = JSON.readTree(body);
                } catch (Exception e) {
                    n = null;
                }
                String type = text(n, "type");
                String id = text(n, "id");
                String action = text(n, "action");
                String visible = "delete".equals(action) ? null : visibleVersion(type, id);
                EVENTS.add(new Event(type, id, text(n, "versionId"), action, visible, body));
                String m = mode;
                if ("drop".equals(m)) {
                    ex.close();
                    return;
                }
                status = "fail".equals(m) ? 500 : 200;
            }
            byte[] out = "{}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(status, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        pool = Executors.newFixedThreadPool(4);
        server.setExecutor(pool);
        server.start();
    }

    @AfterAll
    static void stopMock() {
        if (server != null) server.stop(0);
        if (pool != null) pool.shutdownNow();
    }

    static List<Event> eventsFor(String type, String id) {
        return EVENTS.stream().filter(e -> type.equals(e.type()) && id.equals(e.id())).toList();
    }

    /** Waits up to 5 s until at least {@code n} events for the resource arrived, then a bit longer for extras. */
    static List<Event> awaitEvents(String type, String id, int n) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (eventsFor(type, id).size() < n && System.currentTimeMillis() < deadline) Thread.sleep(50);
        Thread.sleep(1_000);
        return eventsFor(type, id);
    }

    static List<String> summary(List<Event> events) {
        return events.stream().map(e -> e.action() + ":" + e.versionId()).sorted().toList();
    }

    static void assertCommitted(List<Event> events) {
        for (Event e : events) {
            assertNotNull(e.visibleVersion(), () -> "resource not readable when its event arrived (sent before commit?): " + e.raw());
            assertTrue(Integer.parseInt(e.visibleVersion()) >= Integer.parseInt(e.versionId()),
                    () -> "event sent before its change was committed: " + e.raw() + " but server showed version " + e.visibleVersion());
        }
    }

    @Test
    void createAndUpdatesOfPatientAreSent() throws Exception {
        String fam = "Feed" + uniq();
        String id = create("Patient", PatientTest.patient(fam));
        for (int i = 2; i <= 3; i++) {
            Patient p = PatientTest.patient(fam + i);
            p.setId(id);
            assertEquals(200, put("/Patient/" + id, p).status());
        }
        List<Event> got = awaitEvents("Patient", id, 3);
        assertEquals(List.of("create:1", "update:2", "update:3"), summary(got), () -> "events: " + got);
    }

    @Test
    void everyResourceTypeIsSent() throws Exception {
        String pat = EncounterTest.newPatient();
        String doc = EncounterTest.newPractitioner();
        String enc = create("Encounter", EncounterTest.encounter(pat));
        String obs = create("Observation", ObservationTest.observation(pat, "8867-4", "vital-signs"));
        Appointment a = AppointmentTest.appointment(pat, doc, "2034-06-01T10:00:00Z", "2034-06-01T10:30:00Z",
                Appointment.AppointmentStatus.PROPOSED);
        String app = create("Appointment", a);

        for (String[] r : new String[][] {{"Patient", pat}, {"Practitioner", doc}, {"Encounter", enc}, {"Observation", obs},
                {"Appointment", app}}) {
            List<Event> got = awaitEvents(r[0], r[1], 1);
            assertEquals(List.of("create:1"), summary(got), () -> r[0] + " events: " + got);
        }

        Encounter e = EncounterTest.encounter(pat);
        e.setId(enc);
        e.setStatus(Encounter.EncounterStatus.PLANNED);
        assertEquals(200, put("/Encounter/" + enc, e).status());
        a.setId(app);
        a.setDescription("Feed v2");
        assertEquals(200, put("/Appointment/" + app, a).status());
        assertEquals(List.of("create:1", "update:2"), summary(awaitEvents("Encounter", enc, 2)));
        assertEquals(List.of("create:1", "update:2"), summary(awaitEvents("Appointment", app, 2)));
    }

    @Test
    void eventsAreSentAfterCommit() throws Exception {
        String fam = "Commit" + uniq();
        String id = create("Patient", PatientTest.patient(fam));
        Patient p = PatientTest.patient(fam + "b");
        p.setId(id);
        assertEquals(200, put("/Patient/" + id, p).status());
        String obs = create("Observation", ObservationTest.observation(id, "8867-4", "vital-signs"));
        List<Event> got = new ArrayList<>(awaitEvents("Patient", id, 2));
        got.addAll(awaitEvents("Observation", obs, 1));
        assertEquals(3, got.size(), () -> "events: " + got);
        assertCommitted(got);
    }

    @Test
    void failedRequestsSendNothing() throws Exception {
        String fam = "NoFeed" + uniq();
        String id = create("Patient", PatientTest.patient(fam));
        assertEquals(1, awaitEvents("Patient", id, 1).size());
        int mark = EVENTS.size();

        Patient future = PatientTest.patient(fam);
        future.setId(id);
        future.setBirthDateElement(new DateType(LocalDate.now().plusYears(1).toString()));
        assertOutcome(put("/Patient/" + id, future), 422, "business-rule");
        assertOutcome(put("/Patient/987654321", withId(PatientTest.patient(fam), "987654321")), 404, "not-found");
        Patient noName = new Patient();
        noName.addName().addGiven("OnlyGiven");
        assertOutcome(post("Patient", noName), 422, "required");
        Patient futureNew = PatientTest.patient(fam + "n");
        futureNew.setBirthDateElement(new DateType(LocalDate.now().plusYears(1).toString()));
        assertOutcome(post("Patient", futureNew), 422, "business-rule");
        assertOutcome(post("Observation", ObservationTest.observation("987654321", "8867-4", "vital-signs")), 422, "not-found");

        Thread.sleep(2_000);
        assertEquals(List.of("create:1"), summary(eventsFor("Patient", id)), "a failed update must not be sent");
        List<Event> snapshot = new ArrayList<>(EVENTS);
        List<Event> later = snapshot.subList(mark, snapshot.size());
        for (Event e : later) {
            if ("delete".equals(e.action())) continue;
            assertNotNull(e.visibleVersion(), () -> "event for a resource that does not exist (failed request?): " + e.raw());
        }
    }

    static Patient withId(Patient p, String id) {
        p.setId(id);
        return p;
    }

    @Test
    void webhookFailureDoesNotFailTheRequest() throws Exception {
        for (String m : List.of("fail", "drop")) {
            mode = m;
            try {
                String fam = "FeedFail" + m + uniq();
                Resp created = post("Patient", PatientTest.patient(fam));
                assertEquals(201, created.status(), () -> m + ": " + created.body());
                String id = created.as(Patient.class).getIdElement().getIdPart();
                Patient p = PatientTest.patient(fam + "b");
                p.setId(id);
                Resp updated = put("/Patient/" + id, p);
                assertEquals(200, updated.status(), () -> m + ": " + updated.body());
                Resp obs = post("Observation", ObservationTest.observation(id, "8867-4", "vital-signs"));
                assertEquals(201, obs.status(), () -> m + ": " + obs.body());
                assertEquals(fam + "b", read("Patient", id, Patient.class).getNameFirstRep().getFamily());
            } finally {
                mode = "ok";
            }
        }
    }

    @Test
    void deleteIsSentWhenSupported() throws Exception {
        String id = create("Patient", PatientTest.patient("FeedDel" + uniq()));
        assertEquals(1, awaitEvents("Patient", id, 1).size());
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(BASE + "/Patient/" + id)).DELETE()
                .header("Accept", FHIR_JSON).timeout(Duration.ofSeconds(30)).build(), HttpResponse.BodyHandlers.ofString());
        assumeTrue(r.statusCode() == 204, "Patient delete is not supported (status " + r.statusCode() + ")");
        List<Event> got = awaitEvents("Patient", id, 2);
        assertEquals(2, got.size(), () -> "events: " + got);
        assertEquals(1, got.stream().filter(e -> "delete".equals(e.action())).count(), () -> "events: " + got);
    }
}
