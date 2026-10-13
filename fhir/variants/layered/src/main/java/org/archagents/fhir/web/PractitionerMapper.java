package org.archagents.fhir.web;

import org.archagents.fhir.entity.IdentifierValue;
import org.archagents.fhir.entity.PractitionerEntity;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;

@Component
public class PractitionerMapper {

    public PractitionerEntity toEntity(Practitioner practitioner) {
        PractitionerEntity entity = new PractitionerEntity();
        entity.setActive(practitioner.hasActive() ? practitioner.getActive() : null);
        if (practitioner.hasName()) {
            HumanName name = practitioner.getName().getFirst();
            entity.setFamily(name.getFamily());
            entity.setGiven(name.getGiven().stream().filter(StringType::hasValue).map(StringType::getValue).toList());
        }
        entity.setIdentifiers(practitioner.getIdentifier().stream()
                .filter(i -> i.hasSystem() || i.hasValue())
                .map(i -> new IdentifierValue(i.getSystem(), i.getValue()))
                .toList());
        return entity;
    }

    public Practitioner toResource(PractitionerEntity entity) {
        Practitioner practitioner = new Practitioner();
        FhirMapping.setIdAndMeta(practitioner, entity);
        if (entity.getActive() != null) {
            practitioner.setActive(entity.getActive());
        }
        if (entity.getFamily() != null || !entity.getGiven().isEmpty()) {
            HumanName name = practitioner.addName().setFamily(entity.getFamily());
            entity.getGiven().forEach(name::addGiven);
        }
        for (IdentifierValue identifier : entity.getIdentifiers()) {
            practitioner.addIdentifier(new Identifier().setSystem(identifier.getSystem()).setValue(identifier.getValue()));
        }
        return practitioner;
    }
}
