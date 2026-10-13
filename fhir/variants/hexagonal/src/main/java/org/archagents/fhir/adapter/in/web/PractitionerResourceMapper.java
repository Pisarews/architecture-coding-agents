package org.archagents.fhir.adapter.in.web;

import static org.archagents.fhir.adapter.in.web.FhirConversions.applyMeta;

import org.archagents.fhir.domain.Practitioner;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.stereotype.Component;

@Component
class PractitionerResourceMapper {

    Practitioner toDomain(org.hl7.fhir.r4.model.Practitioner resource) {
        return new Practitioner(
                resource.hasActive() ? resource.getActive() : null,
                FhirConversions.toHumanName(resource.getName()),
                FhirConversions.toIdentifiers(resource.getIdentifier()));
    }

    org.hl7.fhir.r4.model.Practitioner toResource(StoredResource<Practitioner> stored) {
        Practitioner practitioner = stored.content();
        org.hl7.fhir.r4.model.Practitioner resource = new org.hl7.fhir.r4.model.Practitioner();
        applyMeta(resource, stored);
        practitioner.identifiers().forEach(i -> resource.addIdentifier(FhirConversions.toFhirIdentifier(i)));
        if (practitioner.active() != null) {
            resource.setActive(practitioner.active());
        }
        if (practitioner.name() != null) {
            resource.addName(FhirConversions.toFhirHumanName(practitioner.name()));
        }
        return resource;
    }
}
