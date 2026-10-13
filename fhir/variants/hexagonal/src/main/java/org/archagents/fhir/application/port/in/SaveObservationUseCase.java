package org.archagents.fhir.application.port.in;

import org.archagents.fhir.domain.Observation;
import org.archagents.fhir.domain.StoredResource;

public interface SaveObservationUseCase {

    StoredResource<Observation> createObservation(Observation observation);

    StoredResource<Observation> updateObservation(long id, Observation observation);
}
