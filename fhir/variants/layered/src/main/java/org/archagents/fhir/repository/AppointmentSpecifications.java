package org.archagents.fhir.repository;

import jakarta.persistence.criteria.Join;
import java.time.Instant;
import java.time.ZoneOffset;
import org.archagents.fhir.entity.AppointmentEntity;
import org.archagents.fhir.entity.AppointmentParticipant;
import org.springframework.data.jpa.domain.Specification;

public final class AppointmentSpecifications {

    private AppointmentSpecifications() {
    }

    public static Specification<AppointmentEntity> hasParticipant(String actorType, long actorId) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<AppointmentEntity, AppointmentParticipant> participant = root.join("participants");
            return cb.and(cb.equal(participant.get("actorType"), actorType),
                    cb.equal(participant.get("actorId"), actorId));
        };
    }

    public static Specification<AppointmentEntity> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<AppointmentEntity> startDate(DateCriterion criterion) {
        return (root, query, cb) -> {
            Instant dayStart = criterion.date().atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant nextDayStart = criterion.date().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            return SpecificationSupport.matchesDay(cb, root.get("start"), criterion.comparator(), dayStart, nextDayStart);
        };
    }
}
