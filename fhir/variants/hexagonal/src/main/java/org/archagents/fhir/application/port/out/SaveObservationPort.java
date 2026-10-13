package org.archagents.fhir.application.port.out;

import org.archagents.fhir.domain.Observation;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.StoredResource;

public interface SaveObservationPort {

    StoredResource<Observation> createObservation(Observation observation, ResourceMeta meta);

    void updateObservation(StoredResource<Observation> observation);
}
