package org.archagents.fhir.web;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import java.time.Instant;
import java.util.Date;
import org.archagents.fhir.entity.ResourceEntity;
import org.hl7.fhir.r4.model.BaseDateTimeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

/**
 * Conversions shared by the resource mappers.
 */
final class FhirMapping {

    private FhirMapping() {
    }

    static void setIdAndMeta(Resource resource, ResourceEntity entity) {
        resource.setId(String.valueOf(entity.getId()));
        resource.getMeta().setVersionId(String.valueOf(entity.getVersion()));
        resource.getMeta().setLastUpdatedElement(instant(entity.getLastUpdated()));
    }

    static String referenceString(Reference reference) {
        return reference == null || !reference.hasReference() ? null : reference.getReference();
    }

    static Reference reference(String resourceType, Long id) {
        return id == null ? null : new Reference(resourceType + "/" + id);
    }

    static Instant toInstant(BaseDateTimeType value) {
        return value == null || value.getValue() == null ? null : value.getValue().toInstant();
    }

    static DateTimeType dateTime(Instant value) {
        if (value == null) {
            return null;
        }
        DateTimeType dateTime = new DateTimeType(Date.from(value), precision(value));
        dateTime.setTimeZoneZulu(true);
        return dateTime;
    }

    static InstantType instant(Instant value) {
        if (value == null) {
            return null;
        }
        InstantType instant = new InstantType(Date.from(value), precision(value));
        instant.setTimeZoneZulu(true);
        return instant;
    }

    private static TemporalPrecisionEnum precision(Instant value) {
        return value.getNano() == 0 ? TemporalPrecisionEnum.SECOND : TemporalPrecisionEnum.MILLI;
    }
}
