package org.archagents.fhir.repository;

import org.archagents.fhir.entity.EncounterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EncounterRepository extends JpaRepository<EncounterEntity, Long>, JpaSpecificationExecutor<EncounterEntity> {
}
