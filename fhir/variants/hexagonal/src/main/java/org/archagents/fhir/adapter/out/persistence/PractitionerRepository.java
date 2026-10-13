package org.archagents.fhir.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface PractitionerRepository extends JpaRepository<PractitionerJpaEntity, Long>, JpaSpecificationExecutor<PractitionerJpaEntity> {
}
