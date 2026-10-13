package org.archagents.fhir.application.port.in;

import org.archagents.fhir.domain.Patient;
import org.archagents.fhir.domain.StoredResource;

public interface SavePatientUseCase {

    StoredResource<Patient> createPatient(Patient patient);

    StoredResource<Patient> updatePatient(long id, Patient patient);
}
