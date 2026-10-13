package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

class EncounterTest {

    static final String ACT_CODE = "http://terminology.hl7.org/CodeSystem/v3-ActCode";

    static String newPatient() {
        return create("Patient", PatientTest.patient("EncPat" + uniq()));
    }

    static String newPractitioner() {
        return create("Practitioner", PractitionerTest.practitioner("EncDoc" + uniq()));
    }

    static Encounter encounter(String patientId) {
        Encounter e = new Encounter();
        e.setStatus(Encounter.EncounterStatus.INPROGRESS);
        e.setClass_(new Coding(ACT_CODE, "AMB", null));
        e.setSubject(new Reference("Patient/" + patientId));
        return e;
    }

    @Test
    void createReadRoundTrip() {
        String pat = newPatient();
        String doc = newPractitioner();
        Encounter e = encounter(pat);
        e.addParticipant().setIndividual(new Reference("Practitioner/" + doc));
        e.setPeriod(new Period()
                .setStartElement(new DateTimeType("2030-01-10T08:00:00Z"))
                .setEndElement(new DateTimeType("2030-01-10T09:30:00Z")));
        String id = create("Encounter", e);

        Encounter r = read("Encounter", id, Encounter.class);
        assertEquals(Encounter.EncounterStatus.INPROGRESS, r.getStatus());
        assertEquals(ACT_CODE, r.getClass_().getSystem());
        assertEquals("AMB", r.getClass_().getCode());
        assertEquals("Patient/" + pat, r.getSubject().getReference());
        assertEquals("Practitioner/" + doc, r.getParticipantFirstRep().getIndividual().getReference());
        assertEquals(new DateTimeType("2030-01-10T08:00:00Z").getValue(), r.getPeriod().getStart());
        assertEquals(new DateTimeType("2030-01-10T09:30:00Z").getValue(), r.getPeriod().getEnd());
        assertEquals("1", r.getMeta().getVersionId());
    }

    @Test
    void r4_statusRequired() {
        Encounter e = encounter(newPatient());
        e.setStatus(null);
        assertOutcome(post("Encounter", e), 422, "required");
    }

    @Test
    void r4_subjectRequired() {
        Encounter e = encounter(newPatient());
        e.setSubject(null);
        assertOutcome(post("Encounter", e), 422, "required");
    }

    @Test
    void r5_unknownPatientIs422NotFound() {
        assertOutcome(post("Encounter", encounter("987654321")), 422, "not-found");
    }

    @Test
    void r5_unknownPractitionerIs422NotFound() {
        Encounter e = encounter(newPatient());
        e.addParticipant().setIndividual(new Reference("Practitioner/987654321"));
        assertOutcome(post("Encounter", e), 422, "not-found");
    }

    @Test
    void r6_periodEndBeforeStartIs422BusinessRule() {
        Encounter e = encounter(newPatient());
        e.setPeriod(new Period()
                .setStartElement(new DateTimeType("2030-01-10T10:00:00Z"))
                .setEndElement(new DateTimeType("2030-01-10T09:00:00Z")));
        assertOutcome(post("Encounter", e), 422, "business-rule");
    }

    @Test
    void updateChangesStatusAndVersion() {
        String pat = newPatient();
        String id = create("Encounter", encounter(pat));
        Encounter e = encounter(pat);
        e.setId(id);
        e.setStatus(Encounter.EncounterStatus.FINISHED);
        Resp resp = put("/Encounter/" + id, e);
        assertEquals(200, resp.status(), resp.body());
        Encounter r = read("Encounter", id, Encounter.class);
        assertEquals(Encounter.EncounterStatus.FINISHED, r.getStatus());
        assertEquals("2", r.getMeta().getVersionId());
    }

    @Test
    void searchByPatientAcceptsBareIdAndTypedReference() {
        String pat = newPatient();
        String a = create("Encounter", encounter(pat));
        String b = create("Encounter", encounter(pat));
        create("Encounter", encounter(newPatient()));
        assertEquals(List.of(a, b), ids(search("Encounter", "patient=" + pat)));
        assertEquals(List.of(a, b), ids(search("Encounter", "patient=Patient/" + pat)));
    }

    @Test
    void searchByStatus() {
        String pat = newPatient();
        create("Encounter", encounter(pat));
        Encounter planned = encounter(pat);
        planned.setStatus(Encounter.EncounterStatus.PLANNED);
        String p = create("Encounter", planned);
        assertEquals(List.of(p), ids(search("Encounter", "patient=" + pat, "status=planned")));
    }
}
