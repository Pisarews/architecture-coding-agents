package org.archagents.fhir.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface EncounterRepository extends JpaRepository<EncounterJpaEntity, Long>, JpaSpecificationExecutor<EncounterJpaEntity> {
}
