package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * T16: LOINC codes are validated against fhir.terminology.base-url on Observation create and update.
 * The app is started with --fhir.terminology.base-url=http://localhost:{mock.port}/terminology.
 * The mock answers GET /terminology/CodeSystem/$validate-code with Parameters result=true for every code,
 * except: INVALID → result=false, SERVER_ERROR → 500, DROP → connection closed without a response.
 * Requests to any other path (other tasks' outbound calls) get 503.
 */
class T16Test {

    static final String PATH = "/terminology/CodeSystem/$validate-code";
    static final String INVALID = "99999-9";
    static final String SERVER_ERROR = "99998-7";
    static final String DROP = "99997-5";

    record Req(String method, String path, Map<String, String> query) {}

    static final List<Req> REQUESTS = new CopyOnWriteArrayList<>();
    static HttpServer server;
    static ExecutorService pool;

    static Map<String, String> query(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null) return m;
        for (String kv : raw.split("&")) {
            int eq = kv.indexOf('=');
            if (eq > 0) m.put(kv.substring(0, eq), kv.substring(eq + 1));
        }
        return m;
    }

    static String parameters(boolean result) {
        return "{\"resourceType\":\"Parameters\",\"parameter\":[{\"name\":\"result\",\"valueBoolean\":" + result + "}]}";
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
            try (InputStream in = ex.getRequestBody()) {
                in.readAllBytes();
            }
            String path = ex.getRequestURI().getPath();
            Map<String, String> q = query(ex.getRequestURI().getQuery());
            REQUESTS.add(new Req(ex.getRequestMethod(), path, q));
            int status;
            String body;
            if (!PATH.equals(path)) {
                status = 503;
                body = "{}";
            } else {
                String code = q.getOrDefault("code", "");
                if (DROP.equals(code)) {
                    ex.close();
                    return;
                }
                status = SERVER_ERROR.equals(code) ? 500 : 200;
                body = status == 500 ? "{}" : parameters(!INVALID.equals(code));
            }
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", status == 200 ? "application/fhir+json" : "application/json");
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

    static long lookups(String code) {
        return REQUESTS.stream()
                .filter(r -> "GET".equals(r.method()) && PATH.equals(r.path()) && code.equals(r.query().get("code"))
                        && ObservationTest.LOINC.equals(r.query().get("url")))
                .count();
    }

    static Observation obs(String pat, String system, String code) {
        Observation o = ObservationTest.observation(pat, "8867-4", "vital-signs");
        o.setCode(new CodeableConcept(new Coding(system, code, "test")));
        return o;
    }

    @Test
    void validCodeIsCheckedAndAccepted() {
        String code = "T16-" + uniq();
        String id = create("Observation", obs(EncounterTest.newPatient(), ObservationTest.LOINC, code));
        assertEquals(code, read("Observation", id, Observation.class).getCode().getCodingFirstRep().getCode());
        assertTrue(lookups(code) >= 1, "expected GET " + PATH + "?url=" + ObservationTest.LOINC + "&code=" + code);
    }

    @Test
    void invalidCodeOnCreateIs422CodeInvalid() {
        String pat = EncounterTest.newPatient();
        long before = lookups(INVALID);
        assertOutcome(post("Observation", obs(pat, ObservationTest.LOINC, INVALID)), 422, "code-invalid");
        assertTrue(lookups(INVALID) > before, "the terminology server must be asked");
        assertEquals(0, search("Observation", "patient=" + pat).getTotal(), "nothing may be stored");
    }

    @Test
    void invalidCodeOnUpdateIs422AndChangesNothing() {
        String pat = EncounterTest.newPatient();
        String valid = "T16-" + uniq();
        String id = create("Observation", obs(pat, ObservationTest.LOINC, valid));
        Observation o = obs(pat, ObservationTest.LOINC, INVALID);
        o.setId(id);
        assertOutcome(put("/Observation/" + id, o), 422, "code-invalid");
        Observation now = read("Observation", id, Observation.class);
        assertEquals("1", now.getMeta().getVersionId());
        assertEquals(valid, now.getCode().getCodingFirstRep().getCode());

        String other = "T16-" + uniq();
        Observation ok = obs(pat, ObservationTest.LOINC, other);
        ok.setId(id);
        Resp r = put("/Observation/" + id, ok);
        assertEquals(200, r.status(), r.body());
        assertTrue(lookups(other) >= 1, "updates must be validated too");
    }

    @Test
    void serverErrorFailsOpen() {
        long before = lookups(SERVER_ERROR);
        Resp r = post("Observation", obs(EncounterTest.newPatient(), ObservationTest.LOINC, SERVER_ERROR));
        assertEquals(201, r.status(), r.body());
        assertTrue(lookups(SERVER_ERROR) > before, "the terminology server must be asked");
    }

    @Test
    void unreachableServerFailsOpen() {
        Resp r = post("Observation", obs(EncounterTest.newPatient(), ObservationTest.LOINC, DROP));
        assertEquals(201, r.status(), r.body());
        String id = r.as(Observation.class).getIdElement().getIdPart();
        assertEquals(DROP, read("Observation", id, Observation.class).getCode().getCodingFirstRep().getCode());
    }

    @Test
    void otherSystemsAreNotValidated() {
        long before = lookups(INVALID);
        String pat = EncounterTest.newPatient();
        String id = create("Observation", obs(pat, "http://snomed.info/sct", INVALID));
        Observation o = obs(pat, "http://snomed.info/sct", INVALID);
        o.setId(id);
        assertEquals(200, put("/Observation/" + id, o).status());
        assertEquals(before, lookups(INVALID), "non-LOINC codings must not be sent to the terminology server");
    }
}
