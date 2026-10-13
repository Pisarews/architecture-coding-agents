package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * T04: POST critical glucose observations to fhir.notifications.critical-url.
 * The app is started with --fhir.notifications.critical-url=http://localhost:{mock.port}/critical.
 * Requests to any other path get 503.
 */
class T04Test {

    static final String PATH = "/critical";
    static final String FAIL_MARKER = "FAIL-ME-500";

    record Req(String method, String path, String contentType, String body) {}

    static final List<Req> REQUESTS = new CopyOnWriteArrayList<>();
    static HttpServer server;
    static ExecutorService pool;

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
            REQUESTS.add(new Req(ex.getRequestMethod(), ex.getRequestURI().getPath(),
                    ex.getRequestHeaders().getFirst("Content-Type"), body));
            // other paths are later tasks' outbound calls (terminology, webhook, mirror): 503 = "unavailable",
            // which every such ticket tolerates; never a 200 with a body they might misread
            int status = !PATH.equals(ex.getRequestURI().getPath()) ? 503 : body.contains(FAIL_MARKER) ? 500 : 200;
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

    /** Id of the Observation carried in a notification body, or null if it is not one. */
    static String observationId(Req r) {
        try {
            return CTX.newJsonParser().parseResource(Observation.class, r.body()).getIdElement().getIdPart();
        } catch (Exception e) {
            return null;
        }
    }

    static List<Req> notificationsFor(String id) {
        return REQUESTS.stream().filter(r -> PATH.equals(r.path()) && id.equals(observationId(r))).toList();
    }

    /** Waits up to 5 s for a matching request, then a little longer to catch duplicates. */
    static List<Req> awaitNotifications(String id) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (notificationsFor(id).isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(50);
        Thread.sleep(1_000);
        return notificationsFor(id);
    }

    static Observation glucose(String pat, String value) {
        Observation o = ObservationTest.observation(pat, "2339-0", "laboratory");
        o.setValue(new Quantity().setValue(new BigDecimal(value)).setUnit("mg/dL")
                .setSystem(ObservationTest.UCUM).setCode("mg/dL"));
        return o;
    }

    @Test
    void criticalGlucoseIsPostedExactlyOnce() throws Exception {
        String id = create("Observation", glucose(EncounterTest.newPatient(), "301"));
        List<Req> got = awaitNotifications(id);
        assertEquals(1, got.size(), "expected exactly one POST for observation " + id + ", got " + got);
        Req r = got.get(0);
        assertEquals("POST", r.method());
        assertNotNull(r.contentType(), "Content-Type header missing");
        assertTrue(r.contentType().startsWith("application/fhir+json"), r.contentType());
        Observation sent = CTX.newJsonParser().parseResource(Observation.class, r.body());
        assertEquals("2339-0", sent.getCode().getCodingFirstRep().getCode());
        assertEquals(0, new BigDecimal("301").compareTo(sent.getValueQuantity().getValue()));
    }

    @Test
    void eachQualifyingCreateIsPosted() throws Exception {
        String pat = EncounterTest.newPatient();
        String a = create("Observation", glucose(pat, "450"));
        String b = create("Observation", glucose(pat, "300.5"));
        assertEquals(1, awaitNotifications(a).size());
        assertEquals(1, awaitNotifications(b).size());
    }

    @Test
    void noPostForNonQualifyingObservations() throws Exception {
        String pat = EncounterTest.newPatient();
        String atLimit = create("Observation", glucose(pat, "300"));
        String low = create("Observation", glucose(pat, "95"));
        Observation otherCode = glucose(pat, "450");
        otherCode.getCode().getCodingFirstRep().setCode("2345-7");
        String other = create("Observation", otherCode);
        String noValue = create("Observation", ObservationTest.observation(pat, "2339-0", "laboratory"));

        String updated = create("Observation", glucose(pat, "120"));
        Observation high = glucose(pat, "480");
        high.setId(updated);
        assertEquals(200, put("/Observation/" + updated, high).status());

        Thread.sleep(2_000);
        for (String id : Set.of(atLimit, low, other, noValue, updated)) {
            assertEquals(List.of(), notificationsFor(id), "no POST expected for observation " + id);
        }
    }

    @Test
    void updateOfCriticalObservationIsNotPostedAgain() throws Exception {
        String pat = EncounterTest.newPatient();
        String id = create("Observation", glucose(pat, "350"));
        assertEquals(1, awaitNotifications(id).size());
        Observation again = glucose(pat, "390");
        again.setId(id);
        assertEquals(200, put("/Observation/" + id, again).status());
        Thread.sleep(2_000);
        assertEquals(1, notificationsFor(id).size(), "an update must not trigger another POST");
    }

    @Test
    void endpointFailureDoesNotFailCreate() {
        Observation o = glucose(EncounterTest.newPatient(), "520");
        o.getCode().getCodingFirstRep().setDisplay(FAIL_MARKER);
        Resp resp = post("Observation", o);
        assertEquals(201, resp.status(), resp.body());
        String id = resp.as(Observation.class).getIdElement().getIdPart();
        read("Observation", id, Observation.class);
    }
}
