package org.archagents.fhir.application.port.in;

import java.util.List;
import org.archagents.fhir.domain.Encounter;
import org.archagents.fhir.domain.EncounterSearchCriteria;
import org.archagents.fhir.domain.StoredResource;

public interface FindEncountersQuery {

    StoredResource<Encounter> getEncounter(long id);

    List<StoredResource<Encounter>> searchEncounters(EncounterSearchCriteria criteria);
}
