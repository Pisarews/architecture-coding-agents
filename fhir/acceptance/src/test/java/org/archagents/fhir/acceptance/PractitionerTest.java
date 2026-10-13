package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.Test;

class PractitionerTest {

    static Practitioner practitioner(String family) {
        Practitioner p = new Practitioner();
        p.addName().setFamily(family).addGiven("Dr");
        return p;
    }

    @Test
    void createReadRoundTrip() {
        String fam = "Prac" + uniq();
        Practitioner p = practitioner(fam);
        p.setActive(true);
        p.addIdentifier().setSystem("urn:npwz").setValue("NPWZ-" + fam);
        Resp resp = post("Practitioner", p);
        assertEquals(201, resp.status(), resp.body());
        assertEquals("W/\"1\"", resp.header("ETag"));
        String id = resp.as(Practitioner.class).getIdElement().getIdPart();
        assertTrue(resp.header("Location").endsWith("/fhir/Practitioner/" + id + "/_history/1"));

        Practitioner r = read("Practitioner", id, Practitioner.class);
        assertEquals(fam, r.getNameFirstRep().getFamily());
        assertEquals("Dr", r.getNameFirstRep().getGiven().get(0).getValue());
        assertTrue(r.getActive());
        assertEquals("urn:npwz", r.getIdentifierFirstRep().getSystem());
        assertEquals("NPWZ-" + fam, r.getIdentifierFirstRep().getValue());
        assertEquals("1", r.getMeta().getVersionId());
    }

    @Test
    void r3_familyRequired() {
        Practitioner p = new Practitioner();
        p.addIdentifier().setSystem("urn:npwz").setValue("x" + uniq());
        assertOutcome(post("Practitioner", p), 422, "required");
    }

    @Test
    void updateIncrementsVersion() {
        String fam = "PracUpd" + uniq();
        String id = create("Practitioner", practitioner(fam));
        Practitioner p = practitioner(fam + "2");
        p.setId(id);
        Resp resp = put("/Practitioner/" + id, p);
        assertEquals(200, resp.status(), resp.body());
        assertEquals("W/\"2\"", resp.header("ETag"));
        assertEquals(fam + "2", read("Practitioner", id, Practitioner.class).getNameFirstRep().getFamily());
    }

    @Test
    void searchByFamilyAndIdentifier() {
        String u = uniq();
        Practitioner p1 = practitioner("Lekarz" + u);
        p1.addIdentifier().setSystem("urn:npwz").setValue("N1" + u);
        String a = create("Practitioner", p1);
        String b = create("Practitioner", practitioner("LEKARZ" + u + "b"));
        assertEquals(List.of(a, b), ids(search("Practitioner", "family=lekarz" + u)));
        assertEquals(List.of(a), ids(search("Practitioner", "identifier=urn:npwz|N1" + u)));
        assertEquals(List.of(a), ids(search("Practitioner", "identifier=N1" + u)));
    }

    @Test
    void unsupportedParamIs400() {
        assertOutcome(get("/Practitioner?gender=male"), 400, "not-supported");
    }
}
