package org.archagents.fhir.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.archagents.fhir.application.port.in.FindEncountersQuery;
import org.archagents.fhir.application.port.in.ResourceNotFoundException;
import org.archagents.fhir.application.port.in.SaveEncounterUseCase;
import org.archagents.fhir.application.port.out.LoadEncounterPort;
import org.archagents.fhir.application.port.out.SaveEncounterPort;
import org.archagents.fhir.domain.Encounter;
import org.archagents.fhir.domain.EncounterSearchCriteria;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.ResourceType;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.transaction.annotation.Transactional;

public class EncounterService implements SaveEncounterUseCase, FindEncountersQuery {

    private final LoadEncounterPort loadEncounterPort;
    private final SaveEncounterPort saveEncounterPort;
    private final ReferenceVerifier references;
    private final Clock clock;

    public EncounterService(
            LoadEncounterPort loadEncounterPort,
            SaveEncounterPort saveEncounterPort,
            ReferenceVerifier references,
            Clock clock) {
        this.loadEncounterPort = loadEncounterPort;
        this.saveEncounterPort = saveEncounterPort;
        this.references = references;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StoredResource<Encounter> createEncounter(Encounter encounter) {
        validate(encounter);
        return saveEncounterPort.createEncounter(encounter, ResourceMeta.initial(now()));
    }

    @Override
    @Transactional
    public StoredResource<Encounter> updateEncounter(long id, Encounter encounter) {
        StoredResource<Encounter> current = getEncounter(id);
        validate(encounter);
        StoredResource<Encounter> updated = current.revise(encounter, now());
        saveEncounterPort.updateEncounter(updated);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public StoredResource<Encounter> getEncounter(long id) {
        return loadEncounterPort.loadEncounter(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.ENCOUNTER, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredResource<Encounter>> searchEncounters(EncounterSearchCriteria criteria) {
        return loadEncounterPort.searchEncounters(criteria);
    }

    private void validate(Encounter encounter) {
        encounter.checkRequiredElements();
        references.verify(encounter.subject(), "Encounter.subject", ResourceType.PATIENT);
        if (encounter.practitioner() != null) {
            references.verify(encounter.practitioner(), "Encounter.participant.individual", ResourceType.PRACTITIONER);
        }
        encounter.checkPeriod();
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
