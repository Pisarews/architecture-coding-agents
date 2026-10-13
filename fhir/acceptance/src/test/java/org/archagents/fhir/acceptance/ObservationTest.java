package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

class ObservationTest {

    static final String LOINC = "http://loinc.org";
    static final String CAT = "http://terminology.hl7.org/CodeSystem/observation-category";
    static final String UCUM = "http://unitsofmeasure.org";

    static Observation observation(String patientId, String loinc, String category) {
        Observation o = new Observation();
        o.setStatus(Observation.ObservationStatus.FINAL);
        o.addCategory(new CodeableConcept(new Coding(CAT, category, null)));
        o.setCode(new CodeableConcept(new Coding(LOINC, loinc, "test")));
        o.setSubject(new Reference("Patient/" + patientId));
        return o;
    }

    @Test
    void createReadRoundTrip() {
        String pat = EncounterTest.newPatient();
        String enc = create("Encounter", EncounterTest.encounter(pat));
        Observation o = observation(pat, "29463-7", "vital-signs");
        o.getCode().getCodingFirstRep().setDisplay("Body weight");
        o.setEncounter(new Reference("Encounter/" + enc));
        o.setEffective(new DateTimeType("2030-02-01T07:15:00Z"));
        o.setValue(new Quantity().setValue(new BigDecimal("72.5")).setUnit("kg").setSystem(UCUM).setCode("kg"));
        String id = create("Observation", o);

        Observation r = read("Observation", id, Observation.class);
        assertEquals(Observation.ObservationStatus.FINAL, r.getStatus());
        assertEquals(CAT, r.getCategoryFirstRep().getCodingFirstRep().getSystem());
        assertEquals("vital-signs", r.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertEquals(LOINC, r.getCode().getCodingFirstRep().getSystem());
        assertEquals("29463-7", r.getCode().getCodingFirstRep().getCode());
        assertEquals("Body weight", r.getCode().getCodingFirstRep().getDisplay());
        assertEquals("Patient/" + pat, r.getSubject().getReference());
        assertEquals("Encounter/" + enc, r.getEncounter().getReference());
        assertEquals(new DateTimeType("2030-02-01T07:15:00Z").getValue(), r.getEffectiveDateTimeType().getValue());
        Quantity q = r.getValueQuantity();
        assertEquals(0, new BigDecimal("72.5").compareTo(q.getValue()));
        assertEquals("kg", q.getUnit());
        assertEquals(UCUM, q.getSystem());
        assertEquals("kg", q.getCode());
    }

    @Test
    void r7_statusRequired() {
        Observation o = observation(EncounterTest.newPatient(), "8867-4", "vital-signs");
        o.setStatus(null);
        assertOutcome(post("Observation", o), 422, "required");
    }

    @Test
    void r7_codeRequired() {
        Observation o = observation(EncounterTest.newPatient(), "8867-4", "vital-signs");
        o.setCode(new CodeableConcept().setText("no coding"));
        assertOutcome(post("Observation", o), 422, "required");
    }

    @Test
    void r7_subjectRequired() {
        Observation o = observation(EncounterTest.newPatient(), "8867-4", "vital-signs");
        o.setSubject(null);
        assertOutcome(post("Observation", o), 422, "required");
    }

    @Test
    void r8_unknownSubjectIs422NotFound() {
        assertOutcome(post("Observation", observation("987654321", "8867-4", "vital-signs")), 422, "not-found");
    }

    @Test
    void r8_unknownEncounterIs422NotFound() {
        Observation o = observation(EncounterTest.newPatient(), "8867-4", "vital-signs");
        o.setEncounter(new Reference("Encounter/987654321"));
        assertOutcome(post("Observation", o), 422, "not-found");
    }

    @Test
    void r9_quantityWithoutValueIs422Required() {
        Observation o = observation(EncounterTest.newPatient(), "8867-4", "vital-signs");
        o.setValue(new Quantity().setUnit("/min").setSystem(UCUM).setCode("/min"));
        assertOutcome(post("Observation", o), 422, "required");
    }

    @Test
    void updateIncrementsVersion() {
        String pat = EncounterTest.newPatient();
        String id = create("Observation", observation(pat, "8867-4", "vital-signs"));
        Observation o = observation(pat, "8867-4", "vital-signs");
        o.setId(id);
        o.setStatus(Observation.ObservationStatus.AMENDED);
        Resp resp = put("/Observation/" + id, o);
        assertEquals(200, resp.status(), resp.body());
        assertEquals("W/\"2\"", resp.header("ETag"));
        assertEquals(Observation.ObservationStatus.AMENDED, read("Observation", id, Observation.class).getStatus());
    }

    @Test
    void searchByPatientCodeAndCategory() {
        String pat = EncounterTest.newPatient();
        String hr = create("Observation", observation(pat, "8867-4", "vital-signs"));
        String wt = create("Observation", observation(pat, "29463-7", "vital-signs"));
        String lab = create("Observation", observation(pat, "2339-0", "laboratory"));
        create("Observation", observation(EncounterTest.newPatient(), "8867-4", "vital-signs"));

        assertEquals(List.of(hr, wt, lab), ids(search("Observation", "patient=" + pat)));
        assertEquals(List.of(hr), ids(search("Observation", "patient=" + pat, "code=8867-4")));
        assertEquals(List.of(hr), ids(search("Observation", "patient=Patient/" + pat, "code=" + LOINC + "|8867-4")));
        assertEquals(List.of(), ids(search("Observation", "patient=" + pat, "code=http://snomed.info/sct|8867-4")));
        assertEquals(List.of(hr, wt), ids(search("Observation", "patient=" + pat, "category=vital-signs")));
        assertEquals(List.of(lab), ids(search("Observation", "patient=" + pat, "category=laboratory")));
    }
}
