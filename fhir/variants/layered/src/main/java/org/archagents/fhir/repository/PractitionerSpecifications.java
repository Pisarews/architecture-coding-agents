package org.archagents.fhir.repository;

import org.archagents.fhir.entity.PractitionerEntity;
import org.springframework.data.jpa.domain.Specification;

public final class PractitionerSpecifications {

    private PractitionerSpecifications() {
    }

    public static Specification<PractitionerEntity> familyStartsWith(String prefix) {
        return (root, query, cb) -> SpecificationSupport.startsWithIgnoreCase(cb, root.get("family"), prefix);
    }

    public static Specification<PractitionerEntity> hasIdentifier(String system, String value) {
        return (root, query, cb) -> {
            query.distinct(true);
            return SpecificationSupport.identifierMatches(cb, root, system, value);
        };
    }
}
