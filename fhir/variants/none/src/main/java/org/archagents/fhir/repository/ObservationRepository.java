package org.archagents.fhir.repository;

import org.archagents.fhir.entity.ObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ObservationRepository extends JpaRepository<ObservationEntity, Long>, JpaSpecificationExecutor<ObservationEntity> {
}
