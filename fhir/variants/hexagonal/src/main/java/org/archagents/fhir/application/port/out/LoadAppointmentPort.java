package org.archagents.fhir.application.port.out;

import java.util.List;
import java.util.Optional;
import org.archagents.fhir.domain.Appointment;
import org.archagents.fhir.domain.AppointmentSearchCriteria;
import org.archagents.fhir.domain.StoredResource;

public interface LoadAppointmentPort {

    Optional<StoredResource<Appointment>> loadAppointment(long id);

    List<StoredResource<Appointment>> searchAppointments(AppointmentSearchCriteria criteria);
}
