package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * T08: SMS through fhir.sms.gateway-url when an appointment becomes booked.
 * The app is started with --fhir.sms.gateway-url=http://localhost:{mock.port}/sms.
 */
class T08Test {

    static final String PATH = "/sms";
    /** Phone numbers with this prefix make the mock gateway answer 500. */
    static final String FAILING_PREFIX = "+48555";

    record Req(String method, String path, String body) {}

    static final List<Req> REQUESTS = new CopyOnWriteArrayList<>();
    static final ObjectMapper JSON = new ObjectMapper();
    static HttpServer server;
    static ExecutorService pool;

    @BeforeAll
    static void startMock() throws Exception {
        int port = Integer.getInteger("mock.port", 18080);
        long deadline = System.currentTimeMillis() + 20_000;
        while (true) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                break;
            } catch (BindException e) {
                if (System.currentTimeMillis() > deadline) throw e;
                Thread.sleep(200);
            }
        }
        server.createContext("/", ex -> {
            String body;
            try (InputStream in = ex.getRequestBody()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            REQUESTS.add(new Req(ex.getRequestMethod(), ex.getRequestURI().getPath(), body));
            int status = body.contains(FAILING_PREFIX) ? 500 : 200;
            byte[] out = "{}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(status, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        pool = Executors.newFixedThreadPool(4);
        server.setExecutor(pool);
        server.start();
    }

    @AfterAll
    static void stopMock() {
        if (server != null) server.stop(0);
        if (pool != null) pool.shutdownNow();
    }

    static String newPhone() {
        return "+486" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    static String patientWithPhone(String phone) {
        Patient p = PatientTest.patient("Sms" + uniq());
        p.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(phone);
        return create("Patient", p);
    }

    static JsonNode parse(Req r) {
        try {
            return JSON.readTree(r.body());
        } catch (Exception e) {
            return null;
        }
    }

    /** All SMS POSTs addressed to {@code phone}, as parsed JSON bodies. */
    static List<JsonNode> smsTo(String phone) {
        return REQUESTS.stream()
                .filter(r -> PATH.equals(r.path()) && "POST".equals(r.method()))
                .map(T08Test::parse)
                .filter(n -> n != null && n.has("to") && phone.equals(n.get("to").asText()))
                .toList();
    }

    /** Waits up to 5 s until at least {@code n} SMS to the phone arrived, then a bit longer for duplicates. */
    static List<JsonNode> awaitSms(String phone, int n) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (smsTo(phone).size() < n && System.currentTimeMillis() < deadline) Thread.sleep(50);
        Thread.sleep(1_000);
        return smsTo(phone);
    }

    static Appointment appt(Appointment.AppointmentStatus status, String start, String end, String... patients) {
        Appointment a = AppointmentTest.appointment(patients[0], EncounterTest.newPractitioner(), start, end, status);
        for (int i = 1; i < patients.length; i++) {
            a.addParticipant().setActor(new Reference("Patient/" + patients[i])).setStatus(Appointment.ParticipationStatus.ACCEPTED);
        }
        return a;
    }

    @Test
    void bookedCreateSendsOneSmsPerPatientWithPhone() throws Exception {
        String phoneA = newPhone();
        String phoneB = newPhone();
        String a = patientWithPhone(phoneA);
        Patient pb = PatientTest.patient("SmsB" + uniq());
        String emailB = "b" + uniq() + "@example.org";
        pb.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(emailB);
        pb.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(phoneB);
        String b = create("Patient", pb);
        Patient pc = PatientTest.patient("SmsC" + uniq());
        String emailC = "c" + uniq() + "@example.org";
        pc.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(emailC);
        String c = create("Patient", pc);
        String d = EncounterTest.newPatient();

        create("Appointment", appt(Appointment.AppointmentStatus.BOOKED, "2032-04-07T09:05:00Z", "2032-04-07T09:35:00Z", a, b, c, d));

        for (String phone : List.of(phoneA, phoneB)) {
            List<JsonNode> got = awaitSms(phone, 1);
            assertEquals(1, got.size(), "expected exactly one SMS to " + phone + ", got " + got);
            assertEquals("Appointment on 2032-04-07 at 09:05 UTC", got.get(0).path("text").asText());
        }
        assertEquals(List.of(), smsTo(emailB));
        assertEquals(List.of(), smsTo(emailC));
    }

    @Test
    void timeIsFormattedInUtc() throws Exception {
        String phone = newPhone();
        create("Appointment", appt(Appointment.AppointmentStatus.BOOKED, "2032-12-31T23:50:00Z", "2033-01-01T00:20:00Z",
                patientWithPhone(phone)));
        List<JsonNode> got = awaitSms(phone, 1);
        assertEquals(1, got.size(), got.toString());
        assertEquals("Appointment on 2032-12-31 at 23:50 UTC", got.get(0).path("text").asText());
    }

    @Test
    void notBookedCreateSendsNothing() throws Exception {
        String phone = newPhone();
        String pat = patientWithPhone(phone);
        create("Appointment", appt(Appointment.AppointmentStatus.PROPOSED, "2032-04-08T10:00:00Z", "2032-04-08T10:30:00Z", pat));
        create("Appointment", appt(Appointment.AppointmentStatus.CANCELLED, "2032-04-08T11:00:00Z", "2032-04-08T11:30:00Z", pat));
        Thread.sleep(2_000);
        assertEquals(List.of(), smsTo(phone));
    }

    @Test
    void updateToBookedSendsSms() throws Exception {
        String phone = newPhone();
        String pat = patientWithPhone(phone);
        Appointment a = appt(Appointment.AppointmentStatus.PROPOSED, "2032-04-09T14:30:00Z", "2032-04-09T15:00:00Z", pat);
        String id = create("Appointment", a);
        a.setId(id);
        a.setStatus(Appointment.AppointmentStatus.BOOKED);
        Resp resp = put("/Appointment/" + id, a);
        assertEquals(200, resp.status(), resp.body());
        List<JsonNode> got = awaitSms(phone, 1);
        assertEquals(1, got.size(), got.toString());
        assertEquals("Appointment on 2032-04-09 at 14:30 UTC", got.get(0).path("text").asText());
    }

    @Test
    void updateThatKeepsBookedSendsNothingMore() throws Exception {
        String phone = newPhone();
        String pat = patientWithPhone(phone);
        Appointment a = appt(Appointment.AppointmentStatus.BOOKED, "2032-04-10T08:00:00Z", "2032-04-10T08:30:00Z", pat);
        String id = create("Appointment", a);
        assertEquals(1, awaitSms(phone, 1).size());

        a.setId(id);
        a.setDescription("Changed");
        assertEquals(200, put("/Appointment/" + id, a).status());
        a.setStatus(Appointment.AppointmentStatus.CANCELLED);
        assertEquals(200, put("/Appointment/" + id, a).status());
        Thread.sleep(2_000);
        assertEquals(1, smsTo(phone).size(), "updates that keep or leave status booked must not send");
    }

    @Test
    void gatewayFailureDoesNotFailRequest() {
        String phone = FAILING_PREFIX + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        String pat = patientWithPhone(phone);
        Resp created = post("Appointment", appt(Appointment.AppointmentStatus.BOOKED, "2032-04-11T08:00:00Z", "2032-04-11T08:30:00Z", pat));
        assertEquals(201, created.status(), created.body());

        Appointment a = appt(Appointment.AppointmentStatus.PROPOSED, "2032-04-12T08:00:00Z", "2032-04-12T08:30:00Z", pat);
        String id = create("Appointment", a);
        a.setId(id);
        a.setStatus(Appointment.AppointmentStatus.BOOKED);
        Resp updated = put("/Appointment/" + id, a);
        assertEquals(200, updated.status(), updated.body());
        assertEquals(Appointment.AppointmentStatus.BOOKED, read("Appointment", id, Appointment.class).getStatus());
    }
}
