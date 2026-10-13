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
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * T24: every Patient create/update is mirrored with PUT {fhir.mirror.base-url}/Patient/{id}, carrying an extra
 * identifier urn:archagents:local-id|{id} that the stored patient never gets.
 * The app is started with --fhir.mirror.base-url=http://localhost:{mock.port}/mirror.
 * The mock answers 500 when the body contains FAIL_MARKER, closes the connection when it contains DROP_MARKER,
 * otherwise 200. Requests to any other path (other tasks' outbound calls) get 503.
 */
class T24Test {

    static final String PREFIX = "/mirror/Patient/";
    static final String LOCAL_ID = "urn:archagents:local-id";
    static final String FAIL_MARKER = "MirrorFail";
    static final String DROP_MARKER = "MirrorDrop";

    record Req(String method, String path, String body) {}

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
            String path = ex.getRequestURI().getPath();
            REQUESTS.add(new Req(ex.getRequestMethod(), path, body));
            int status;
            if (!path.startsWith("/mirror/")) {
                status = 503;
            } else if (body.contains(DROP_MARKER)) {
                ex.close();
                return;
            } else {
                status = body.contains(FAIL_MARKER) ? 500 : 200;
            }
            byte[] out = (status == 200 ? body : "{}").getBytes(StandardCharsets.UTF_8);
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

    static List<Req> mirrored(String id) {
        return REQUESTS.stream().filter(r -> (PREFIX + id).equals(r.path())).toList();
    }

    /** Waits up to 5 s until at least {@code n} mirror requests for the patient arrived, then a bit longer for duplicates. */
    static List<Req> awaitMirrored(String id, int n) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (mirrored(id).size() < n && System.currentTimeMillis() < deadline) Thread.sleep(50);
        Thread.sleep(1_000);
        return mirrored(id);
    }

    static Patient parse(Req r) {
        return CTX.newJsonParser().parseResource(Patient.class, r.body());
    }

    static boolean hasLocalId(Patient p) {
        return p.getIdentifier().stream().anyMatch(i -> LOCAL_ID.equals(i.getSystem()));
    }

    static void assertNoTrace(Patient p) {
        assertFalse(hasLocalId(p), () -> "stored/returned patient must not carry " + LOCAL_ID + ": " + json(p));
    }

    static void assertMirrorCopy(Req r, String id, String family, String identifierValue) {
        assertEquals("PUT", r.method());
        Patient sent = parse(r);
        List<Identifier> local = sent.getIdentifier().stream().filter(i -> LOCAL_ID.equals(i.getSystem())).toList();
        assertEquals(1, local.size(), () -> "exactly one " + LOCAL_ID + " identifier expected: " + r.body());
        assertEquals(id, local.get(0).getValue());
        assertEquals(family, sent.getNameFirstRep().getFamily());
        assertTrue(sent.getIdentifier().stream().anyMatch(i -> "urn:sys:mirror".equals(i.getSystem()) && identifierValue.equals(i.getValue())),
                () -> "the patient's own identifiers must be kept: " + r.body());
    }

    static Patient patient(String fam, String value) {
        Patient p = PatientTest.patient(fam, "Mirror");
        p.addIdentifier().setSystem("urn:sys:mirror").setValue(value);
        return p;
    }

    @Test
    void createIsMirroredWithLocalIdentifier() throws Exception {
        String fam = "Mir" + uniq();
        String value = "M-" + uniq();
        Resp resp = post("Patient", patient(fam, value));
        assertEquals(201, resp.status(), resp.body());
        Patient body = resp.as(Patient.class);
        String id = body.getIdElement().getIdPart();
        assertNoTrace(body);

        List<Req> got = awaitMirrored(id, 1);
        assertEquals(1, got.size(), () -> "expected exactly one mirror PUT for patient " + id + ", got " + got);
        assertMirrorCopy(got.get(0), id, fam, value);

        Patient stored = read("Patient", id, Patient.class);
        assertNoTrace(stored);
        assertEquals(1, stored.getIdentifier().size());
        assertEquals(0, search("Patient", "identifier=" + LOCAL_ID + "|" + id).getTotal());
    }

    @Test
    void eachUpdateIsMirrored() throws Exception {
        String fam = "MirUpd" + uniq();
        String value = "MU-" + uniq();
        String id = create("Patient", patient(fam, value));
        assertEquals(1, awaitMirrored(id, 1).size());

        Patient v2 = patient(fam + "b", value);
        v2.setId(id);
        Resp r = put("/Patient/" + id, v2);
        assertEquals(200, r.status(), r.body());
        assertNoTrace(r.as(Patient.class));

        List<Req> got = awaitMirrored(id, 2);
        assertEquals(2, got.size(), () -> "expected one PUT per create/update, got " + got);
        Req update = got.stream().filter(q -> (fam + "b").equals(parse(q).getNameFirstRep().getFamily())).findFirst()
                .orElseThrow(() -> new AssertionError("no mirror PUT with the updated family: " + got));
        assertMirrorCopy(update, id, fam + "b", value);
        assertNoTrace(read("Patient", id, Patient.class));
        assertEquals(1, read("Patient", id, Patient.class).getIdentifier().size());
    }

    @Test
    void mirrorFailureDoesNotFailTheRequest() throws Exception {
        for (String marker : List.of(FAIL_MARKER, DROP_MARKER)) {
            String fam = marker + uniq();
            Resp created = post("Patient", patient(fam, "MF-" + uniq()));
            assertEquals(201, created.status(), () -> marker + ": " + created.body());
            Patient body = created.as(Patient.class);
            assertNoTrace(body);
            String id = body.getIdElement().getIdPart();

            Patient v2 = patient(fam + "b", "MF-" + uniq());
            v2.setId(id);
            Resp updated = put("/Patient/" + id, v2);
            assertEquals(200, updated.status(), () -> marker + ": " + updated.body());
            assertNoTrace(updated.as(Patient.class));
            Patient stored = read("Patient", id, Patient.class);
            assertNoTrace(stored);
            assertEquals(fam + "b", stored.getNameFirstRep().getFamily());
            assertFalse(awaitMirrored(id, 1).isEmpty(), "the mirror must have been attempted");
        }
    }

    @Test
    void otherResourcesAreNotMirrored() throws Exception {
        String doc = create("Practitioner", PractitionerTest.practitioner("MirDoc" + uniq()));
        String pat = EncounterTest.newPatient();
        String obs = create("Observation", ObservationTest.observation(pat, "8867-4", "vital-signs"));
        Thread.sleep(2_000);
        assertTrue(REQUESTS.stream().noneMatch(r -> r.path().startsWith("/mirror/") && !r.path().startsWith(PREFIX)),
                () -> "only Patients are mirrored: " + REQUESTS.stream().map(Req::path).toList());
        assertEquals(List.of(), mirrored(doc), "a Practitioner must not be mirrored");
        assertEquals(List.of(), mirrored(obs), "an Observation must not be mirrored");
    }
}
