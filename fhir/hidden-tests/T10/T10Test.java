package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

/** T10: Patient.address is stored; search parameter address-city. */
class T10Test {

    static Address address(String city, String postalCode, String country, String... lines) {
        Address a = new Address();
        for (String l : lines) a.addLine(l);
        if (city != null) a.setCity(city);
        if (postalCode != null) a.setPostalCode(postalCode);
        if (country != null) a.setCountry(country);
        return a;
    }

    /** Persisted address elements in a comparable form. */
    static List<String> flat(Patient p) {
        return p.getAddress().stream()
                .map(a -> a.getLine().stream().map(StringType::getValue).toList() + "|" + a.getCity() + "|"
                        + a.getPostalCode() + "|" + a.getCountry())
                .toList();
    }

    static Patient patient(String fam, Address... addresses) {
        Patient p = PatientTest.patient(fam);
        for (Address a : addresses) p.addAddress(a);
        return p;
    }

    @Test
    void addressesRoundTripOnCreateAndRead() {
        String u = uniq();
        Patient p = patient("Addr" + u,
                address("Gdansk" + u, "80-001", "PL", "ul. Dluga 1", "m. 2"),
                address("Sopot" + u, null, null));
        String id = create("Patient", p);
        assertEquals(List.of("[ul. Dluga 1, m. 2]|Gdansk" + u + "|80-001|PL", "[]|Sopot" + u + "|null|null"),
                flat(read("Patient", id, Patient.class)));
    }

    @Test
    void addressesRoundTripOnUpdate() {
        String u = uniq();
        String fam = "AddrUpd" + u;
        String id = create("Patient", patient(fam, address("Lodz" + u, "90-001", "PL", "Piotrkowska 1")));

        Patient v2 = patient(fam, address("Poznan" + u, "60-001", "PL", "Stary Rynek 1", "II p.", "lok. 3"));
        v2.setId(id);
        Resp r = put("/Patient/" + id, v2);
        assertEquals(200, r.status(), r.body());
        assertEquals(List.of("[Stary Rynek 1, II p., lok. 3]|Poznan" + u + "|60-001|PL"), flat(read("Patient", id, Patient.class)));

        Patient v3 = patient(fam);
        v3.setId(id);
        assertEquals(200, put("/Patient/" + id, v3).status());
        assertEquals(List.of(), flat(read("Patient", id, Patient.class)));
    }

    @Test
    void searchByCityIsCaseInsensitivePrefixOnAnyAddress() {
        String u = uniq();
        String a = create("Patient", patient("CityA" + u, address("KRAK" + u.toUpperCase() + "OW", "30-001", "PL")));
        String b = create("Patient", patient("CityB" + u,
                address("Gdansk" + u, null, "PL"),
                address("krak" + u + " Nowa Huta", null, "PL")));
        create("Patient", patient("CityC" + u, address("Warszawa" + u, null, "PL")));
        create("Patient", patient("CityD" + u));

        assertEquals(List.of(a, b), ids(search("Patient", "address-city=Krak" + u)));
        assertEquals(List.of(a), ids(search("Patient", "address-city=krak" + u + "o")));
        assertEquals(List.of(b), ids(search("Patient", "address-city=gdansk" + u)));
        assertEquals(List.of(), ids(search("Patient", "address-city=rak" + u)));
    }

    @Test
    void searchByCityCombinesWithOtherParameters() {
        String u = uniq();
        String fam = "CityFam" + u;
        Patient f = patient(fam, address("Torun" + u, null, null));
        f.setGender(AdministrativeGender.FEMALE);
        String a = create("Patient", f);
        Patient m = patient(fam, address("Torun" + u, null, null));
        m.setGender(AdministrativeGender.MALE);
        String b = create("Patient", m);
        String c = create("Patient", patient("Other" + u, address("Torun" + u, null, null)));

        assertEquals(List.of(a, b, c), ids(search("Patient", "address-city=torun" + u)));
        assertEquals(List.of(a, b), ids(search("Patient", "address-city=torun" + u, "family=" + fam)));
        assertEquals(List.of(a), ids(search("Patient", "address-city=TORUN" + u, "family=" + fam, "gender=female")));
    }

    @Test
    void capabilityStatementListsAddressCity() {
        CapabilityStatement cs = get("/metadata").as(CapabilityStatement.class);
        Set<String> params = cs.getRestFirstRep().getResource().stream()
                .filter(r -> r.getType().equals("Patient"))
                .flatMap(r -> r.getSearchParam().stream())
                .map(p -> p.getName() + ":" + p.getType().toCode())
                .collect(Collectors.toSet());
        assertTrue(params.contains("address-city:string"), params.toString());
    }

    @Test
    void patientWithoutAddressStillRoundTrips() {
        String id = create("Patient", PatientTest.patient("NoAddr" + uniq()));
        assertEquals(List.of(), flat(read("Patient", id, Patient.class)));
    }
}
