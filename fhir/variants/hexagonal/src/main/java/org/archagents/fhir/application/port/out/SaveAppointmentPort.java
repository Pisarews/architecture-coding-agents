package org.archagents.fhir.application.port.out;

import org.archagents.fhir.domain.Appointment;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.StoredResource;

public interface SaveAppointmentPort {

    StoredResource<Appointment> createAppointment(Appointment appointment, ResourceMeta meta);

    void updateAppointment(StoredResource<Appointment> appointment);
}
