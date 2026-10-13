package org.archagents.fhir.repository;

import jakarta.persistence.criteria.Join;
import org.archagents.fhir.entity.PatientEntity;
import org.springframework.data.jpa.domain.Specification;

public final class PatientSpecifications {

    private PatientSpecifications() {
    }

    public static Specification<PatientEntity> familyStartsWith(String prefix) {
        return (root, query, cb) -> SpecificationSupport.startsWithIgnoreCase(cb, root.get("family"), prefix);
    }

    public static Specification<PatientEntity> givenStartsWith(String prefix) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<PatientEntity, String> given = root.join("given");
            return SpecificationSupport.startsWithIgnoreCase(cb, given, prefix);
        };
    }

    public static Specification<PatientEntity> hasIdentifier(String system, String value) {
        return (root, query, cb) -> {
            query.distinct(true);
            return SpecificationSupport.identifierMatches(cb, root, system, value);
        };
    }

    public static Specification<PatientEntity> hasGender(String gender) {
        return (root, query, cb) -> cb.equal(root.get("gender"), gender);
    }

    public static Specification<PatientEntity> birthDate(DateCriterion criterion) {
        return (root, query, cb) -> SpecificationSupport.matchesDay(cb, root.get("birthDate"),
                criterion.comparator(), criterion.date(), criterion.date().plusDays(1));
    }
}
