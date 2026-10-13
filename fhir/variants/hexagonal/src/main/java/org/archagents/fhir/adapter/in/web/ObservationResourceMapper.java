package org.archagents.fhir.adapter.in.web;

import static org.archagents.fhir.adapter.in.web.FhirConversions.applyMeta;
import static org.archagents.fhir.adapter.in.web.FhirConversions.code;
import static org.archagents.fhir.adapter.in.web.FhirConversions.toReference;

import org.archagents.fhir.domain.Coding;
import org.archagents.fhir.domain.Observation;
import org.archagents.fhir.domain.ObservationStatus;
import org.archagents.fhir.domain.Quantity;
import org.archagents.fhir.domain.StoredResource;
import org.springframework.stereotype.Component;

@Component
class ObservationResourceMapper {

    Observation toDomain(org.hl7.fhir.r4.model.Observation resource) {
        Coding category = null;
        if (resource.hasCategory() && resource.getCategoryFirstRep().hasCoding()) {
            org.hl7.fhir.r4.model.Coding coding = resource.getCategoryFirstRep().getCodingFirstRep();
            category = new Coding(coding.getSystem(), coding.getCode(), null);
        }
        Coding code = null;
        if (resource.hasCode() && resource.getCode().hasCoding()) {
            org.hl7.fhir.r4.model.Coding coding = resource.getCode().getCodingFirstRep();
            code = new Coding(coding.getSystem(), coding.getCode(), coding.getDisplay());
        }
        Quantity value = null;
        if (resource.getValue() instanceof org.hl7.fhir.r4.model.Quantity quantity && !quantity.isEmpty()) {
            value = new Quantity(quantity.getValue(), quantity.getUnit(), quantity.getSystem(), quantity.getCode());
        }
        return new Observation(
                resource.hasStatus() ? code(resource.getStatus().toCode(), ObservationStatus::fromCode) : null,
                category,
                code,
                toReference(resource.getSubject()),
                toReference(resource.getEncounter()),
                resource.hasEffectiveDateTimeType()
                        ? FhirConversions.toInstant(resource.getEffectiveDateTimeType())
                        : null,
                value);
    }

    org.hl7.fhir.r4.model.Observation toResource(StoredResource<Observation> stored) {
        Observation observation = stored.content();
        org.hl7.fhir.r4.model.Observation resource = new org.hl7.fhir.r4.model.Observation();
        applyMeta(resource, stored);
        resource.setStatus(
                org.hl7.fhir.r4.model.Observation.ObservationStatus.fromCode(observation.status().code()));
        if (observation.category() != null) {
            resource.addCategory().addCoding()
                    .setSystem(observation.category().system())
                    .setCode(observation.category().code());
        }
        resource.getCode().addCoding()
                .setSystem(observation.code().system())
                .setCode(observation.code().code())
                .setDisplay(observation.code().display());
        resource.setSubject(FhirConversions.toFhirReference(observation.subject()));
        if (observation.encounter() != null) {
            resource.setEncounter(FhirConversions.toFhirReference(observation.encounter()));
        }
        if (observation.effective() != null) {
            resource.setEffective(FhirConversions.toDateTime(observation.effective()));
        }
        Quantity value = observation.value();
        if (value != null) {
            resource.setValue(new org.hl7.fhir.r4.model.Quantity()
                    .setValue(value.value())
                    .setUnit(value.unit())
                    .setSystem(value.system())
                    .setCode(value.code()));
        }
        return resource;
    }
}
