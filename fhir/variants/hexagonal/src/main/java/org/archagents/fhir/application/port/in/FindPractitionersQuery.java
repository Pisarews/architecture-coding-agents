package org.archagents.fhir.application.port.in;

import java.util.List;
import org.archagents.fhir.domain.Practitioner;
import org.archagents.fhir.domain.PractitionerSearchCriteria;
import org.archagents.fhir.domain.StoredResource;

public interface FindPractitionersQuery {

    StoredResource<Practitioner> getPractitioner(long id);

    List<StoredResource<Practitioner>> searchPractitioners(PractitionerSearchCriteria criteria);
}
