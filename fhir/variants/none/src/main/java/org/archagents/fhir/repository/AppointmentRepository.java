package org.archagents.fhir.repository;

import java.time.Instant;
import org.archagents.fhir.entity.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long>, JpaSpecificationExecutor<AppointmentEntity> {

    @Query("""
            select count(a) > 0 from AppointmentEntity a join a.participants p
            where a.status = 'booked'
              and p.actorType = 'Practitioner' and p.actorId = :practitionerId
              and a.start < :end and a.end > :start
              and (:excludeId is null or a.id <> :excludeId)
            """)
    boolean existsOverlappingBooking(@Param("practitionerId") Long practitionerId,
                                     @Param("start") Instant start,
                                     @Param("end") Instant end,
                                     @Param("excludeId") Long excludeId);
}
