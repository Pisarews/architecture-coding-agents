package org.archagents.fhir.application.port.in;

import java.util.List;
import org.archagents.fhir.domain.Appointment;
import org.archagents.fhir.domain.AppointmentSearchCriteria;
import org.archagents.fhir.domain.StoredResource;

public interface FindAppointmentsQuery {

    StoredResource<Appointment> getAppointment(long id);

    List<StoredResource<Appointment>> searchAppointments(AppointmentSearchCriteria criteria);
}
