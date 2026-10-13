package org.archagents.fhir.adapter.out.persistence;

import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.allOf;
import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.anyElement;
import static org.archagents.fhir.adapter.out.persistence.SearchPredicates.matchesDay;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.archagents.fhir.application.port.out.LoadAppointmentPort;
import org.archagents.fhir.application.port.out.SaveAppointmentPort;
import org.archagents.fhir.domain.Appointment;
import org.archagents.fhir.domain.AppointmentSearchCriteria;
import org.archagents.fhir.domain.DateCriterion;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.ResourceType;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
class AppointmentPersistenceAdapter implements LoadAppointmentPort, SaveAppointmentPort {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentPersistenceMapper appointmentMapper;

    AppointmentPersistenceAdapter(
            AppointmentRepository appointmentRepository, AppointmentPersistenceMapper appointmentMapper) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentMapper = appointmentMapper;
    }

    @Override
    public Optional<StoredResource<Appointment>> loadAppointment(long id) {
        return appointmentRepository.findById(id).map(appointmentMapper::toDomain);
    }

    @Override
    public List<StoredResource<Appointment>> searchAppointments(AppointmentSearchCriteria criteria) {
        return appointmentRepository.findAll(matching(criteria), Sort.by("id")).stream()
                .map(appointmentMapper::toDomain)
                .toList();
    }

    @Override
    public StoredResource<Appointment> createAppointment(Appointment appointment, ResourceMeta meta) {
        AppointmentJpaEntity entity = new AppointmentJpaEntity();
        appointmentMapper.copyToEntity(appointment, meta, entity);
        return appointmentMapper.toDomain(appointmentRepository.save(entity));
    }

    @Override
    public void updateAppointment(StoredResource<Appointment> appointment) {
        AppointmentJpaEntity entity = appointmentRepository.findById(appointment.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Appointment/" + appointment.id() + " vanished during update"));
        appointmentMapper.copyToEntity(appointment.content(), appointment.meta(), entity);
        appointmentRepository.save(entity);
    }

    private static Specification<AppointmentJpaEntity> matching(AppointmentSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Long patientId : criteria.patientIds()) {
                predicates.add(hasActor(root, query, cb, ResourceType.PATIENT, patientId));
            }
            for (Long practitionerId : criteria.practitionerIds()) {
                predicates.add(hasActor(root, query, cb, ResourceType.PRACTITIONER, practitionerId));
            }
            for (String status : criteria.statuses()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            for (DateCriterion date : criteria.startDates()) {
                predicates.add(matchesDay(cb, root.get("startTime"), date));
            }
            return allOf(cb, predicates);
        };
    }

    private static Predicate hasActor(
            Root<AppointmentJpaEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb, ResourceType type, long id) {
        return anyElement(root, query, cb, "participants",
                (Join<AppointmentJpaEntity, EmbeddedParticipant> participant) -> cb.and(
                        cb.equal(participant.get("actorType"), type.typeName()),
                        cb.equal(participant.get("actorId"), id)));
    }
}
