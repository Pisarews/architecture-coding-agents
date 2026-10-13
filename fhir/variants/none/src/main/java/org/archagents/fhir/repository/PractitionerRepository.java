package org.archagents.fhir.repository;

import org.archagents.fhir.entity.PractitionerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PractitionerRepository extends JpaRepository<PractitionerEntity, Long>, JpaSpecificationExecutor<PractitionerEntity> {
}
