package org.archagents.fhir.domain;

import java.util.List;

public record AppointmentSearchCriteria(
        List<Long> patientIds,
        List<Long> practitionerIds,
        List<String> statuses,
        List<DateCriterion> startDates) {

    public AppointmentSearchCriteria {
        patientIds = List.copyOf(patientIds);
        practitionerIds = List.copyOf(practitionerIds);
        statuses = List.copyOf(statuses);
        startDates = List.copyOf(startDates);
    }

    public static AppointmentSearchCriteria bookedFor(long practitionerId) {
        return new AppointmentSearchCriteria(
                List.of(), List.of(practitionerId), List.of(AppointmentStatus.BOOKED.code()), List.of());
    }
}
