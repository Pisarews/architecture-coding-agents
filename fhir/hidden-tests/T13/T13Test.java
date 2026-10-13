package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.Test;

/** T13: an Encounter with status finished needs period.end (422 required), on create and update. */
class T13Test {

    static Encounter encounter(String pat, Encounter.EncounterStatus status, String start, String end) {
        Encounter e = EncounterTest.encounter(pat);
        e.setStatus(status);
        if (start != null || end != null) {
            Period p = new Period();
            if (start != null) p.setStartElement(new DateTimeType(start));
            if (end != null) p.setEndElement(new DateTimeType(end));
            e.setPeriod(p);
        }
        return e;
    }

    @Test
    void finishedWithoutPeriodIsRejected() {
        assertOutcome(post("Encounter", encounter(EncounterTest.newPatient(), Encounter.EncounterStatus.FINISHED, null, null)),
                422, "required");
    }

    @Test
    void finishedWithStartButNoEndIsRejected() {
        assertOutcome(post("Encounter", encounter(EncounterTest.newPatient(), Encounter.EncounterStatus.FINISHED,
                "2030-05-01T08:00:00Z", null)), 422, "required");
    }

    @Test
    void finishedWithEndIsAccepted() {
        String pat = EncounterTest.newPatient();
        String id = create("Encounter", encounter(pat, Encounter.EncounterStatus.FINISHED,
                "2030-05-02T08:00:00Z", "2030-05-02T09:00:00Z"));
        Encounter r = read("Encounter", id, Encounter.class);
        assertEquals(Encounter.EncounterStatus.FINISHED, r.getStatus());
        assertEquals(new DateTimeType("2030-05-02T09:00:00Z").getValue(), r.getPeriod().getEnd());
    }

    @Test
    void updateToFinishedWithoutEndIsRejectedAndChangesNothing() {
        String pat = EncounterTest.newPatient();
        String id = create("Encounter", encounter(pat, Encounter.EncounterStatus.INPROGRESS, "2030-05-03T08:00:00Z", null));
        Encounter e = encounter(pat, Encounter.EncounterStatus.FINISHED, "2030-05-03T08:00:00Z", null);
        e.setId(id);
        assertOutcome(put("/Encounter/" + id, e), 422, "required");
        Encounter r = read("Encounter", id, Encounter.class);
        assertEquals(Encounter.EncounterStatus.INPROGRESS, r.getStatus());
        assertEquals("1", r.getMeta().getVersionId());

        e.getPeriod().setEndElement(new DateTimeType("2030-05-03T10:00:00Z"));
        Resp ok = put("/Encounter/" + id, e);
        assertEquals(200, ok.status(), ok.body());
        assertEquals(Encounter.EncounterStatus.FINISHED, read("Encounter", id, Encounter.class).getStatus());
    }

    @Test
    void removingEndFromFinishedEncounterIsRejected() {
        String pat = EncounterTest.newPatient();
        String id = create("Encounter", encounter(pat, Encounter.EncounterStatus.FINISHED,
                "2030-05-04T08:00:00Z", "2030-05-04T09:00:00Z"));
        Encounter e = encounter(pat, Encounter.EncounterStatus.FINISHED, "2030-05-04T08:00:00Z", null);
        e.setId(id);
        assertOutcome(put("/Encounter/" + id, e), 422, "required");
    }

    @Test
    void otherStatusesNeedNoEnd() {
        String pat = EncounterTest.newPatient();
        for (Encounter.EncounterStatus s : new Encounter.EncounterStatus[] {
                Encounter.EncounterStatus.PLANNED, Encounter.EncounterStatus.INPROGRESS, Encounter.EncounterStatus.CANCELLED}) {
            create("Encounter", encounter(pat, s, null, null));
            String id = create("Encounter", encounter(pat, s, "2030-05-05T08:00:00Z", null));
            Encounter e = encounter(pat, s, "2030-05-05T08:30:00Z", null);
            e.setId(id);
            Resp r = put("/Encounter/" + id, e);
            assertEquals(200, r.status(), () -> s.toCode() + ": " + r.body());
        }
    }
}
