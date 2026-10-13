package org.archagents.fhir.config;

import java.time.Clock;
import org.archagents.fhir.application.port.out.LoadAppointmentPort;
import org.archagents.fhir.application.port.out.LoadEncounterPort;
import org.archagents.fhir.application.port.out.LoadObservationPort;
import org.archagents.fhir.application.port.out.LoadPatientPort;
import org.archagents.fhir.application.port.out.LoadPractitionerPort;
import org.archagents.fhir.application.port.out.ResourceExistencePort;
import org.archagents.fhir.application.port.out.SaveAppointmentPort;
import org.archagents.fhir.application.port.out.SaveEncounterPort;
import org.archagents.fhir.application.port.out.SaveObservationPort;
import org.archagents.fhir.application.port.out.SavePatientPort;
import org.archagents.fhir.application.port.out.SavePractitionerPort;
import org.archagents.fhir.application.service.AppointmentService;
import org.archagents.fhir.application.service.EncounterService;
import org.archagents.fhir.application.service.ObservationService;
import org.archagents.fhir.application.service.PatientService;
import org.archagents.fhir.application.service.PractitionerService;
import org.archagents.fhir.application.service.ReferenceVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class UseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ReferenceVerifier referenceVerifier(ResourceExistencePort resourceExistencePort) {
        return new ReferenceVerifier(resourceExistencePort);
    }

    @Bean
    PatientService patientService(LoadPatientPort loadPatientPort, SavePatientPort savePatientPort, Clock clock) {
        return new PatientService(loadPatientPort, savePatientPort, clock);
    }

    @Bean
    PractitionerService practitionerService(
            LoadPractitionerPort loadPractitionerPort, SavePractitionerPort savePractitionerPort, Clock clock) {
        return new PractitionerService(loadPractitionerPort, savePractitionerPort, clock);
    }

    @Bean
    EncounterService encounterService(
            LoadEncounterPort loadEncounterPort,
            SaveEncounterPort saveEncounterPort,
            ReferenceVerifier referenceVerifier,
            Clock clock) {
        return new EncounterService(loadEncounterPort, saveEncounterPort, referenceVerifier, clock);
    }

    @Bean
    ObservationService observationService(
            LoadObservationPort loadObservationPort,
            SaveObservationPort saveObservationPort,
            ReferenceVerifier referenceVerifier,
            Clock clock) {
        return new ObservationService(loadObservationPort, saveObservationPort, referenceVerifier, clock);
    }

    @Bean
    AppointmentService appointmentService(
            LoadAppointmentPort loadAppointmentPort,
            SaveAppointmentPort saveAppointmentPort,
            ReferenceVerifier referenceVerifier,
            Clock clock) {
        return new AppointmentService(loadAppointmentPort, saveAppointmentPort, referenceVerifier, clock);
    }
}
