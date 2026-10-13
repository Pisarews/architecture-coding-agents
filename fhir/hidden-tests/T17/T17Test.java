package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

/** T17: Patient.deceased[x] is stored; future deceasedDateTime is rejected; deceased patients cannot be booked. */
class T17Test {

    static Patient patient(String fam) {
        return PatientTest.patient(fam, "Dec");
    }

    static Patient deceasedBool(String fam, boolean value) {
        Patient p = patient(fam);
        p.setDeceased(new BooleanType(value));
        return p;
    }

    static Patient deceasedAt(String fam, String dateTime) {
        Patient p = patient(fam);
        p.setDeceased(new DateTimeType(dateTime));
        return p;
    }

    static String future() {
        return Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
    }

    static Appointment appt(String pat, Appointment.AppointmentStatus status, String start, String end) {
        return AppointmentTest.appointment(pat, EncounterTest.newPractitioner(), start, end, status);
    }

    @Test
    void deceasedBooleanRoundTrips() {
        String fam = "DecB" + uniq();
        String id = create("Patient", deceasedBool(fam, true));
        Patient r = read("Patient", id, Patient.class);
        assertTrue(r.hasDeceasedBooleanType(), "deceasedBoolean expected: " + json(r));
        assertTrue(r.getDeceasedBooleanType().booleanValue());

        Patient v2 = deceasedBool(fam, false);
        v2.setId(id);
        assertEquals(200, put("/Patient/" + id, v2).status());
        r = read("Patient", id, Patient.class);
        assertTrue(r.hasDeceasedBooleanType(), "deceasedBoolean expected: " + json(r));
        assertFalse(r.getDeceasedBooleanType().booleanValue());
    }

    @Test
    void deceasedDateTimeRoundTrips() {
        String fam = "DecDt" + uniq();
        String id = create("Patient", deceasedAt(fam, "2021-03-04T05:06:07Z"));
        Patient r = read("Patient", id, Patient.class);
        assertTrue(r.hasDeceasedDateTimeType(), "deceasedDateTime expected: " + json(r));
        assertEquals(new DateTimeType("2021-03-04T05:06:07Z").getValue(), r.getDeceasedDateTimeType().getValue());

        Patient v2 = deceasedAt(fam, "2022-01-02T03:04:05+02:00");
        v2.setId(id);
        assertEquals(200, put("/Patient/" + id, v2).status());
        r = read("Patient", id, Patient.class);
        assertTrue(r.hasDeceasedDateTimeType(), "deceasedDateTime expected: " + json(r));
        assertEquals(new DateTimeType("2022-01-02T03:04:05+02:00").getValue(), r.getDeceasedDateTimeType().getValue());
    }

    @Test
    void formCanBeSwitchedAndRemovedOnUpdate() {
        String fam = "DecSw" + uniq();
        String id = create("Patient", deceasedBool(fam, true));
        Patient v2 = deceasedAt(fam, "2020-07-08T09:10:11Z");
        v2.setId(id);
        assertEquals(200, put("/Patient/" + id, v2).status());
        Patient r = read("Patient", id, Patient.class);
        assertTrue(r.hasDeceasedDateTimeType(), json(r));
        assertEquals(new DateTimeType("2020-07-08T09:10:11Z").getValue(), r.getDeceasedDateTimeType().getValue());

        Patient v3 = patient(fam);
        v3.setId(id);
        assertEquals(200, put("/Patient/" + id, v3).status());
        assertFalse(read("Patient", id, Patient.class).hasDeceased(), "deceased must be removed");
    }

    @Test
    void futureDeceasedDateTimeIsRejected() {
        String fam = "DecFut" + uniq();
        assertOutcome(post("Patient", deceasedAt(fam, future())), 422, "business-rule");
        assertEquals(0, search("Patient", "family=" + fam).getTotal());

        String id = create("Patient", patient(fam));
        Patient p = deceasedAt(fam, future());
        p.setId(id);
        assertOutcome(put("/Patient/" + id, p), 422, "business-rule");
        assertFalse(read("Patient", id, Patient.class).hasDeceased());
    }

    @Test
    void bookingDeceasedPatientIsRejected() {
        String byBool = create("Patient", deceasedBool("DecBk" + uniq(), true));
        assertOutcome(post("Appointment", appt(byBool, Appointment.AppointmentStatus.BOOKED,
                "2034-03-01T10:00:00Z", "2034-03-01T10:30:00Z")), 422, "business-rule");
        String byDate = create("Patient", deceasedAt("DecBk" + uniq(), "2019-01-01T00:00:00Z"));
        assertOutcome(post("Appointment", appt(byDate, Appointment.AppointmentStatus.BOOKED,
                "2034-03-01T11:00:00Z", "2034-03-01T11:30:00Z")), 422, "business-rule");
    }

    @Test
    void updateToBookedWithDeceasedPatientIsRejected() {
        String pat = create("Patient", deceasedBool("DecUp" + uniq(), true));
        Appointment a = appt(pat, Appointment.AppointmentStatus.PROPOSED, "2034-03-02T10:00:00Z", "2034-03-02T10:30:00Z");
        String id = create("Appointment", a);
        a.setId(id);
        a.setStatus(Appointment.AppointmentStatus.BOOKED);
        assertOutcome(put("/Appointment/" + id, a), 422, "business-rule");
        assertEquals(Appointment.AppointmentStatus.PROPOSED, read("Appointment", id, Appointment.class).getStatus());
    }

    @Test
    void updateOfBookedAppointmentAfterPatientDiedIsRejected() {
        String fam = "DecLater" + uniq();
        String pat = create("Patient", patient(fam));
        Appointment a = appt(pat, Appointment.AppointmentStatus.BOOKED, "2034-03-03T10:00:00Z", "2034-03-03T10:30:00Z");
        String id = create("Appointment", a);

        Patient dead = deceasedAt(fam, "2024-05-06T07:08:09Z");
        dead.setId(pat);
        assertEquals(200, put("/Patient/" + pat, dead).status());

        a.setId(id);
        a.setDescription("Moved");
        assertOutcome(put("/Appointment/" + id, a), 422, "business-rule");
    }

    @Test
    void livingAndNonBookedAreAllowed() {
        String alive = create("Patient", deceasedBool("DecNo" + uniq(), false));
        create("Appointment", appt(alive, Appointment.AppointmentStatus.BOOKED, "2034-03-04T10:00:00Z", "2034-03-04T10:30:00Z"));
        String plain = EncounterTest.newPatient();
        create("Appointment", appt(plain, Appointment.AppointmentStatus.BOOKED, "2034-03-04T11:00:00Z", "2034-03-04T11:30:00Z"));

        String dead = create("Patient", deceasedBool("DecOk" + uniq(), true));
        create("Appointment", appt(dead, Appointment.AppointmentStatus.PROPOSED, "2034-03-05T10:00:00Z", "2034-03-05T10:30:00Z"));
        create("Appointment", appt(dead, Appointment.AppointmentStatus.CANCELLED, "2034-03-05T11:00:00Z", "2034-03-05T11:30:00Z"));
    }
}
