package org.archagents.fhir.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface AppointmentRepository extends JpaRepository<AppointmentJpaEntity, Long>, JpaSpecificationExecutor<AppointmentJpaEntity> {
}
