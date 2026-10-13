package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class PatientTest {

    static Patient patient(String family, String... given) {
        Patient p = new Patient();
        var name = p.addName().setFamily(family);
        for (String g : given) name.addGiven(g);
        return p;
    }

    @Test
    void createReturns201WithLocationEtagAndMeta() {
        Resp resp = post("Patient", patient("Create" + uniq(), "Anna"));
        assertEquals(201, resp.status(), resp.body());
        Patient p = resp.as(Patient.class);
        String id = p.getIdElement().getIdPart();
        assertTrue(id.matches("\\d+"));
        assertEquals("1", p.getMeta().getVersionId());
        assertNotNull(p.getMeta().getLastUpdated());
        assertEquals("W/\"1\"", resp.header("ETag"));
        String loc = resp.header("Location");
        assertNotNull(loc);
        assertTrue(loc.startsWith("http"), loc);
        assertTrue(loc.endsWith("/fhir/Patient/" + id + "/_history/1"), loc);
    }

    @Test
    void createIgnoresClientSuppliedId() {
        Patient p = patient("ClientId" + uniq());
        p.setId("999999999");
        String id = create("Patient", p);
        assertNotEquals("999999999", id);
    }

    @Test
    void readRoundTripsAllPersistedElements() {
        String fam = "Round" + uniq();
        Patient p = patient(fam, "Jan", "Maria");
        p.setActive(true);
        p.addIdentifier().setSystem("urn:oid:2.16.840.1.113883.3.4424.1.1.616").setValue("PESEL-" + fam);
        p.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("+48 600 000 000");
        p.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("jan@example.org");
        p.setGender(AdministrativeGender.MALE);
        p.setBirthDateElement(new DateType("1980-02-29"));
        String id = create("Patient", p);

        Resp resp = get("/Patient/" + id);
        assertEquals(200, resp.status());
        assertEquals("W/\"1\"", resp.header("ETag"));
        Patient r = resp.as(Patient.class);
        assertEquals(id, r.getIdElement().getIdPart());
        assertTrue(r.getActive());
        assertEquals(fam, r.getNameFirstRep().getFamily());
        assertEquals(List.of("Jan", "Maria"), r.getNameFirstRep().getGiven().stream().map(Object::toString).toList());
        assertEquals(1, r.getIdentifier().size());
        assertEquals("urn:oid:2.16.840.1.113883.3.4424.1.1.616", r.getIdentifierFirstRep().getSystem());
        assertEquals("PESEL-" + fam, r.getIdentifierFirstRep().getValue());
        assertEquals(2, r.getTelecom().size());
        assertEquals(ContactPoint.ContactPointSystem.PHONE, r.getTelecom().get(0).getSystem());
        assertEquals("+48 600 000 000", r.getTelecom().get(0).getValue());
        assertEquals(ContactPoint.ContactPointSystem.EMAIL, r.getTelecom().get(1).getSystem());
        assertEquals(AdministrativeGender.MALE, r.getGender());
        assertEquals("1980-02-29", r.getBirthDateElement().getValueAsString());
    }

    @Test
    void onlyFirstNameIsStored() {
        String fam = "First" + uniq();
        Patient p = patient(fam, "A");
        p.addName().setFamily("Second").addGiven("B");
        String id = create("Patient", p);
        Patient r = read("Patient", id, Patient.class);
        assertEquals(1, r.getName().size());
        assertEquals(fam, r.getNameFirstRep().getFamily());
    }

    @Test
    void updateIncrementsVersionAndPersistsChanges() {
        String fam = "Upd" + uniq();
        String id = create("Patient", patient(fam, "Old"));
        Patient changed = patient(fam + "x", "New");
        changed.setId(id);
        changed.setGender(AdministrativeGender.FEMALE);
        Resp resp = put("/Patient/" + id, changed);
        assertEquals(200, resp.status(), resp.body());
        assertEquals("W/\"2\"", resp.header("ETag"));
        Patient body = resp.as(Patient.class);
        assertEquals("2", body.getMeta().getVersionId());

        Patient r = read("Patient", id, Patient.class);
        assertEquals("2", r.getMeta().getVersionId());
        assertEquals(fam + "x", r.getNameFirstRep().getFamily());
        assertEquals("New", r.getNameFirstRep().getGiven().get(0).getValue());
        assertEquals(AdministrativeGender.FEMALE, r.getGender());
    }

    @Test
    void updateWithMismatchedIdIs400Invalid() {
        String id = create("Patient", patient("Mis" + uniq()));
        Patient p = patient("Mis");
        p.setId(id + "0");
        assertOutcome(put("/Patient/" + id, p), 400, "invalid");
    }

    @Test
    void updateWithoutIdIs400Invalid() {
        String id = create("Patient", patient("NoId" + uniq()));
        assertOutcome(put("/Patient/" + id, patient("NoId")), 400, "invalid");
    }

    @Test
    void updateOfUnknownIdIs404() {
        Patient p = patient("Ghost");
        p.setId("987654321");
        assertOutcome(put("/Patient/987654321", p), 404, "not-found");
    }

    @Test
    void readOfUnknownIdIs404() {
        assertOutcome(get("/Patient/987654321"), 404, "not-found");
    }

    @Test
    void r1_patientWithoutFamilyOrIdentifierIs422Required() {
        Patient p = new Patient();
        p.addName().addGiven("OnlyGiven");
        assertOutcome(post("Patient", p), 422, "required");
    }

    @Test
    void r1_identifierAloneIsEnough() {
        Patient p = new Patient();
        p.addIdentifier().setSystem("urn:test").setValue("only-" + uniq());
        create("Patient", p);
    }

    @Test
    void r2_futureBirthDateIs422BusinessRule() {
        Patient p = patient("Future" + uniq());
        p.setBirthDateElement(new DateType(LocalDate.now().plusDays(10).toString()));
        assertOutcome(post("Patient", p), 422, "business-rule");
    }

    @Test
    void r2_appliesOnUpdateToo() {
        String id = create("Patient", patient("FutUpd" + uniq()));
        Patient p = patient("FutUpd");
        p.setId(id);
        p.setBirthDateElement(new DateType(LocalDate.now().plusYears(1).toString()));
        assertOutcome(put("/Patient/" + id, p), 422, "business-rule");
    }

    @Test
    void searchByFamilyIsCaseInsensitivePrefix() {
        String u = uniq();
        String a = create("Patient", patient("Kowal" + u + "ski"));
        String b = create("Patient", patient("KOWAL" + u + "czyk"));
        create("Patient", patient("Nowak" + u));
        Bundle bundle = search("Patient", "family=kowal" + u);
        assertEquals(2, bundle.getTotal());
        assertEquals(List.of(a, b), ids(bundle));
    }

    @Test
    void searchByGivenMatchesAnyGivenName() {
        String u = uniq();
        String fam = "Giv" + u;
        String a = create("Patient", patient(fam, "Zofia", "Ewa" + u));
        create("Patient", patient(fam, "Zofia"));
        assertEquals(List.of(a), ids(search("Patient", "given=ewa" + u)));
    }

    @Test
    void repeatedParameterIsAnd() {
        String u = uniq();
        String fam = "And" + u;
        String both = create("Patient", patient(fam, "Alpha" + u, "Beta" + u));
        create("Patient", patient(fam, "Alpha" + u));
        assertEquals(List.of(both), ids(search("Patient", "given=Alpha" + u, "given=Beta" + u)));
    }

    @Test
    void searchByIdentifierSystemAndValueOrValueOnly() {
        String v = "ID-" + uniq();
        Patient p1 = patient("Ident");
        p1.addIdentifier().setSystem("urn:sys:a").setValue(v);
        String a = create("Patient", p1);
        Patient p2 = patient("Ident");
        p2.addIdentifier().setSystem("urn:sys:b").setValue(v);
        String b = create("Patient", p2);

        assertEquals(List.of(a), ids(search("Patient", "identifier=urn:sys:a|" + v)));
        assertEquals(List.of(a, b), ids(search("Patient", "identifier=" + v)));
        assertEquals(0, search("Patient", "identifier=urn:sys:c|" + v).getTotal());
    }

    @Test
    void searchByGenderCombinesWithOtherParams() {
        String fam = "Gen" + uniq();
        Patient f = patient(fam);
        f.setGender(AdministrativeGender.FEMALE);
        String a = create("Patient", f);
        Patient m = patient(fam);
        m.setGender(AdministrativeGender.MALE);
        create("Patient", m);
        assertEquals(List.of(a), ids(search("Patient", "family=" + fam, "gender=female")));
    }

    @Test
    void searchByBirthdateSupportsPrefixes() {
        String fam = "Bd" + uniq();
        Patient p1 = patient(fam);
        p1.setBirthDateElement(new DateType("1980-01-01"));
        String a = create("Patient", p1);
        Patient p2 = patient(fam);
        p2.setBirthDateElement(new DateType("1990-06-15"));
        String b = create("Patient", p2);

        assertEquals(List.of(a), ids(search("Patient", "family=" + fam, "birthdate=1980-01-01")));
        assertEquals(List.of(a), ids(search("Patient", "family=" + fam, "birthdate=eq1980-01-01")));
        assertEquals(List.of(b), ids(search("Patient", "family=" + fam, "birthdate=ne1980-01-01")));
        assertEquals(List.of(b), ids(search("Patient", "family=" + fam, "birthdate=gt1980-01-01")));
        assertEquals(List.of(a, b), ids(search("Patient", "family=" + fam, "birthdate=ge1980-01-01")));
        assertEquals(List.of(a), ids(search("Patient", "family=" + fam, "birthdate=lt1990-06-15")));
        assertEquals(List.of(a, b), ids(search("Patient", "family=" + fam, "birthdate=le1990-06-15")));
        assertEquals(List.of(b), ids(search("Patient", "family=" + fam, "birthdate=gt1985-01-01", "birthdate=lt1995-01-01")));
    }

    @Test
    void searchBundleShape() {
        String fam = "Shape" + uniq();
        String a = create("Patient", patient(fam));
        Bundle b = search("Patient", "family=" + fam);
        assertEquals(1, b.getTotal());
        Bundle.BundleEntryComponent e = b.getEntryFirstRep();
        assertTrue(e.getFullUrl().startsWith("http"), e.getFullUrl());
        assertTrue(e.getFullUrl().endsWith("/fhir/Patient/" + a), e.getFullUrl());
        assertEquals(Bundle.SearchEntryMode.MATCH, e.getSearch().getMode());
        assertEquals(a, e.getResource().getIdElement().getIdPart());
    }

    @Test
    void searchWithoutParamsReturnsAll() {
        String a = create("Patient", patient("All" + uniq()));
        Bundle b = search("Patient");
        assertTrue(ids(b).contains(a));
        assertEquals(b.getEntry().size(), b.getTotal());
    }

    @Test
    void unknownSearchParamIs400NotSupported() {
        assertOutcome(get("/Patient?shoeSize=42"), 400, "not-supported");
        assertOutcome(get("/Patient?_count=10"), 400, "not-supported");
    }

    @Test
    void malformedDateParamIs400Invalid() {
        assertOutcome(get("/Patient?birthdate=yesterday"), 400, "invalid");
    }
}
