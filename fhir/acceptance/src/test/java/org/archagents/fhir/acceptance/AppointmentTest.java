package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

class AppointmentTest {

    static Appointment appointment(String pat, String doc, String start, String end, Appointment.AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setStatus(status);
        a.setDescription("Check-up");
        a.setStartElement(new InstantType(start));
        a.setEndElement(new InstantType(end));
        a.addParticipant().setActor(new Reference("Patient/" + pat)).setStatus(Appointment.ParticipationStatus.ACCEPTED);
        a.addParticipant().setActor(new Reference("Practitioner/" + doc)).setStatus(Appointment.ParticipationStatus.ACCEPTED);
        return a;
    }

    static Appointment booked(String pat, String doc, String start, String end) {
        return appointment(pat, doc, start, end, Appointment.AppointmentStatus.BOOKED);
    }

    @Test
    void createReadRoundTrip() {
        String pat = EncounterTest.newPatient();
        String doc = EncounterTest.newPractitioner();
        String id = create("Appointment", booked(pat, doc, "2031-03-04T10:00:00Z", "2031-03-04T10:30:00Z"));

        Appointment r = read("Appointment", id, Appointment.class);
        assertEquals(Appointment.AppointmentStatus.BOOKED, r.getStatus());
        assertEquals("Check-up", r.getDescription());
        assertEquals(new InstantType("2031-03-04T10:00:00Z").getValue(), r.getStart());
        assertEquals(new InstantType("2031-03-04T10:30:00Z").getValue(), r.getEnd());
        assertEquals(2, r.getParticipant().size());
        assertEquals("Patient/" + pat, r.getParticipant().get(0).getActor().getReference());
        assertEquals(Appointment.ParticipationStatus.ACCEPTED, r.getParticipant().get(0).getStatus());
        assertEquals("Practitioner/" + doc, r.getParticipant().get(1).getActor().getReference());
    }

    @Test
    void r10_participantRequired() {
        Appointment a = booked(EncounterTest.newPatient(), EncounterTest.newPractitioner(), "2031-03-04T10:00:00Z", "2031-03-04T10:30:00Z");
        a.getParticipant().clear();
        assertOutcome(post("Appointment", a), 422, "required");
    }

    @Test
    void r10_startRequired() {
        Appointment a = booked(EncounterTest.newPatient(), EncounterTest.newPractitioner(), "2031-03-04T10:00:00Z", "2031-03-04T10:30:00Z");
        a.setStartElement(null);
        assertOutcome(post("Appointment", a), 422, "required");
    }

    @Test
    void r11_endMustBeAfterStart() {
        String pat = EncounterTest.newPatient();
        String doc = EncounterTest.newPractitioner();
        assertOutcome(post("Appointment", booked(pat, doc, "2031-03-04T10:00:00Z", "2031-03-04T10:00:00Z")), 422, "business-rule");
        assertOutcome(post("Appointment", booked(pat, doc, "2031-03-04T10:00:00Z", "2031-03-04T09:00:00Z")), 422, "business-rule");
    }

    @Test
    void r12_unknownActorIs422NotFound() {
        Appointment a = booked(EncounterTest.newPatient(), "987654321", "2031-03-04T10:00:00Z", "2031-03-04T10:30:00Z");
        assertOutcome(post("Appointment", a), 422, "not-found");
    }

    @Test
    void r13_overlappingBookedAppointmentsForPractitionerRejected() {
        String doc = EncounterTest.newPractitioner();
        create("Appointment", booked(EncounterTest.newPatient(), doc, "2031-05-01T10:00:00Z", "2031-05-01T11:00:00Z"));
        assertOutcome(post("Appointment", booked(EncounterTest.newPatient(), doc, "2031-05-01T10:30:00Z", "2031-05-01T11:30:00Z")),
                422, "business-rule");
    }

    @Test
    void r13_adjacentSlotsAreAllowed() {
        String doc = EncounterTest.newPractitioner();
        create("Appointment", booked(EncounterTest.newPatient(), doc, "2031-05-02T10:00:00Z", "2031-05-02T11:00:00Z"));
        create("Appointment", booked(EncounterTest.newPatient(), doc, "2031-05-02T11:00:00Z", "2031-05-02T12:00:00Z"));
    }

    @Test
    void r13_onlyBookedAppointmentsConflict() {
        String doc = EncounterTest.newPractitioner();
        create("Appointment", appointment(EncounterTest.newPatient(), doc, "2031-05-03T10:00:00Z", "2031-05-03T11:00:00Z",
                Appointment.AppointmentStatus.PROPOSED));
        create("Appointment", booked(EncounterTest.newPatient(), doc, "2031-05-03T10:30:00Z", "2031-05-03T11:30:00Z"));
    }

    @Test
    void r13_differentPractitionersDoNotConflict() {
        create("Appointment", booked(EncounterTest.newPatient(), EncounterTest.newPractitioner(), "2031-05-04T10:00:00Z", "2031-05-04T11:00:00Z"));
        create("Appointment", booked(EncounterTest.newPatient(), EncounterTest.newPractitioner(), "2031-05-04T10:00:00Z", "2031-05-04T11:00:00Z"));
    }

    @Test
    void r13_updateIgnoresItselfButChecksOthers() {
        String doc = EncounterTest.newPractitioner();
        String pat = EncounterTest.newPatient();
        String a = create("Appointment", booked(pat, doc, "2031-05-05T10:00:00Z", "2031-05-05T11:00:00Z"));
        String b = create("Appointment", booked(pat, doc, "2031-05-05T12:00:00Z", "2031-05-05T13:00:00Z"));

        Appointment same = booked(pat, doc, "2031-05-05T10:00:00Z", "2031-05-05T11:15:00Z");
        same.setId(a);
        assertEquals(200, put("/Appointment/" + a, same).status());

        Appointment clash = booked(pat, doc, "2031-05-05T11:00:00Z", "2031-05-05T12:30:00Z");
        clash.setId(b);
        assertOutcome(put("/Appointment/" + b, clash), 422, "business-rule");
    }

    @Test
    void searchByPatientPractitionerStatusAndDate() {
        String pat = EncounterTest.newPatient();
        String doc = EncounterTest.newPractitioner();
        String a = create("Appointment", booked(pat, doc, "2031-06-01T09:00:00Z", "2031-06-01T09:30:00Z"));
        String b = create("Appointment", booked(pat, doc, "2031-06-02T23:30:00Z", "2031-06-02T23:59:00Z"));
        String c = create("Appointment", appointment(pat, doc, "2031-06-03T09:00:00Z", "2031-06-03T09:30:00Z",
                Appointment.AppointmentStatus.CANCELLED));

        assertEquals(List.of(a, b, c), ids(search("Appointment", "patient=" + pat)));
        assertEquals(List.of(a, b, c), ids(search("Appointment", "practitioner=Practitioner/" + doc)));
        assertEquals(List.of(c), ids(search("Appointment", "practitioner=" + doc, "status=cancelled")));
        assertEquals(List.of(b), ids(search("Appointment", "practitioner=" + doc, "date=2031-06-02")));
        assertEquals(List.of(b, c), ids(search("Appointment", "practitioner=" + doc, "date=ge2031-06-02")));
        assertEquals(List.of(a), ids(search("Appointment", "practitioner=" + doc, "date=lt2031-06-02")));
        assertEquals(List.of(), ids(search("Appointment", "patient=" + EncounterTest.newPatient())));
    }
}
