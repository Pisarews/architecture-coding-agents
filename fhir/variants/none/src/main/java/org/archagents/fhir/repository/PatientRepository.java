package org.archagents.fhir.repository;

import org.archagents.fhir.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PatientRepository extends JpaRepository<PatientEntity, Long>, JpaSpecificationExecutor<PatientEntity> {
}
