package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.Test;

/** T21: final Observations may only move to amended/corrected/final/entered-in-error; entered-in-error is frozen. */
class T21Test {

    static Observation obs(String pat, ObservationStatus status, String value) {
        Observation o = ObservationTest.observation(pat, "8867-4", "vital-signs");
        o.setStatus(status);
        o.setValue(new Quantity().setValue(new BigDecimal(value)).setUnit("/min")
                .setSystem(ObservationTest.UCUM).setCode("/min"));
        return o;
    }

    static Resp update(String id, String pat, ObservationStatus status, String value) {
        Observation o = obs(pat, status, value);
        o.setId(id);
        return put("/Observation/" + id, o);
    }

    static void assertUnchanged(String id, String version, ObservationStatus status, String value) {
        Observation r = read("Observation", id, Observation.class);
        assertEquals(version, r.getMeta().getVersionId(), "version must be unchanged");
        assertEquals(status, r.getStatus(), "status must be unchanged");
        assertEquals(0, new BigDecimal(value).compareTo(r.getValueQuantity().getValue()), "value must be unchanged");
    }

    @Test
    void finalCannotGoBackToEarlierStatuses() {
        String pat = EncounterTest.newPatient();
        String id = create("Observation", obs(pat, ObservationStatus.FINAL, "70"));
        for (ObservationStatus s : List.of(ObservationStatus.REGISTERED, ObservationStatus.PRELIMINARY, ObservationStatus.CANCELLED)) {
            assertOutcome(update(id, pat, s, "71"), 422, "business-rule");
            assertUnchanged(id, "1", ObservationStatus.FINAL, "70");
        }
    }

    @Test
    void finalCanMoveToAllowedStatuses() {
        String pat = EncounterTest.newPatient();
        // `corrected` is not in the baseline status subset (SPEC §4, C11 -> 400), so it is not exercised here.
        for (ObservationStatus s : List.of(ObservationStatus.AMENDED, ObservationStatus.FINAL, ObservationStatus.ENTEREDINERROR)) {
            String id = create("Observation", obs(pat, ObservationStatus.FINAL, "70"));
            Resp r = update(id, pat, s, "72");
            assertEquals(200, r.status(), () -> "final -> " + s.toCode() + ": " + r.body());
            assertEquals(s, read("Observation", id, Observation.class).getStatus());
        }
    }

    @Test
    void enteredInErrorCannotBeChanged() {
        String pat = EncounterTest.newPatient();
        String id = create("Observation", obs(pat, ObservationStatus.FINAL, "70"));
        assertEquals(200, update(id, pat, ObservationStatus.ENTEREDINERROR, "70").status());
        for (ObservationStatus s : List.of(ObservationStatus.FINAL, ObservationStatus.AMENDED, ObservationStatus.PRELIMINARY)) {
            assertOutcome(update(id, pat, s, "70"), 422, "business-rule");
            assertUnchanged(id, "2", ObservationStatus.ENTEREDINERROR, "70");
        }
        assertOutcome(update(id, pat, ObservationStatus.ENTEREDINERROR, "99"), 422, "business-rule");
        assertUnchanged(id, "2", ObservationStatus.ENTEREDINERROR, "70");
    }

    @Test
    void createdAsEnteredInErrorIsFrozenToo() {
        String pat = EncounterTest.newPatient();
        String id = create("Observation", obs(pat, ObservationStatus.ENTEREDINERROR, "80"));
        assertOutcome(update(id, pat, ObservationStatus.REGISTERED, "81"), 422, "business-rule");
        assertUnchanged(id, "1", ObservationStatus.ENTEREDINERROR, "80");
    }

    @Test
    void otherTransitionsBehaveAsBefore() {
        String pat = EncounterTest.newPatient();
        String id = create("Observation", obs(pat, ObservationStatus.REGISTERED, "70"));
        String[] values = {"71", "72", "73", "74", "75"};
        ObservationStatus[] path = {ObservationStatus.PRELIMINARY, ObservationStatus.REGISTERED, ObservationStatus.CANCELLED,
                ObservationStatus.PRELIMINARY, ObservationStatus.FINAL};
        for (int i = 0; i < path.length; i++) {
            Resp r = update(id, pat, path[i], values[i]);
            assertEquals(200, r.status(), r.body());
        }
        String amended = create("Observation", obs(pat, ObservationStatus.AMENDED, "70"));
        assertEquals(200, update(amended, pat, ObservationStatus.PRELIMINARY, "70").status());
        create("Observation", obs(pat, ObservationStatus.ENTEREDINERROR, "70"));
    }
}
