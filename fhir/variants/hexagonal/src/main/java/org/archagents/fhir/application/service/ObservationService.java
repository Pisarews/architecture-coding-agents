package org.archagents.fhir.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.archagents.fhir.application.port.in.FindObservationsQuery;
import org.archagents.fhir.application.port.in.ResourceNotFoundException;
import org.archagents.fhir.application.port.in.SaveObservationUseCase;
import org.archagents.fhir.application.port.out.LoadObservationPort;
import org.archagents.fhir.application.port.out.SaveObservationPort;
import org.archagents.fhir.domain.Observation;
import org.archagents.fhir.domain.ObservationSearchCriteria;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.ResourceType;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.transaction.annotation.Transactional;

public class ObservationService implements SaveObservationUseCase, FindObservationsQuery {

    private final LoadObservationPort loadObservationPort;
    private final SaveObservationPort saveObservationPort;
    private final ReferenceVerifier references;
    private final Clock clock;

    public ObservationService(
            LoadObservationPort loadObservationPort,
            SaveObservationPort saveObservationPort,
            ReferenceVerifier references,
            Clock clock) {
        this.loadObservationPort = loadObservationPort;
        this.saveObservationPort = saveObservationPort;
        this.references = references;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StoredResource<Observation> createObservation(Observation observation) {
        validate(observation);
        return saveObservationPort.createObservation(observation, ResourceMeta.initial(now()));
    }

    @Override
    @Transactional
    public StoredResource<Observation> updateObservation(long id, Observation observation) {
        StoredResource<Observation> current = getObservation(id);
        validate(observation);
        StoredResource<Observation> updated = current.revise(observation, now());
        saveObservationPort.updateObservation(updated);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public StoredResource<Observation> getObservation(long id) {
        return loadObservationPort.loadObservation(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.OBSERVATION, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredResource<Observation>> searchObservations(ObservationSearchCriteria criteria) {
        return loadObservationPort.searchObservations(criteria);
    }

    private void validate(Observation observation) {
        observation.checkRequiredElements();
        references.verify(observation.subject(), "Observation.subject", ResourceType.PATIENT);
        if (observation.encounter() != null) {
            references.verify(observation.encounter(), "Observation.encounter", ResourceType.ENCOUNTER);
        }
        observation.checkValue();
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
