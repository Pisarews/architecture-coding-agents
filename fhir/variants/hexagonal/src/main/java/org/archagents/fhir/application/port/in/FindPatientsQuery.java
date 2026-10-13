package org.archagents.fhir.application.port.in;

import java.util.List;
import org.archagents.fhir.domain.Patient;
import org.archagents.fhir.domain.PatientSearchCriteria;
import org.archagents.fhir.domain.StoredResource;

public interface FindPatientsQuery {

    StoredResource<Patient> getPatient(long id);

    List<StoredResource<Patient>> searchPatients(PatientSearchCriteria criteria);
}
