package org.archagents.fhir.application.port.in;

import java.util.List;
import org.archagents.fhir.domain.Observation;
import org.archagents.fhir.domain.ObservationSearchCriteria;
import org.archagents.fhir.domain.StoredResource;

public interface FindObservationsQuery {

    StoredResource<Observation> getObservation(long id);

    List<StoredResource<Observation>> searchObservations(ObservationSearchCriteria criteria);
}
