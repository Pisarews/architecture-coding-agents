package org.archagents.fhir.application.port.in;

import org.archagents.fhir.domain.Appointment;
import org.archagents.fhir.domain.StoredResource;

public interface SaveAppointmentUseCase {

    StoredResource<Appointment> createAppointment(Appointment appointment);

    StoredResource<Appointment> updateAppointment(long id, Appointment appointment);
}
