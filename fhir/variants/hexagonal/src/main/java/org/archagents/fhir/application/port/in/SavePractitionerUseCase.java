package org.archagents.fhir.application.port.in;

import org.archagents.fhir.domain.Practitioner;
import org.archagents.fhir.domain.StoredResource;

public interface SavePractitionerUseCase {

    StoredResource<Practitioner> createPractitioner(Practitioner practitioner);

    StoredResource<Practitioner> updatePractitioner(long id, Practitioner practitioner);
}
