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
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.jupiter.api.Test;

/** T18: _count (1..100) and Bundle.link self/next on every search. */
class T18Test {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** GET an absolute or relative link (relative links resolve against the /fhir/ base). */
    static Bundle follow(String link) {
        URI uri = URI.create(BASE + "/").resolve(link);
        try {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(uri).GET().header("Accept", FHIR_JSON)
                    .timeout(Duration.ofSeconds(30)).build(), HttpResponse.BodyHandlers.ofString());
            return new Resp(r.statusCode(), r).bundle();
        } catch (Exception e) {
            throw new AssertionError("HTTP call failed: " + e, e);
        }
    }

    static String link(Bundle b, String rel) {
        Bundle.BundleLinkComponent l = b.getLink(rel);
        return l == null || !l.hasUrl() ? null : l.getUrl();
    }

    /** Pages through a search with _count and checks page sizes, total, links, order and completeness. */
    static void assertPaging(String type, int count, List<String> expected, String... params) {
        List<String> all = new ArrayList<>(List.of(params));
        all.add("_count=" + count);
        Bundle page = search(type, all.toArray(String[]::new));
        List<String> seen = new ArrayList<>();
        int pages = 0;
        while (true) {
            pages++;
            assertTrue(pages <= expected.size() + 1, "too many pages for " + type + " " + all);
            assertEquals(expected.size(), page.getTotal(), "total must count all matches");
            assertTrue(page.getEntry().size() <= count, "page larger than _count: " + page.getEntry().size());
            assertNotNull(link(page, "self"), "Bundle.link self missing");
            seen.addAll(ids(page));
            String next = link(page, "next");
            if (seen.size() < expected.size()) {
                assertNotNull(next, "next link missing after " + seen + " of " + expected);
            } else {
                assertNull(next, "no next link expected on the last page, got " + next);
                break;
            }
            page = follow(next);
        }
        assertEquals(expected, seen, type + " " + all);
    }

    @Test
    void patientPagesInIdOrderWithoutGapsOrDuplicates() {
        String fam = "Page" + uniq();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) ids.add(create("Patient", PatientTest.patient(fam)));
        assertPaging("Patient", 2, ids, "family=" + fam);
        assertPaging("Patient", 1, ids, "family=" + fam);
        assertPaging("Patient", 5, ids, "family=" + fam);
        assertPaging("Patient", 100, ids, "family=" + fam);
    }

    @Test
    void everyResourceTypeSupportsPaging() {
        String u = uniq();
        List<String> docs = new ArrayList<>();
        for (int i = 0; i < 3; i++) docs.add(create("Practitioner", PractitionerTest.practitioner("PageDoc" + u)));
        assertPaging("Practitioner", 2, docs, "family=PageDoc" + u);

        String pat = EncounterTest.newPatient();
        List<String> encs = new ArrayList<>();
        for (int i = 0; i < 3; i++) encs.add(create("Encounter", EncounterTest.encounter(pat)));
        assertPaging("Encounter", 2, encs, "patient=" + pat);

        List<String> obs = new ArrayList<>();
        for (int i = 0; i < 3; i++) obs.add(create("Observation", ObservationTest.observation(pat, "8867-4", "vital-signs")));
        assertPaging("Observation", 2, obs, "patient=" + pat);

        String doc = EncounterTest.newPractitioner();
        List<String> appts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            appts.add(create("Appointment", AppointmentTest.appointment(pat, doc, "2034-04-0" + (i + 1) + "T10:00:00Z",
                    "2034-04-0" + (i + 1) + "T10:30:00Z", Appointment.AppointmentStatus.PROPOSED)));
        }
        assertPaging("Appointment", 2, appts, "patient=" + pat);
    }

    @Test
    void pagingCombinesWithOtherParameters() {
        String pat = EncounterTest.newPatient();
        List<String> planned = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            create("Encounter", EncounterTest.encounter(pat));
            Encounter e = EncounterTest.encounter(pat);
            e.setStatus(Encounter.EncounterStatus.PLANNED);
            planned.add(create("Encounter", e));
        }
        assertPaging("Encounter", 3, planned, "patient=" + pat, "status=planned");
    }

    @Test
    void countWithoutOtherParameters() {
        create("Patient", PatientTest.patient("PageAny" + uniq()));
        create("Patient", PatientTest.patient("PageAny" + uniq()));
        Bundle b = search("Patient", "_count=1");
        assertEquals(1, b.getEntry().size());
        assertTrue(b.getTotal() >= 2, "total must count all matches");
        assertNotNull(link(b, "next"), "next link expected");
        Bundle second = follow(link(b, "next"));
        assertEquals(1, second.getEntry().size());
        assertTrue(Long.parseLong(ids(second).get(0)) > Long.parseLong(ids(b).get(0)), "pages must be in id order");
    }

    @Test
    void countOutsideRangeIs400Invalid() {
        assertOutcome(get("/Patient?_count=0"), 400, "invalid");
        assertOutcome(get("/Patient?_count=101"), 400, "invalid");
        assertOutcome(get("/Patient?_count=-1"), 400, "invalid");
        assertOutcome(get("/Observation?patient=1&_count=500"), 400, "invalid");
    }

    @Test
    void withoutCountAllMatchesAreReturned() {
        String fam = "NoPage" + uniq();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) ids.add(create("Patient", PatientTest.patient(fam)));
        Bundle b = search("Patient", "family=" + fam);
        assertEquals(3, b.getTotal());
        assertEquals(ids, ids(b));
        assertNull(link(b, "next"));
        assertOutcome(get("/Patient?shoeSize=42"), 400, "not-supported");
    }
}
