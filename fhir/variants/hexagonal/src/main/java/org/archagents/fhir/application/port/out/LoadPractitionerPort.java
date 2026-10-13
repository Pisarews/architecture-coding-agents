package org.archagents.fhir.application.port.out;

import java.util.List;
import java.util.Optional;
import org.archagents.fhir.domain.Practitioner;
import org.archagents.fhir.domain.PractitionerSearchCriteria;
import org.archagents.fhir.domain.StoredResource;

public interface LoadPractitionerPort {

    Optional<StoredResource<Practitioner>> loadPractitioner(long id);

    List<StoredResource<Practitioner>> searchPractitioners(PractitionerSearchCriteria criteria);
}
