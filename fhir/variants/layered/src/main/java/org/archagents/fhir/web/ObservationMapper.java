package org.archagents.fhir.web;

import java.math.BigDecimal;
import org.archagents.fhir.entity.ObservationEntity;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Quantity;
import org.springframework.stereotype.Component;

@Component
public class ObservationMapper {

    public ObservationEntity toEntity(Observation observation) {
        ObservationEntity entity = new ObservationEntity();
        entity.setStatus(observation.hasStatus() ? observation.getStatus().toCode() : null);
        if (observation.hasCategory() && observation.getCategory().getFirst().hasCoding()) {
            Coding category = observation.getCategory().getFirst().getCoding().getFirst();
            entity.setCategorySystem(category.getSystem());
            entity.setCategoryCode(category.getCode());
        }
        if (observation.hasCode() && observation.getCode().hasCoding()) {
            Coding code = observation.getCode().getCoding().getFirst();
            entity.setCodeSystem(code.getSystem());
            entity.setCode(code.getCode());
            entity.setCodeDisplay(code.getDisplay());
        }
        entity.setSubjectReference(FhirMapping.referenceString(observation.getSubject()));
        entity.setEncounterReference(FhirMapping.referenceString(observation.getEncounter()));
        if (observation.hasEffectiveDateTimeType()) {
            entity.setEffective(FhirMapping.toInstant(observation.getEffectiveDateTimeType()));
        }
        if (observation.hasValueQuantity()) {
            Quantity quantity = observation.getValueQuantity();
            entity.setValueQuantity(quantity.getValue());
            entity.setValueUnit(quantity.getUnit());
            entity.setValueSystem(quantity.getSystem());
            entity.setValueCode(quantity.getCode());
        }
        return entity;
    }

    public Observation toResource(ObservationEntity entity) {
        Observation observation = new Observation();
        FhirMapping.setIdAndMeta(observation, entity);
        observation.setStatus(ObservationStatus.fromCode(entity.getStatus()));
        if (entity.getCategorySystem() != null || entity.getCategoryCode() != null) {
            observation.addCategory(new CodeableConcept(
                    new Coding().setSystem(entity.getCategorySystem()).setCode(entity.getCategoryCode())));
        }
        observation.setCode(new CodeableConcept(new Coding()
                .setSystem(entity.getCodeSystem())
                .setCode(entity.getCode())
                .setDisplay(entity.getCodeDisplay())));
        observation.setSubject(FhirMapping.reference("Patient", entity.getSubjectId()));
        observation.setEncounter(FhirMapping.reference("Encounter", entity.getEncounterId()));
        if (entity.getEffective() != null) {
            observation.setEffective(FhirMapping.dateTime(entity.getEffective()));
        }
        if (entity.hasValueQuantity()) {
            observation.setValue(new Quantity()
                    .setValue(normalize(entity.getValueQuantity()))
                    .setUnit(entity.getValueUnit())
                    .setSystem(entity.getValueSystem())
                    .setCode(entity.getValueCode()));
        }
        return observation;
    }

    private static BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
