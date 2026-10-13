package org.archagents.fhir.repository;

import org.archagents.fhir.entity.ObservationEntity;
import org.springframework.data.jpa.domain.Specification;

public final class ObservationSpecifications {

    private ObservationSpecifications() {
    }

    public static Specification<ObservationEntity> hasSubject(long patientId) {
        return (root, query, cb) -> cb.equal(root.get("subjectId"), patientId);
    }

    public static Specification<ObservationEntity> hasCode(String system, String code) {
        return (root, query, cb) -> {
            if (system == null) {
                return cb.equal(root.get("code"), code);
            }
            var systemMatches = system.isEmpty()
                    ? cb.isNull(root.get("codeSystem"))
                    : cb.equal(root.get("codeSystem"), system);
            return cb.and(systemMatches, cb.equal(root.get("code"), code));
        };
    }

    public static Specification<ObservationEntity> hasCategory(String code) {
        return (root, query, cb) -> cb.equal(root.get("categoryCode"), code);
    }
}
