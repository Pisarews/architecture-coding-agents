package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.Test;

/** T01: body-weight observations (LOINC 29463-7) with a valueQuantity must use UCUM kg or g. */
class T01Test {

    static final String BODY_WEIGHT = "29463-7";

    static Observation weight(String pat, String value, String system, String code) {
        Observation o = ObservationTest.observation(pat, BODY_WEIGHT, "vital-signs");
        Quantity q = new Quantity().setValue(new BigDecimal(value)).setUnit(code).setCode(code);
        if (system != null) q.setSystem(system);
        o.setValue(q);
        return o;
    }

    @Test
    void centimetresAreRejected() {
        assertOutcome(post("Observation", weight(EncounterTest.newPatient(), "180", ObservationTest.UCUM, "cm")),
                422, "business-rule");
    }

    @Test
    void poundsAreRejected() {
        assertOutcome(post("Observation", weight(EncounterTest.newPatient(), "160", ObservationTest.UCUM, "[lb_av]")),
                422, "business-rule");
    }

    @Test
    void kgOutsideUcumSystemIsRejected() {
        String pat = EncounterTest.newPatient();
        assertOutcome(post("Observation", weight(pat, "72", "http://example.org/units", "kg")), 422, "business-rule");
        assertOutcome(post("Observation", weight(pat, "72", null, "kg")), 422, "business-rule");
    }

    @Test
    void appliesOnUpdate() {
        String pat = EncounterTest.newPatient();
        String id = create("Observation", weight(pat, "72", ObservationTest.UCUM, "kg"));
        Observation o = weight(pat, "160", ObservationTest.UCUM, "[lb_av]");
        o.setId(id);
        assertOutcome(put("/Observation/" + id, o), 422, "business-rule");
        assertEquals("kg", read("Observation", id, Observation.class).getValueQuantity().getCode());
    }

    @Test
    void kgAndGramsAreAccepted() {
        String pat = EncounterTest.newPatient();
        create("Observation", weight(pat, "72.5", ObservationTest.UCUM, "kg"));
        String id = create("Observation", weight(pat, "3400", ObservationTest.UCUM, "g"));
        Observation o = weight(pat, "3.4", ObservationTest.UCUM, "kg");
        o.setId(id);
        assertEquals(200, put("/Observation/" + id, o).status());
    }

    @Test
    void bodyWeightWithoutQuantityIsAccepted() {
        create("Observation", ObservationTest.observation(EncounterTest.newPatient(), BODY_WEIGHT, "vital-signs"));
    }

    @Test
    void otherCodesUnaffected() {
        String pat = EncounterTest.newPatient();
        Observation height = ObservationTest.observation(pat, "8302-2", "vital-signs");
        height.setValue(new Quantity().setValue(new BigDecimal("180")).setUnit("cm")
                .setSystem(ObservationTest.UCUM).setCode("cm"));
        create("Observation", height);
    }
}
