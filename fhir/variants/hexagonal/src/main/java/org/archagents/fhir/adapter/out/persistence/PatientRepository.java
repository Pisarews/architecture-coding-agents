package org.archagents.fhir.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface PatientRepository extends JpaRepository<PatientJpaEntity, Long>, JpaSpecificationExecutor<PatientJpaEntity> {
}
