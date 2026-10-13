package org.archagents.fhir.application.port.out;

import org.archagents.fhir.domain.Patient;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.StoredResource;

public interface SavePatientPort {

    StoredResource<Patient> createPatient(Patient patient, ResourceMeta meta);

    void updatePatient(StoredResource<Patient> patient);
}
