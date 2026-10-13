package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class P3Test {

    @Test
    void successResponsesCarryHeader() {
        assertEquals("4.0.1", get("/metadata").header("X-FHIR-Version"));
        assertEquals("4.0.1", post("Patient", PatientTest.patient("Hdr" + uniq())).header("X-FHIR-Version"));
        assertEquals("4.0.1", get("/Patient?family=Hdr").header("X-FHIR-Version"));
    }

    @Test
    void errorResponsesCarryHeader() {
        assertEquals("4.0.1", get("/Patient/987654321").header("X-FHIR-Version"));
        assertEquals("4.0.1", get("/Patient?shoeSize=1").header("X-FHIR-Version"));
        assertEquals("4.0.1", postRaw("Patient", "{broken", FHIR_JSON).header("X-FHIR-Version"));
    }
}
