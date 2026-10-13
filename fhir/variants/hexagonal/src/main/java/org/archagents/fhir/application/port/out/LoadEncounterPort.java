package org.archagents.fhir.application.port.out;

import java.util.List;
import java.util.Optional;
import org.archagents.fhir.domain.Encounter;
import org.archagents.fhir.domain.EncounterSearchCriteria;
import org.archagents.fhir.domain.StoredResource;

public interface LoadEncounterPort {

    Optional<StoredResource<Encounter>> loadEncounter(long id);

    List<StoredResource<Encounter>> searchEncounters(EncounterSearchCriteria criteria);
}
