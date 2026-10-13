package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

/** T23: every successful create/update is recorded as an AuditEvent; read + search by entity; read-only. */
class T23Test {

    static final String AUDIT_TYPE_SYSTEM = "http://terminology.hl7.org/CodeSystem/audit-event-type";

    static List<AuditEvent> audits(String reference) {
        Bundle b = search("AuditEvent", "entity=" + reference);
        return b.getEntry().stream().map(e -> (AuditEvent) e.getResource()).toList();
    }

    /** Waits up to 5 s for at least {@code n} AuditEvents for the reference, then re-reads after a short pause. */
    static List<AuditEvent> awaitAudits(String reference, int n) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (audits(reference).size() < n && System.currentTimeMillis() < deadline) Thread.sleep(100);
        Thread.sleep(300);
        return audits(reference);
    }

    static String what(AuditEvent a) {
        return a.getEntityFirstRep().getWhat().getReference();
    }

    static void assertAudit(AuditEvent a, String action, String reference, String vid) {
        assertEquals(AUDIT_TYPE_SYSTEM, a.getType().getSystem());
        assertEquals("rest", a.getType().getCode());
        assertNotNull(a.getAction(), "action missing");
        assertEquals(action, a.getAction().toCode());
        assertNotNull(a.getRecorded(), "recorded missing");
        assertNotNull(a.getOutcome(), "outcome missing");
        assertEquals("0", a.getOutcome().toCode());
        assertEquals(reference + "/_history/" + vid, what(a));
    }

    /** Actions and entity references of the AuditEvents, in search order (id ascending). */
    static List<String> summary(List<AuditEvent> list) {
        return list.stream().map(a -> (a.getAction() == null ? "?" : a.getAction().toCode()) + " " + what(a)).toList();
    }

    @Test
    void createAndUpdatesAreAudited() throws Exception {
        String fam = "Aud" + uniq();
        String id = create("Patient", PatientTest.patient(fam));
        for (int i = 2; i <= 3; i++) {
            Patient p = PatientTest.patient(fam + i);
            p.setId(id);
            assertEquals(200, put("/Patient/" + id, p).status());
        }
        String ref = "Patient/" + id;
        List<AuditEvent> got = awaitAudits(ref, 3);
        assertEquals(List.of("C " + ref + "/_history/1", "U " + ref + "/_history/2", "U " + ref + "/_history/3"), summary(got));
        assertAudit(got.get(0), "C", ref, "1");
        assertAudit(got.get(1), "U", ref, "2");
        assertAudit(got.get(2), "U", ref, "3");
    }

    @Test
    void everyResourceTypeIsAudited() throws Exception {
        String pat = EncounterTest.newPatient();
        String doc = EncounterTest.newPractitioner();
        String enc = create("Encounter", EncounterTest.encounter(pat));
        String obs = create("Observation", ObservationTest.observation(pat, "8867-4", "vital-signs"));
        Appointment a = AppointmentTest.appointment(pat, doc, "2034-08-01T10:00:00Z", "2034-08-01T10:30:00Z",
                Appointment.AppointmentStatus.PROPOSED);
        String app = create("Appointment", a);
        for (String ref : List.of("Patient/" + pat, "Practitioner/" + doc, "Encounter/" + enc, "Observation/" + obs,
                "Appointment/" + app)) {
            List<AuditEvent> got = awaitAudits(ref, 1);
            assertEquals(1, got.size(), () -> ref + ": " + summary(got));
            assertAudit(got.get(0), "C", ref, "1");
        }
        Encounter e = EncounterTest.encounter(pat);
        e.setId(enc);
        e.setStatus(Encounter.EncounterStatus.PLANNED);
        assertEquals(200, put("/Encounter/" + enc, e).status());
        List<AuditEvent> encAudits = awaitAudits("Encounter/" + enc, 2);
        assertEquals(2, encAudits.size(), () -> summary(encAudits).toString());
        assertAudit(encAudits.get(1), "U", "Encounter/" + enc, "2");
    }

    @Test
    void auditEventCanBeReadById() throws Exception {
        String id = create("Patient", PatientTest.patient("AudRead" + uniq()));
        List<AuditEvent> got = awaitAudits("Patient/" + id, 1);
        assertEquals(1, got.size());
        String auditId = got.get(0).getIdElement().getIdPart();
        AuditEvent read = read("AuditEvent", auditId, AuditEvent.class);
        assertEquals(auditId, read.getIdElement().getIdPart());
        assertAudit(read, "C", "Patient/" + id, "1");
    }

    @Test
    void entitySearchMatchesOnlyThatResource() throws Exception {
        String a = create("Patient", PatientTest.patient("AudA" + uniq()));
        String b = create("Patient", PatientTest.patient("AudB" + uniq()));
        String doc = create("Practitioner", PractitionerTest.practitioner("AudDoc" + uniq()));
        List<AuditEvent> got = awaitAudits("Patient/" + a, 1);
        assertEquals(List.of("C Patient/" + a + "/_history/1"), summary(got));
        assertEquals(List.of("C Patient/" + b + "/_history/1"), summary(awaitAudits("Patient/" + b, 1)));
        assertEquals(List.of("C Practitioner/" + doc + "/_history/1"), summary(awaitAudits("Practitioner/" + doc, 1)));
    }

    @Test
    void failedRequestsAreNotAudited() throws Exception {
        String fam = "AudFail" + uniq();
        String id = create("Patient", PatientTest.patient(fam));
        assertEquals(1, awaitAudits("Patient/" + id, 1).size());

        Patient future = PatientTest.patient(fam);
        future.setId(id);
        future.setBirthDateElement(new DateType(LocalDate.now().plusYears(1).toString()));
        assertOutcome(put("/Patient/" + id, future), 422, "business-rule");
        Patient noName = new Patient();
        noName.addName().addGiven("OnlyGiven");
        assertOutcome(post("Patient", noName), 422, "required");
        Patient futureNew = PatientTest.patient(fam + "n");
        futureNew.setBirthDateElement(new DateType(LocalDate.now().plusYears(1).toString()));
        assertOutcome(post("Patient", futureNew), 422, "business-rule");
        assertOutcome(post("Observation", ObservationTest.observation("987654321", "8867-4", "vital-signs")), 422, "not-found");
        Patient ghost = PatientTest.patient(fam);
        ghost.setId("987654321");
        assertOutcome(put("/Patient/987654321", ghost), 404, "not-found");
        String next = create("Patient", PatientTest.patient(fam + "next"));
        assertEquals(1, awaitAudits("Patient/" + next, 1).size());

        assertEquals(List.of("C Patient/" + id + "/_history/1"), summary(audits("Patient/" + id)), "a failed update must not be audited");
        assertEquals(List.of(), summary(audits("Patient/987654321")));
        // ids handed out between the two successful creates may belong to failed creates: none may be audited
        for (long k = Long.parseLong(id) + 1; k < Long.parseLong(next) && k < Long.parseLong(id) + 200; k++) {
            assertEquals(List.of(), summary(audits("Patient/" + k)), "failed create audited for Patient/" + k);
            assertEquals(List.of(), summary(audits("Observation/" + k)), "failed create audited for Observation/" + k);
        }
    }

    @Test
    void auditEventsAreReadOnly() throws Exception {
        String id = create("Patient", PatientTest.patient("AudRo" + uniq()));
        AuditEvent existing = awaitAudits("Patient/" + id, 1).get(0);
        String auditId = existing.getIdElement().getIdPart();

        assertOutcome(postRaw("AuditEvent", json(existing.copy().setId((String) null)), FHIR_JSON), 405, "not-supported");
        AuditEvent changed = existing.copy();
        changed.setId(auditId);
        changed.setOutcome(AuditEvent.AuditEventOutcome._4);
        assertOutcome(put("/AuditEvent/" + auditId, changed), 405, "not-supported");
        assertEquals("0", read("AuditEvent", auditId, AuditEvent.class).getOutcome().toCode());
    }

    @Test
    void capabilityStatementListsAuditEvent() {
        CapabilityStatement cs = get("/metadata").as(CapabilityStatement.class);
        Set<String> interactions = cs.getRestFirstRep().getResource().stream()
                .filter(r -> r.getType().equals("AuditEvent"))
                .flatMap(r -> r.getInteraction().stream())
                .map(i -> i.getCode().toCode())
                .collect(Collectors.toSet());
        assertTrue(interactions.containsAll(Set.of("read", "search-type")), "AuditEvent interactions: " + interactions);
    }
}
