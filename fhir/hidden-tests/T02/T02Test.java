package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.Test;

/** T02: Observation search parameter `date` on the UTC date part of effectiveDateTime. */
class T02Test {

    static String obs(String pat, String code, String effective) {
        Observation o = ObservationTest.observation(pat, code, "vital-signs");
        if (effective != null) o.setEffective(new DateTimeType(effective));
        return create("Observation", o);
    }

    /** Fixture: a 03-01, b and c on 03-02 (UTC; c is given with a +02:00 offset), d without date, e 03-04. */
    record Data(String pat, String a, String b, String c, String d, String e) {}

    static Data data() {
        String pat = EncounterTest.newPatient();
        String a = obs(pat, "8867-4", "2030-03-01T10:00:00Z");
        String b = obs(pat, "8867-4", "2030-03-02T23:30:00Z");
        String c = obs(pat, "2339-0", "2030-03-03T01:15:00+02:00");
        String d = obs(pat, "8867-4", null);
        String e = obs(pat, "8867-4", "2030-03-04T00:00:00Z");
        return new Data(pat, a, b, c, d, e);
    }

    static List<String> find(Data x, String... params) {
        String[] all = new String[params.length + 1];
        all[0] = "patient=" + x.pat;
        System.arraycopy(params, 0, all, 1, params.length);
        return ids(search("Observation", all));
    }

    @Test
    void prefixesCompareUtcDatePart() {
        Data x = data();
        assertEquals(List.of(x.b, x.c), find(x, "date=2030-03-02"));
        assertEquals(List.of(x.b, x.c), find(x, "date=eq2030-03-02"));
        assertEquals(List.of(x.a, x.e), find(x, "date=ne2030-03-02"));
        assertEquals(List.of(x.a), find(x, "date=lt2030-03-02"));
        assertEquals(List.of(x.a, x.b, x.c), find(x, "date=le2030-03-02"));
        assertEquals(List.of(x.e), find(x, "date=gt2030-03-02"));
        assertEquals(List.of(x.b, x.c, x.e), find(x, "date=ge2030-03-02"));
    }

    @Test
    void observationsWithoutEffectiveNeverMatch() {
        Data x = data();
        assertEquals(List.of(x.a, x.b, x.c, x.e), find(x, "date=ge1900-01-01"));
        assertFalse(find(x, "date=ne2099-01-01").contains(x.d));
    }

    @Test
    void combinesWithPatientAndCode() {
        Data x = data();
        assertEquals(List.of(x.b), find(x, "code=8867-4", "date=2030-03-02"));
        assertEquals(List.of(x.c), find(x, "code=" + ObservationTest.LOINC + "|2339-0", "date=2030-03-02"));
        assertEquals(List.of(x.a, x.b, x.c), find(x, "date=ge2030-03-01", "date=lt2030-03-04"));
    }

    @Test
    void worksAlone() {
        Data x = data();
        List<String> found = ids(search("Observation", "date=2030-03-02"));
        assertTrue(found.containsAll(List.of(x.b, x.c)), found.toString());
        assertFalse(found.contains(x.a) || found.contains(x.d) || found.contains(x.e), found.toString());

        List<String> byCode = ids(search("Observation", "code=2339-0", "date=2030-03-02"));
        assertTrue(byCode.contains(x.c), byCode.toString());
        assertFalse(byCode.contains(x.b), byCode.toString());
    }

    @Test
    void malformedDateIs400Invalid() {
        assertOutcome(get("/Observation?date=yesterday"), 400, "invalid");
        assertOutcome(get("/Observation?date=03%2F02%2F2030"), 400, "invalid");
    }

    @Test
    void capabilityStatementListsDate() {
        CapabilityStatement cs = get("/metadata").as(CapabilityStatement.class);
        Set<String> params = cs.getRestFirstRep().getResource().stream()
                .filter(r -> r.getType().equals("Observation"))
                .flatMap(r -> r.getSearchParam().stream())
                .map(p -> p.getName() + ":" + p.getType().toCode())
                .collect(Collectors.toSet());
        assertTrue(params.contains("date:date"), params.toString());
        assertTrue(params.containsAll(Set.of("patient:reference", "code:token", "category:token")), params.toString());
    }
}
