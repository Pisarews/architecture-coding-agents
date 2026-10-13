package org.archagents.fhir.acceptance;

import static org.archagents.fhir.acceptance.Fhir.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.Test;

class P2Test {

    static Practitioner prac(String family, String given) {
        Practitioner p = new Practitioner();
        p.addName().setFamily(family).addGiven(given);
        return p;
    }

    @Test
    void searchByGivenPrefixCaseInsensitive() {
        String u = uniq();
        String a = create("Practitioner", prac("Fam" + u, "Wojciech" + u));
        String b = create("Practitioner", prac("Other" + u, "WOJCIECH" + u + "x"));
        create("Practitioner", prac("Fam" + u, "Adam" + u));
        assertEquals(List.of(a, b), ids(search("Practitioner", "given=wojciech" + u)));
    }

    @Test
    void combinesWithFamily() {
        String u = uniq();
        String a = create("Practitioner", prac("Fam" + u, "Ola" + u));
        create("Practitioner", prac("Else" + u, "Ola" + u));
        assertEquals(List.of(a), ids(search("Practitioner", "given=ola" + u, "family=fam" + u)));
    }

    @Test
    void capabilityStatementListsGiven() {
        CapabilityStatement cs = get("/metadata").as(CapabilityStatement.class);
        Set<String> params = cs.getRestFirstRep().getResource().stream()
                .filter(r -> r.getType().equals("Practitioner"))
                .flatMap(r -> r.getSearchParam().stream())
                .map(p -> p.getName() + ":" + p.getType().toCode())
                .collect(Collectors.toSet());
        assertEquals(Set.of("family:string", "identifier:token", "given:string"), params);
    }
}
