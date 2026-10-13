package org.archagents.fhir.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

/** Minimal black-box FHIR client + assertions. Knows nothing about the server's internals. */
final class Fhir {

    static final FhirContext CTX = FhirContext.forR4();
    static final String BASE = System.getProperty("base.url", "http://localhost:8080").replaceAll("/+$", "") + "/fhir";
    static final String FHIR_JSON = "application/fhir+json";

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private Fhir() {}

    record Resp(int status, HttpResponse<String> raw) {
        String header(String name) {
            return raw.headers().firstValue(name).orElse(null);
        }

        String body() {
            return raw.body();
        }

        <T extends IBaseResource> T as(Class<T> type) {
            String ct = header("Content-Type");
            assertTrue(ct != null && ct.startsWith(FHIR_JSON), "Content-Type must be " + FHIR_JSON + " but was " + ct);
            IParser strict = CTX.newJsonParser().setParserErrorHandler(new StrictErrorHandler());
            return strict.parseResource(type, raw.body());
        }

        Bundle bundle() {
            assertEquals(200, status, () -> "search failed: " + raw.body());
            Bundle b = as(Bundle.class);
            assertEquals(Bundle.BundleType.SEARCHSET, b.getType());
            return b;
        }
    }

    static String json(IBaseResource r) {
        return CTX.newJsonParser().encodeResourceToString(r);
    }

    static Resp get(String pathAndQuery) {
        return send(HttpRequest.newBuilder(URI.create(BASE + pathAndQuery)).GET().header("Accept", FHIR_JSON));
    }

    static Resp post(String type, IBaseResource r) {
        return postRaw(type, json(r), FHIR_JSON);
    }

    static Resp postRaw(String type, String body, String contentType) {
        return send(HttpRequest.newBuilder(URI.create(BASE + "/" + type))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", contentType)
                .header("Accept", FHIR_JSON));
    }

    static Resp put(String path, IBaseResource r) {
        return putRaw(path, json(r));
    }

    static Resp putRaw(String path, String body) {
        return send(HttpRequest.newBuilder(URI.create(BASE + path))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", FHIR_JSON)
                .header("Accept", FHIR_JSON));
    }

    private static Resp send(HttpRequest.Builder b) {
        try {
            HttpResponse<String> r = HTTP.send(b.timeout(Duration.ofSeconds(30)).build(), HttpResponse.BodyHandlers.ofString());
            return new Resp(r.statusCode(), r);
        } catch (Exception e) {
            throw new AssertionError("HTTP call failed: " + e, e);
        }
    }

    /** POST, assert 201, return the server-assigned id. */
    static String create(String type, IBaseResource r) {
        Resp resp = post(type, r);
        assertEquals(201, resp.status, () -> "create " + type + " failed: " + resp.body());
        Resource created = (Resource) resp.as(r.getClass());
        String id = created.getIdElement().getIdPart();
        assertTrue(id != null && id.matches("\\d+"), "server id must be numeric, was " + id);
        return id;
    }

    static <T extends IBaseResource> T read(String type, String id, Class<T> cls) {
        Resp resp = get("/" + type + "/" + id);
        assertEquals(200, resp.status, () -> "read failed: " + resp.body());
        return resp.as(cls);
    }

    static Bundle search(String type, String... params) {
        String q = List.of(params).stream().map(p -> {
            int eq = p.indexOf('=');
            return p.substring(0, eq) + "=" + URLEncoder.encode(p.substring(eq + 1), StandardCharsets.UTF_8);
        }).collect(Collectors.joining("&"));
        return get("/" + type + (q.isEmpty() ? "" : "?" + q)).bundle();
    }

    static List<String> ids(Bundle b) {
        return b.getEntry().stream().map(e -> e.getResource().getIdElement().getIdPart()).toList();
    }

    static void assertOutcome(Resp resp, int status, String issueCode) {
        assertEquals(status, resp.status, () -> "expected HTTP " + status + " but got " + resp.status + ": " + resp.body());
        OperationOutcome oo = resp.as(OperationOutcome.class);
        assertFalse(oo.getIssue().isEmpty(), "OperationOutcome must have an issue");
        boolean match = oo.getIssue().stream().anyMatch(i ->
                i.getSeverity() == OperationOutcome.IssueSeverity.ERROR && issueCode.equals(i.getCode().toCode()));
        if (!match) {
            fail("expected an error issue with code '" + issueCode + "' but got: " + resp.body());
        }
    }

    static String uniq() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
