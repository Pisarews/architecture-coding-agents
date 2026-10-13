package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * T12: practitioners with a urn:npwz identifier are checked against the licensing registry on create.
 * The app is started with --fhir.registry.base-url=http://localhost:{mock.port}/registry.
 * The mock answers GET /registry/licenses/{value} by the value's prefix:
 * ACT → 200 ACTIVE, SUS → 200 SUSPENDED, ERR → 500, UNA → 503, anything else → 404.
 */
class T12Test {

    static final String PREFIX = "/registry/licenses/";

    record Req(String method, String path) {}

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
            try (InputStream in = ex.getRequestBody()) {
                in.readAllBytes();
            }
            String path = ex.getRequestURI().getPath();
            REQUESTS.add(new Req(ex.getRequestMethod(), path));
            String value = path.startsWith(PREFIX) ? path.substring(PREFIX.length()) : "";
            int status;
            String body;
            if (value.startsWith("ACT")) {
                status = 200;
                body = "{\"status\":\"ACTIVE\"}";
            } else if (value.startsWith("SUS")) {
                status = 200;
                body = "{\"status\":\"SUSPENDED\"}";
            } else if (value.startsWith("ERR")) {
                status = 500;
                body = "{\"error\":\"internal\"}";
            } else if (value.startsWith("UNA")) {
                status = 503;
                body = "{\"error\":\"unavailable\"}";
            } else {
                status = 404;
                body = "{\"error\":\"not found\"}";
            }
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
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

    static Practitioner prac(String fam, String system, String value, Boolean active) {
        Practitioner p = PractitionerTest.practitioner(fam);
        if (system != null) p.addIdentifier().setSystem(system).setValue(value);
        if (active != null) p.setActive(active);
        return p;
    }

    static long lookups(String value) {
        return REQUESTS.stream().filter(r -> "GET".equals(r.method()) && (PREFIX + value).equals(r.path())).count();
    }

    static void assertStoredActive(String id, boolean expected, Resp createResp) {
        assertEquals(expected, createResp.as(Practitioner.class).getActive(), "active in create response");
        assertEquals(expected, read("Practitioner", id, Practitioner.class).getActive(), "active after read");
    }

    @Test
    void activeLicenceIsStoredActive() {
        String u = uniq();
        String value = "ACT" + u;
        Resp resp = post("Practitioner", prac("Reg" + u, "urn:npwz", value, null));
        assertEquals(201, resp.status(), resp.body());
        assertStoredActive(resp.as(Practitioner.class).getIdElement().getIdPart(), true, resp);
        assertTrue(lookups(value) >= 1, "registry must be called with GET " + PREFIX + value);

        String value2 = "ACT" + uniq();
        Resp resp2 = post("Practitioner", prac("Reg" + u + "b", "urn:npwz", value2, false));
        assertEquals(201, resp2.status(), resp2.body());
        assertStoredActive(resp2.as(Practitioner.class).getIdElement().getIdPart(), true, resp2);
    }

    @Test
    void suspendedLicenceIsStoredInactive() {
        String u = uniq();
        String value = "SUS" + u;
        Resp resp = post("Practitioner", prac("Sus" + u, "urn:npwz", value, true));
        assertEquals(201, resp.status(), resp.body());
        assertStoredActive(resp.as(Practitioner.class).getIdElement().getIdPart(), false, resp);
        assertTrue(lookups(value) >= 1, "registry must be called with GET " + PREFIX + value);
    }

    @Test
    void npwzIdentifierAmongOthersIsChecked() {
        String u = uniq();
        Practitioner p = prac("Multi" + u, "urn:other", "X" + u, true);
        p.addIdentifier().setSystem("urn:npwz").setValue("SUS" + u);
        Resp resp = post("Practitioner", p);
        assertEquals(201, resp.status(), resp.body());
        assertStoredActive(resp.as(Practitioner.class).getIdElement().getIdPart(), false, resp);
    }

    @Test
    void unknownLicenceIs422() {
        String u = uniq();
        String fam = "Unk" + u;
        assertOutcome(post("Practitioner", prac(fam, "urn:npwz", "NONE" + u, true)), 422, "business-rule");
        assertTrue(lookups("NONE" + u) >= 1, "registry must be called with GET " + PREFIX + "NONE" + u);
        assertEquals(0, search("Practitioner", "family=" + fam).getTotal());
    }

    @Test
    void registryServerErrorIs503AndNothingStored() {
        String u = uniq();
        String fam = "Err" + u;
        assertOutcome(post("Practitioner", prac(fam, "urn:npwz", "ERR" + u, true)), 503, "transient");
        assertOutcome(post("Practitioner", prac(fam, "urn:npwz", "UNA" + u, true)), 503, "transient");
        assertEquals(0, search("Practitioner", "family=" + fam).getTotal());
    }

    @Test
    void practitionersWithoutNpwzAreNotChecked() {
        String u = uniq();
        String value = "NONE" + u;
        Resp resp = post("Practitioner", prac("NoNpwz" + u, "urn:other", value, false));
        assertEquals(201, resp.status(), resp.body());
        assertStoredActive(resp.as(Practitioner.class).getIdElement().getIdPart(), false, resp);
        Resp plain = post("Practitioner", prac("NoId" + u, null, null, true));
        assertEquals(201, plain.status(), plain.body());
        assertStoredActive(plain.as(Practitioner.class).getIdElement().getIdPart(), true, plain);
        assertEquals(0, lookups(value));
    }

    @Test
    void updatesAreNotChecked() {
        String u = uniq();
        String id = create("Practitioner", prac("UpdReg" + u, "urn:npwz", "ACT" + u, null));
        for (String value : List.of("NONE" + u, "ERR" + u, "SUS" + u)) {
            Practitioner p = prac("UpdReg" + u, "urn:npwz", value, true);
            p.setId(id);
            Resp r = put("/Practitioner/" + id, p);
            assertEquals(200, r.status(), r.body());
            assertEquals(0, lookups(value), "updates must not call the registry");
        }
        Practitioner r = read("Practitioner", id, Practitioner.class);
        assertTrue(r.getActive());
        assertEquals("SUS" + u, r.getIdentifierFirstRep().getValue());
    }
}
