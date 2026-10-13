package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;

import java.math.BigDecimal;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.Test;

class P1Test {

    static Observation heartRate(String pat, String value) {
        Observation o = ObservationTest.observation(pat, "8867-4", "vital-signs");
        o.setValue(new Quantity().setValue(new BigDecimal(value)).setUnit("/min")
                .setSystem(ObservationTest.UCUM).setCode("/min"));
        return o;
    }

    @Test
    void tooLowIsRejected() {
        assertOutcome(post("Observation", heartRate(EncounterTest.newPatient(), "19.9")), 422, "business-rule");
    }

    @Test
    void tooHighIsRejected() {
        assertOutcome(post("Observation", heartRate(EncounterTest.newPatient(), "301")), 422, "business-rule");
    }

    @Test
    void boundariesAreAccepted() {
        String pat = EncounterTest.newPatient();
        create("Observation", heartRate(pat, "20"));
        create("Observation", heartRate(pat, "300"));
    }

    @Test
    void appliesOnUpdate() {
        String pat = EncounterTest.newPatient();
        String id = create("Observation", heartRate(pat, "70"));
        Observation o = heartRate(pat, "0");
        o.setId(id);
        assertOutcome(put("/Observation/" + id, o), 422, "business-rule");
    }

    @Test
    void otherCodesUnaffected() {
        Observation o = ObservationTest.observation(EncounterTest.newPatient(), "29463-7", "vital-signs");
        o.setValue(new Quantity().setValue(new BigDecimal("1000")).setUnit("g")
                .setSystem(ObservationTest.UCUM).setCode("g"));
        create("Observation", o);
    }
}
