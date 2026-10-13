package org.archagents.fhir.application.port.out;

import java.util.List;
import java.util.Optional;
import org.archagents.fhir.domain.Observation;
import org.archagents.fhir.domain.ObservationSearchCriteria;
import org.archagents.fhir.domain.StoredResource;

public interface LoadObservationPort {

    Optional<StoredResource<Observation>> loadObservation(long id);

    List<StoredResource<Observation>> searchObservations(ObservationSearchCriteria criteria);
}
