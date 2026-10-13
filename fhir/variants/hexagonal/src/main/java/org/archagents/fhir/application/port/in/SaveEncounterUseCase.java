package org.archagents.fhir.application.port.in;

import org.archagents.fhir.domain.Encounter;
import org.archagents.fhir.domain.StoredResource;

public interface SaveEncounterUseCase {

    StoredResource<Encounter> createEncounter(Encounter encounter);

    StoredResource<Encounter> updateEncounter(long id, Encounter encounter);
}
