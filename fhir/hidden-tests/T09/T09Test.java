package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.Test;

/** T09: a booked Appointment must not have an inactive Practitioner participant. */
class T09Test {

    /** active: TRUE, FALSE or null (absent). No urn:npwz identifier, so no registry check applies. */
    static String practitioner(Boolean active) {
        Practitioner p = PractitionerTest.practitioner("Act" + uniq());
        if (active != null) p.setActive(active);
        return create("Practitioner", p);
    }

    @Test
    void bookingInactivePractitionerOnCreateIs422() {
        String doc = practitioner(false);
        assertOutcome(post("Appointment", AppointmentTest.booked(EncounterTest.newPatient(), doc,
                "2032-05-01T10:00:00Z", "2032-05-01T10:30:00Z")), 422, "business-rule");
    }

    @Test
    void updateToBookedWithInactivePractitionerIs422() {
        String doc = practitioner(false);
        Appointment a = AppointmentTest.appointment(EncounterTest.newPatient(), doc,
                "2032-05-02T10:00:00Z", "2032-05-02T10:30:00Z", Appointment.AppointmentStatus.PROPOSED);
        String id = create("Appointment", a);
        a.setId(id);
        a.setStatus(Appointment.AppointmentStatus.BOOKED);
        assertOutcome(put("/Appointment/" + id, a), 422, "business-rule");
    }

    @Test
    void updateOfBookedAppointmentAfterPractitionerBecameInactiveIs422() {
        String doc = practitioner(true);
        Appointment a = AppointmentTest.booked(EncounterTest.newPatient(), doc, "2032-05-03T10:00:00Z", "2032-05-03T10:30:00Z");
        String id = create("Appointment", a);

        Practitioner p = read("Practitioner", doc, Practitioner.class);
        p.setActive(false);
        p.setMeta(null);
        assertEquals(200, put("/Practitioner/" + doc, p).status());

        a.setId(id);
        a.setDescription("Moved");
        assertOutcome(put("/Appointment/" + id, a), 422, "business-rule");
    }

    @Test
    void activeOrUnspecifiedPractitionersCanBeBooked() {
        create("Appointment", AppointmentTest.booked(EncounterTest.newPatient(), practitioner(true),
                "2032-05-04T10:00:00Z", "2032-05-04T10:30:00Z"));
        String doc = practitioner(null);
        Appointment a = AppointmentTest.appointment(EncounterTest.newPatient(), doc,
                "2032-05-04T11:00:00Z", "2032-05-04T11:30:00Z", Appointment.AppointmentStatus.PROPOSED);
        String id = create("Appointment", a);
        a.setId(id);
        a.setStatus(Appointment.AppointmentStatus.BOOKED);
        assertEquals(200, put("/Appointment/" + id, a).status());
    }

    @Test
    void inactivePractitionerInOtherStatusesIsAllowed() {
        String doc = practitioner(false);
        String pat = EncounterTest.newPatient();
        for (Appointment.AppointmentStatus s : new Appointment.AppointmentStatus[] {
                Appointment.AppointmentStatus.PROPOSED, Appointment.AppointmentStatus.CANCELLED,
                Appointment.AppointmentStatus.FULFILLED}) {
            create("Appointment", AppointmentTest.appointment(pat, doc, "2032-05-05T10:00:00Z", "2032-05-05T10:30:00Z", s));
        }
    }
}
