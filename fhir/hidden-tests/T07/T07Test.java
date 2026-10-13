package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

/** T07: X-Request-Id on every response, echoed or generated, and in error diagnostics. */
class T07Test {

    static final String HEADER = "X-Request-Id";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** Sends a request; {@code requestId} null means no X-Request-Id header. */
    static Resp send(String method, String path, String body, String requestId) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .header("Accept", FHIR_JSON)
                .timeout(Duration.ofSeconds(30));
        if (body != null) b.header("Content-Type", FHIR_JSON);
        if (requestId != null) b.header(HEADER, requestId);
        try {
            HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Resp(r.statusCode(), r);
        } catch (Exception e) {
            throw new AssertionError("HTTP call failed: " + e, e);
        }
    }

    record Call(String method, String path, String body) {}

    static List<Call> successfulCalls() {
        String fam = "Rid" + uniq();
        String id = create("Patient", PatientTest.patient(fam));
        Patient upd = PatientTest.patient(fam + "u");
        upd.setId(id);
        return List.of(
                new Call("GET", "/metadata", null),
                new Call("POST", "/Patient", json(PatientTest.patient(fam + "c"))),
                new Call("GET", "/Patient/" + id, null),
                new Call("PUT", "/Patient/" + id, json(upd)),
                new Call("GET", "/Patient?family=" + fam, null));
    }

    static List<Call> failingCalls() {
        return List.of(
                new Call("GET", "/Patient/987654321", null),                                   // 404 not-found
                new Call("GET", "/Patient?shoeSize=42", null),                                 // 400 not-supported
                new Call("GET", "/Patient?birthdate=yesterday", null),                         // 400 invalid
                new Call("POST", "/Patient", "{broken"),                                       // 400 structure
                new Call("POST", "/Patient", "{\"resourceType\":\"Patient\",\"name\":[{\"given\":[\"X\"]}]}"), // 422
                new Call("GET", "/Medication/1", null),                                        // 404 unknown type
                new Call("DELETE", "/Encounter/987654321", null));                             // unsupported method
    }

    static void assertUuid(String value) {
        assertNotNull(value, HEADER + " missing");
        assertTrue(value.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "generated request id must be a UUID, was " + value);
    }

    static void assertDiagnosticsContain(Resp r, String requestId, Call c) {
        assertTrue(r.status() >= 400, () -> c + " should fail but returned " + r.status());
        OperationOutcome oo = r.as(OperationOutcome.class);
        assertFalse(oo.getIssue().isEmpty(), c + ": OperationOutcome without issue");
        String diag = oo.getIssueFirstRep().getDiagnostics();
        assertTrue(diag != null && diag.contains(requestId),
                () -> c + ": first issue diagnostics must contain request id " + requestId + " but was " + diag);
    }

    @Test
    void providedIdIsEchoedOnSuccess() {
        for (Call c : successfulCalls()) {
            String rid = "trace-" + uniq() + "-Ab_9";
            Resp r = send(c.method(), c.path(), c.body(), rid);
            assertTrue(r.status() < 300, () -> c + " failed: " + r.body());
            assertEquals(rid, r.header(HEADER), c.toString());
        }
    }

    @Test
    void generatedUuidWhenAbsent() {
        for (Call c : successfulCalls()) {
            Resp r = send(c.method(), c.path(), c.body(), null);
            assertTrue(r.status() < 300, () -> c + " failed: " + r.body());
            assertUuid(r.header(HEADER));
        }
        String first = send("GET", "/metadata", null, null).header(HEADER);
        String second = send("GET", "/metadata", null, null).header(HEADER);
        assertUuid(first);
        assertUuid(second);
        assertNotEquals(first, second, "each request needs a new id");
    }

    @Test
    void errorsEchoProvidedIdInHeaderAndDiagnostics() {
        for (Call c : failingCalls()) {
            String rid = "err-" + uniq();
            Resp r = send(c.method(), c.path(), c.body(), rid);
            assertEquals(rid, r.header(HEADER), c.toString());
            assertDiagnosticsContain(r, rid, c);
        }
    }

    @Test
    void errorsCarryGeneratedIdInHeaderAndDiagnostics() {
        for (Call c : failingCalls()) {
            Resp r = send(c.method(), c.path(), c.body(), null);
            String rid = r.header(HEADER);
            assertUuid(rid);
            assertDiagnosticsContain(r, rid, c);
        }
    }

    @Test
    void generatedIdIsAValidUuid() {
        String rid = send("GET", "/metadata", null, null).header(HEADER);
        assertUuid(rid);
        assertEquals(rid.toLowerCase(), UUID.fromString(rid).toString());
    }
}
