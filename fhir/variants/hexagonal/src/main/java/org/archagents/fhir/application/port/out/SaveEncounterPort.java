package org.archagents.fhir.application.port.out;

import org.archagents.fhir.domain.Encounter;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.StoredResource;

public interface SaveEncounterPort {

    StoredResource<Encounter> createEncounter(Encounter encounter, ResourceMeta meta);

    void updateEncounter(StoredResource<Encounter> encounter);
}
