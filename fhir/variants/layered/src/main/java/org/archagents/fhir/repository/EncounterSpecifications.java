package org.archagents.fhir.repository;

import org.archagents.fhir.entity.EncounterEntity;
import org.springframework.data.jpa.domain.Specification;

public final class EncounterSpecifications {

    private EncounterSpecifications() {
    }

    public static Specification<EncounterEntity> hasSubject(long patientId) {
        return (root, query, cb) -> cb.equal(root.get("subjectId"), patientId);
    }

    public static Specification<EncounterEntity> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
