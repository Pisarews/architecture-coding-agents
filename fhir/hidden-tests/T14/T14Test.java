package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

/** T14: Encounter search parameters `practitioner` (reference) and `date` (UTC date within [period.start, period.end]). */
class T14Test {

    static String encounter(String pat, String doc, Encounter.EncounterStatus status, String start, String end) {
        Encounter e = EncounterTest.encounter(pat);
        e.setStatus(status);
        if (doc != null) e.addParticipant().setIndividual(new Reference("Practitioner/" + doc));
        Period p = new Period().setStartElement(new DateTimeType(start));
        if (end != null) p.setEndElement(new DateTimeType(end));
        e.setPeriod(p);
        return create("Encounter", e);
    }

    /**
     * e1: doc1, in-progress, 2030-04-01T22:00Z .. 2030-04-03T01:00Z (UTC dates 04-01..04-03).
     * e2: doc1, finished, 2030-04-05T01:30+02:00 .. 2030-04-05T03:00+02:00 (UTC dates 04-04..04-05).
     * e3: doc1, in-progress, 2020-06-10T08:00Z, open end (2020-06-10 .. today).
     * e5: doc2, planned, 2030-04-02T09:00Z .. 2030-04-02T10:00Z.
     * e6: no practitioner, in-progress, 2030-04-02T09:00Z .. 2030-04-02T10:00Z.
     */
    record Data(String pat, String doc1, String doc2, String e1, String e2, String e3, String e5, String e6) {}

    static Data data() {
        String pat = EncounterTest.newPatient();
        String doc1 = EncounterTest.newPractitioner();
        String doc2 = EncounterTest.newPractitioner();
        String e1 = encounter(pat, doc1, Encounter.EncounterStatus.INPROGRESS, "2030-04-01T22:00:00Z", "2030-04-03T01:00:00Z");
        String e2 = encounter(pat, doc1, Encounter.EncounterStatus.FINISHED, "2030-04-05T01:30:00+02:00", "2030-04-05T03:00:00+02:00");
        String e3 = encounter(pat, doc1, Encounter.EncounterStatus.INPROGRESS, "2020-06-10T08:00:00Z", null);
        String e5 = encounter(pat, doc2, Encounter.EncounterStatus.PLANNED, "2030-04-02T09:00:00Z", "2030-04-02T10:00:00Z");
        String e6 = encounter(pat, null, Encounter.EncounterStatus.INPROGRESS, "2030-04-02T09:00:00Z", "2030-04-02T10:00:00Z");
        return new Data(pat, doc1, doc2, e1, e2, e3, e5, e6);
    }

    static List<String> find(String... params) {
        return ids(search("Encounter", params));
    }

    @Test
    void practitionerAcceptsBareIdAndTypedReference() {
        Data x = data();
        assertEquals(List.of(x.e1, x.e2, x.e3), find("practitioner=" + x.doc1));
        assertEquals(List.of(x.e1, x.e2, x.e3), find("practitioner=Practitioner/" + x.doc1));
        assertEquals(List.of(x.e5), find("practitioner=" + x.doc2));
        assertEquals(List.of(), find("practitioner=" + EncounterTest.newPractitioner()));
    }

    @Test
    void dateMatchesUtcDatesWithinPeriod() {
        Data x = data();
        String d = "practitioner=" + x.doc1;
        assertEquals(List.of(), find(d, "date=2030-03-31"));
        assertEquals(List.of(x.e1), find(d, "date=2030-04-01"));
        assertEquals(List.of(x.e1), find(d, "date=2030-04-02"));
        assertEquals(List.of(x.e1), find(d, "date=eq2030-04-03"));
        assertEquals(List.of(x.e2), find(d, "date=2030-04-04"));
        assertEquals(List.of(x.e2), find(d, "date=eq2030-04-05"));
        assertEquals(List.of(), find(d, "date=2030-04-06"));
    }

    @Test
    void openEndMeansUntilNow() {
        Data x = data();
        String d = "practitioner=" + x.doc1;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        assertEquals(List.of(), find(d, "date=2020-06-09"));
        assertEquals(List.of(x.e3), find(d, "date=2020-06-10"));
        assertEquals(List.of(x.e3), find(d, "date=2023-01-15"));
        assertEquals(List.of(x.e3), find(d, "date=" + today));
        assertEquals(List.of(), find(d, "date=" + today.plusDays(30)));
    }

    @Test
    void dateWorksAlone() {
        Data x = data();
        List<String> found = find("date=2030-04-04");
        assertTrue(found.contains(x.e2), found.toString());
        assertFalse(found.contains(x.e1) || found.contains(x.e3) || found.contains(x.e5) || found.contains(x.e6), found.toString());
    }

    @Test
    void combinesWithPatientAndStatus() {
        Data x = data();
        assertEquals(List.of(x.e1, x.e5, x.e6), find("patient=" + x.pat, "date=2030-04-02"));
        assertEquals(List.of(x.e1, x.e6), find("patient=Patient/" + x.pat, "status=in-progress", "date=2030-04-02"));
        assertEquals(List.of(x.e5), find("patient=" + x.pat, "status=planned", "date=2030-04-02"));
        assertEquals(List.of(x.e2), find("patient=" + x.pat, "practitioner=" + x.doc1, "status=finished"));
        assertEquals(List.of(x.e1, x.e3), find("practitioner=" + x.doc1, "status=in-progress"));
        assertEquals(List.of(x.e1), find("patient=" + x.pat, "practitioner=" + x.doc1, "status=in-progress", "date=2030-04-02"));
        assertEquals(List.of(), find("patient=" + EncounterTest.newPatient(), "practitioner=" + x.doc1));
    }

    @Test
    void capabilityStatementListsNewParameters() {
        CapabilityStatement cs = get("/metadata").as(CapabilityStatement.class);
        Set<String> params = cs.getRestFirstRep().getResource().stream()
                .filter(r -> r.getType().equals("Encounter"))
                .flatMap(r -> r.getSearchParam().stream())
                .map(p -> p.getName() + ":" + p.getType().toCode())
                .collect(Collectors.toSet());
        assertTrue(params.containsAll(Set.of("practitioner:reference", "date:date")), params.toString());
        assertTrue(params.containsAll(Set.of("patient:reference", "status:token")), params.toString());
    }
}
