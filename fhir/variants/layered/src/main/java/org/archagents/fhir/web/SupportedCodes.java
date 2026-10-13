package org.archagents.fhir.web;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;

/**
 * The subset of R4 codes this server accepts for coded elements whose value sets it narrows.
 */
final class SupportedCodes {

    private static final Set<String> TELECOM_SYSTEMS = Set.of("phone", "email");
    private static final Set<String> ENCOUNTER_STATUSES = Set.of("planned", "in-progress", "finished", "cancelled");
    private static final Set<String> OBSERVATION_STATUSES =
            Set.of("registered", "preliminary", "final", "amended", "cancelled", "entered-in-error");
    private static final Set<String> APPOINTMENT_STATUSES =
            Set.of("proposed", "booked", "cancelled", "fulfilled", "noshow");
    private static final Set<String> PARTICIPATION_STATUSES =
            Set.of("accepted", "declined", "tentative", "needs-action");

    private SupportedCodes() {
    }

    /**
     * Returns a description of the first unsupported code in the resource, if any.
     */
    static Optional<String> findUnsupported(IBaseResource resource) {
        return switch (resource) {
            case Patient patient -> check("Patient.telecom.system", TELECOM_SYSTEMS, patient.getTelecom().stream()
                    .filter(ContactPoint::hasSystem)
                    .map(telecom -> telecom.getSystem().toCode()));
            case Encounter encounter -> check("Encounter.status", ENCOUNTER_STATUSES,
                    Stream.ofNullable(encounter.hasStatus() ? encounter.getStatus().toCode() : null));
            case Observation observation -> check("Observation.status", OBSERVATION_STATUSES,
                    Stream.ofNullable(observation.hasStatus() ? observation.getStatus().toCode() : null));
            case Appointment appointment -> check("Appointment.status", APPOINTMENT_STATUSES,
                    Stream.ofNullable(appointment.hasStatus() ? appointment.getStatus().toCode() : null))
                    .or(() -> check("Appointment.participant.status", PARTICIPATION_STATUSES,
                            appointment.getParticipant().stream()
                                    .filter(Appointment.AppointmentParticipantComponent::hasStatus)
                                    .map(participant -> participant.getStatus().toCode())));
            default -> Optional.empty();
        };
    }

    private static Optional<String> check(String element, Set<String> supported, Stream<String> codes) {
        return codes.filter(code -> !supported.contains(code))
                .findFirst()
                .map(code -> "Code '" + code + "' is not supported for " + element);
    }
}
