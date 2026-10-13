package org.archagents.fhir.application.port.out;

import org.archagents.fhir.domain.Practitioner;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.StoredResource;

public interface SavePractitionerPort {

    StoredResource<Practitioner> createPractitioner(Practitioner practitioner, ResourceMeta meta);

    void updatePractitioner(StoredResource<Practitioner> practitioner);
}
