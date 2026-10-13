package org.archagents.fhir.domain;

import java.time.LocalDate;
import java.util.List;

public record Patient(
        Boolean active,
        HumanName name,
        List<Identifier> identifiers,
        List<ContactPoint> telecoms,
        Gender gender,
        LocalDate birthDate) {

    public Patient {
        identifiers = List.copyOf(identifiers);
        telecoms = List.copyOf(telecoms);
    }

    public void validate(LocalDate today) {
        boolean hasFamily = name != null && name.hasFamily();
        boolean hasIdentifierValue = identifiers.stream().anyMatch(Identifier::hasValue);
        if (!hasFamily && !hasIdentifierValue) {
            throw RuleViolationException.required("Patient requires name.family or an identifier with a value");
        }
        if (birthDate != null && birthDate.isAfter(today)) {
            throw RuleViolationException.businessRule("Patient.birthDate must not be in the future");
        }
    }
}
