package org.archagents.fhir.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.archagents.fhir.application.port.in.FindPatientsQuery;
import org.archagents.fhir.application.port.in.ResourceNotFoundException;
import org.archagents.fhir.application.port.in.SavePatientUseCase;
import org.archagents.fhir.application.port.out.LoadPatientPort;
import org.archagents.fhir.application.port.out.SavePatientPort;
import org.archagents.fhir.domain.Patient;
import org.archagents.fhir.domain.PatientSearchCriteria;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.ResourceType;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.transaction.annotation.Transactional;

public class PatientService implements SavePatientUseCase, FindPatientsQuery {

    private final LoadPatientPort loadPatientPort;
    private final SavePatientPort savePatientPort;
    private final Clock clock;

    public PatientService(LoadPatientPort loadPatientPort, SavePatientPort savePatientPort, Clock clock) {
        this.loadPatientPort = loadPatientPort;
        this.savePatientPort = savePatientPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StoredResource<Patient> createPatient(Patient patient) {
        patient.validate(LocalDate.now(clock));
        return savePatientPort.createPatient(patient, ResourceMeta.initial(now()));
    }

    @Override
    @Transactional
    public StoredResource<Patient> updatePatient(long id, Patient patient) {
        StoredResource<Patient> current = getPatient(id);
        patient.validate(LocalDate.now(clock));
        StoredResource<Patient> updated = current.revise(patient, now());
        savePatientPort.updatePatient(updated);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public StoredResource<Patient> getPatient(long id) {
        return loadPatientPort.loadPatient(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.PATIENT, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredResource<Patient>> searchPatients(PatientSearchCriteria criteria) {
        return loadPatientPort.searchPatients(criteria);
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
