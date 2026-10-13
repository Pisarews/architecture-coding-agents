package org.archagents.fhir.adapter.out.persistence;

import org.archagents.fhir.application.port.out.ResourceExistencePort;
import org.archagents.fhir.domain.ResourceType;
import org.springframework.stereotype.Component;

@Component
class ResourceExistenceAdapter implements ResourceExistencePort {

    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final EncounterRepository encounterRepository;
    private final ObservationRepository observationRepository;
    private final AppointmentRepository appointmentRepository;

    ResourceExistenceAdapter(
            PatientRepository patientRepository,
            PractitionerRepository practitionerRepository,
            EncounterRepository encounterRepository,
            ObservationRepository observationRepository,
            AppointmentRepository appointmentRepository) {
        this.patientRepository = patientRepository;
        this.practitionerRepository = practitionerRepository;
        this.encounterRepository = encounterRepository;
        this.observationRepository = observationRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public boolean exists(ResourceType type, long id) {
        return switch (type) {
            case PATIENT -> patientRepository.existsById(id);
            case PRACTITIONER -> practitionerRepository.existsById(id);
            case ENCOUNTER -> encounterRepository.existsById(id);
            case OBSERVATION -> observationRepository.existsById(id);
            case APPOINTMENT -> appointmentRepository.existsById(id);
        };
    }
}
