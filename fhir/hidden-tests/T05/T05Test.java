package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.InstantType;
import org.junit.jupiter.api.Test;

/** T05: booked appointments must last between 5 and 240 minutes (inclusive). Other statuses are not limited. */
class T05Test {

    /** A fresh patient and practitioner each time, so R13 (overlap) never interferes. */
    static Appointment appt(Appointment.AppointmentStatus status, String start, String end) {
        return AppointmentTest.appointment(EncounterTest.newPatient(), EncounterTest.newPractitioner(), start, end, status);
    }

    static Appointment booked(String start, String end) {
        return appt(Appointment.AppointmentStatus.BOOKED, start, end);
    }

    @Test
    void tooShortBookedIsRejected() {
        assertOutcome(post("Appointment", booked("2034-01-10T10:00:00Z", "2034-01-10T10:04:00Z")), 422, "business-rule");
        assertOutcome(post("Appointment", booked("2034-01-10T10:00:00Z", "2034-01-10T10:01:00Z")), 422, "business-rule");
    }

    @Test
    void tooLongBookedIsRejected() {
        assertOutcome(post("Appointment", booked("2034-01-11T08:00:00Z", "2034-01-11T12:01:00Z")), 422, "business-rule");
        assertOutcome(post("Appointment", booked("2034-01-11T08:00:00Z", "2034-01-12T08:00:00Z")), 422, "business-rule");
    }

    @Test
    void boundariesAreAccepted() {
        create("Appointment", booked("2034-01-12T10:00:00Z", "2034-01-12T10:05:00Z"));
        create("Appointment", booked("2034-01-12T10:00:00Z", "2034-01-12T14:00:00Z"));
        create("Appointment", booked("2034-01-12T22:30:00Z", "2034-01-13T02:30:00Z"));
    }

    @Test
    void appliesOnUpdateOfBookedAppointment() {
        Appointment a = booked("2034-01-13T10:00:00Z", "2034-01-13T10:30:00Z");
        String id = create("Appointment", a);
        a.setId(id);
        a.setEndElement(new InstantType("2034-01-13T15:00:00Z"));
        assertOutcome(put("/Appointment/" + id, a), 422, "business-rule");
        Appointment now = read("Appointment", id, Appointment.class);
        assertEquals("1", now.getMeta().getVersionId(), "nothing may change after 422");
        assertEquals(new InstantType("2034-01-13T10:30:00Z").getValue(), now.getEnd());

        a.setEndElement(new InstantType("2034-01-13T14:00:00Z"));
        Resp ok = put("/Appointment/" + id, a);
        assertEquals(200, ok.status(), ok.body());
    }

    @Test
    void appliesOnUpdateToBooked() {
        Appointment a = appt(Appointment.AppointmentStatus.PROPOSED, "2034-01-14T10:00:00Z", "2034-01-14T10:02:00Z");
        String id = create("Appointment", a);
        a.setId(id);
        a.setStatus(Appointment.AppointmentStatus.BOOKED);
        assertOutcome(put("/Appointment/" + id, a), 422, "business-rule");
        assertEquals(Appointment.AppointmentStatus.PROPOSED, read("Appointment", id, Appointment.class).getStatus());
    }

    @Test
    void otherStatusesAreNotLimited() {
        create("Appointment", appt(Appointment.AppointmentStatus.PROPOSED, "2034-01-15T10:00:00Z", "2034-01-15T10:01:00Z"));
        create("Appointment", appt(Appointment.AppointmentStatus.PROPOSED, "2034-01-15T08:00:00Z", "2034-01-15T18:00:00Z"));
        create("Appointment", appt(Appointment.AppointmentStatus.CANCELLED, "2034-01-15T10:00:00Z", "2034-01-15T10:03:00Z"));
        create("Appointment", appt(Appointment.AppointmentStatus.FULFILLED, "2034-01-15T08:00:00Z", "2034-01-15T13:00:00Z"));
        create("Appointment", appt(Appointment.AppointmentStatus.NOSHOW, "2034-01-15T08:00:00Z", "2034-01-16T08:00:00Z"));

        Appointment a = booked("2034-01-16T10:00:00Z", "2034-01-16T10:30:00Z");
        String id = create("Appointment", a);
        a.setId(id);
        a.setStatus(Appointment.AppointmentStatus.CANCELLED);
        a.setEndElement(new InstantType("2034-01-16T20:00:00Z"));
        Resp r = put("/Appointment/" + id, a);
        assertEquals(200, r.status(), r.body());
    }
}
