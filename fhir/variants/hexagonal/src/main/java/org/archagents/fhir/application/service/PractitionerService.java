package org.archagents.fhir.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.archagents.fhir.application.port.in.FindPractitionersQuery;
import org.archagents.fhir.application.port.in.ResourceNotFoundException;
import org.archagents.fhir.application.port.in.SavePractitionerUseCase;
import org.archagents.fhir.application.port.out.LoadPractitionerPort;
import org.archagents.fhir.application.port.out.SavePractitionerPort;
import org.archagents.fhir.domain.Practitioner;
import org.archagents.fhir.domain.PractitionerSearchCriteria;
import org.archagents.fhir.domain.ResourceMeta;
import org.archagents.fhir.domain.ResourceType;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.transaction.annotation.Transactional;

public class PractitionerService implements SavePractitionerUseCase, FindPractitionersQuery {

    private final LoadPractitionerPort loadPractitionerPort;
    private final SavePractitionerPort savePractitionerPort;
    private final Clock clock;

    public PractitionerService(
            LoadPractitionerPort loadPractitionerPort, SavePractitionerPort savePractitionerPort, Clock clock) {
        this.loadPractitionerPort = loadPractitionerPort;
        this.savePractitionerPort = savePractitionerPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StoredResource<Practitioner> createPractitioner(Practitioner practitioner) {
        practitioner.validate();
        return savePractitionerPort.createPractitioner(practitioner, ResourceMeta.initial(now()));
    }

    @Override
    @Transactional
    public StoredResource<Practitioner> updatePractitioner(long id, Practitioner practitioner) {
        StoredResource<Practitioner> current = getPractitioner(id);
        practitioner.validate();
        StoredResource<Practitioner> updated = current.revise(practitioner, now());
        savePractitionerPort.updatePractitioner(updated);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public StoredResource<Practitioner> getPractitioner(long id) {
        return loadPractitionerPort.loadPractitioner(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.PRACTITIONER, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredResource<Practitioner>> searchPractitioners(PractitionerSearchCriteria criteria) {
        return loadPractitionerPort.searchPractitioners(criteria);
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
