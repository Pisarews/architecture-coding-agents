package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.Test;

/** T11: _summary=count on every search endpoint. */
class T11Test {

    /** Runs the search with _summary=count and returns the total; asserts searchset, a total, and no entries. */
    static int count(String type, String... params) {
        List<String> all = new ArrayList<>(List.of(params));
        all.add("_summary=count");
        Bundle b = search(type, all.toArray(String[]::new));
        assertTrue(b.hasTotal(), "Bundle.total missing");
        assertEquals(List.of(), b.getEntry(), "_summary=count must not return entries");
        return b.getTotal();
    }

    /** The count must equal the number of matches of the same search without _summary. */
    static void assertCount(int expected, String type, String... params) {
        assertEquals(expected, search(type, params).getTotal(), "fixture sanity: " + type + " " + List.of(params));
        assertEquals(expected, count(type, params), type + " " + List.of(params));
    }

    @Test
    void patientSearch() {
        String u = uniq();
        String fam = "Cnt" + u;
        Patient p1 = PatientTest.patient(fam, "Ala" + u);
        p1.addIdentifier().setSystem("urn:sys:cnt").setValue("C1" + u);
        p1.setGender(AdministrativeGender.FEMALE);
        p1.setBirthDateElement(new DateType("1970-01-01"));
        create("Patient", p1);
        Patient p2 = PatientTest.patient(fam, "Ala" + u);
        p2.setGender(AdministrativeGender.FEMALE);
        p2.setBirthDateElement(new DateType("1980-01-01"));
        create("Patient", p2);
        Patient p3 = PatientTest.patient(fam, "Ola" + u);
        p3.setGender(AdministrativeGender.MALE);
        p3.setBirthDateElement(new DateType("1990-01-01"));
        create("Patient", p3);

        assertCount(3, "Patient", "family=" + fam);
        assertCount(2, "Patient", "given=ala" + u);
        assertCount(1, "Patient", "identifier=urn:sys:cnt|C1" + u);
        assertCount(1, "Patient", "identifier=C1" + u);
        assertCount(2, "Patient", "family=" + fam, "gender=female");
        assertCount(2, "Patient", "family=" + fam, "birthdate=ge1975-01-01");
        assertCount(1, "Patient", "family=" + fam, "given=ala" + u, "birthdate=lt1975-01-01", "gender=female");
        assertCount(0, "Patient", "family=Nobody" + u);
    }

    @Test
    void practitionerSearch() {
        String u = uniq();
        Practitioner a = PractitionerTest.practitioner("CntDoc" + u);
        a.addIdentifier().setSystem("urn:sys:cnt").setValue("P" + u);
        create("Practitioner", a);
        create("Practitioner", PractitionerTest.practitioner("CntDoc" + u + "b"));
        assertCount(2, "Practitioner", "family=cntdoc" + u);
        assertCount(1, "Practitioner", "identifier=urn:sys:cnt|P" + u);
        assertCount(1, "Practitioner", "family=CntDoc" + u, "identifier=P" + u);
    }

    @Test
    void encounterSearch() {
        String pat = EncounterTest.newPatient();
        create("Encounter", EncounterTest.encounter(pat));
        create("Encounter", EncounterTest.encounter(pat));
        Encounter planned = EncounterTest.encounter(pat);
        planned.setStatus(Encounter.EncounterStatus.PLANNED);
        create("Encounter", planned);
        assertCount(3, "Encounter", "patient=" + pat);
        assertCount(1, "Encounter", "patient=Patient/" + pat, "status=planned");
        assertCount(2, "Encounter", "patient=" + pat, "status=in-progress");
    }

    @Test
    void observationSearch() {
        String pat = EncounterTest.newPatient();
        create("Observation", ObservationTest.observation(pat, "8867-4", "vital-signs"));
        create("Observation", ObservationTest.observation(pat, "29463-7", "vital-signs"));
        create("Observation", ObservationTest.observation(pat, "2339-0", "laboratory"));
        assertCount(3, "Observation", "patient=" + pat);
        assertCount(1, "Observation", "patient=" + pat, "code=" + ObservationTest.LOINC + "|8867-4");
        assertCount(2, "Observation", "patient=" + pat, "category=vital-signs");
        assertCount(1, "Observation", "patient=" + pat, "category=laboratory", "code=2339-0");
    }

    @Test
    void appointmentSearch() {
        String pat = EncounterTest.newPatient();
        String doc = EncounterTest.newPractitioner();
        create("Appointment", AppointmentTest.booked(pat, doc, "2033-06-01T09:00:00Z", "2033-06-01T09:30:00Z"));
        create("Appointment", AppointmentTest.booked(pat, doc, "2033-06-02T09:00:00Z", "2033-06-02T09:30:00Z"));
        create("Appointment", AppointmentTest.appointment(pat, doc, "2033-06-03T09:00:00Z", "2033-06-03T09:30:00Z",
                Appointment.AppointmentStatus.CANCELLED));
        assertCount(3, "Appointment", "patient=" + pat);
        assertCount(3, "Appointment", "practitioner=Practitioner/" + doc);
        assertCount(1, "Appointment", "practitioner=" + doc, "status=cancelled");
        assertCount(2, "Appointment", "patient=" + pat, "date=ge2033-06-02");
        assertCount(1, "Appointment", "patient=" + pat, "practitioner=" + doc, "status=booked", "date=2033-06-02");
    }

    @Test
    void otherSummaryValuesAreNotSupported() {
        for (String v : List.of("true", "false", "text", "data", "bogus")) {
            assertOutcome(get("/Patient?_summary=" + v), 400, "not-supported");
        }
        assertOutcome(get("/Observation?patient=1&_summary=true"), 400, "not-supported");
    }
}
