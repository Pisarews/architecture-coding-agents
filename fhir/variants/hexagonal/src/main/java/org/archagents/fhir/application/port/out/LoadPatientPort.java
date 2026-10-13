package org.archagents.fhir.application.port.out;

import java.util.List;
import java.util.Optional;
import org.archagents.fhir.domain.Patient;
import org.archagents.fhir.domain.PatientSearchCriteria;
import org.archagents.fhir.domain.StoredResource;

public interface LoadPatientPort {

    Optional<StoredResource<Patient>> loadPatient(long id);

    List<StoredResource<Patient>> searchPatients(PatientSearchCriteria criteria);
}
