package org.archagents.fhir.adapter.in.web;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Function;
import org.archagents.fhir.domain.HumanName;
import org.archagents.fhir.domain.Identifier;
import org.archagents.fhir.domain.Reference;
import org.archagents.fhir.domain.StoredResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.BaseDateTimeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;

/** Conversions between FHIR datatypes and the domain's value types, shared by the resource mappers. */
final class FhirConversions {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private FhirConversions() {
    }

    static void applyMeta(Resource resource, StoredResource<?> stored) {
        resource.setId(Long.toString(stored.id()));
        resource.getMeta()
                .setVersionId(Integer.toString(stored.meta().versionId()))
                .setLastUpdatedElement(toInstantType(stored.meta().lastUpdated()));
    }

    /** Checks the id of an update body against the URL and returns it as a numeric id. */
    static long updateId(Resource resource, String pathId) {
        String bodyId = resource.getIdElement().getIdPart();
        if (bodyId == null) {
            throw FhirRequestException.invalid("Resource id is required for update");
        }
        if (!bodyId.equals(pathId)) {
            throw FhirRequestException.invalid("Resource id '" + bodyId + "' does not match URL id '" + pathId + "'");
        }
        return pathId(resource.fhirType(), pathId);
    }

    static long pathId(String type, String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw FhirRequestException.notFound(type + "/" + id + " does not exist");
        }
    }

    static <E> E code(String code, Function<String, E> fromCode) {
        try {
            return fromCode.apply(code);
        } catch (IllegalArgumentException e) {
            throw FhirRequestException.structure(e.getMessage());
        }
    }

    static Reference toReference(org.hl7.fhir.r4.model.Reference reference) {
        if (reference == null || !reference.hasReference()) {
            return null;
        }
        IIdType id = reference.getReferenceElement();
        return new Reference(id.getResourceType(), id.getIdPart());
    }

    static org.hl7.fhir.r4.model.Reference toFhirReference(Reference reference) {
        return new org.hl7.fhir.r4.model.Reference(reference.type() + "/" + reference.id());
    }

    static HumanName toHumanName(List<org.hl7.fhir.r4.model.HumanName> names) {
        if (names.isEmpty()) {
            return null;
        }
        org.hl7.fhir.r4.model.HumanName first = names.get(0);
        if (!first.hasFamily() && !first.hasGiven()) {
            return null;
        }
        List<String> given = first.getGiven().stream().map(StringType::getValue).filter(Objects::nonNull).toList();
        return new HumanName(first.getFamily(), given);
    }

    static org.hl7.fhir.r4.model.HumanName toFhirHumanName(HumanName name) {
        org.hl7.fhir.r4.model.HumanName fhirName = new org.hl7.fhir.r4.model.HumanName().setFamily(name.family());
        name.given().forEach(fhirName::addGiven);
        return fhirName;
    }

    static List<Identifier> toIdentifiers(List<org.hl7.fhir.r4.model.Identifier> identifiers) {
        return identifiers.stream().map(i -> new Identifier(i.getSystem(), i.getValue())).toList();
    }

    static org.hl7.fhir.r4.model.Identifier toFhirIdentifier(Identifier identifier) {
        return new org.hl7.fhir.r4.model.Identifier().setSystem(identifier.system()).setValue(identifier.value());
    }

    static Instant toInstant(BaseDateTimeType value) {
        return value == null || value.getValue() == null ? null : value.getValue().toInstant();
    }

    static DateTimeType toDateTime(Instant instant) {
        return instant == null ? null : new DateTimeType(Date.from(instant), precisionOf(instant), UTC);
    }

    static InstantType toInstantType(Instant instant) {
        return instant == null ? null : new InstantType(Date.from(instant), TemporalPrecisionEnum.MILLI, UTC);
    }

    static LocalDate toLocalDate(DateType date) {
        if (date == null || date.getValue() == null) {
            return null;
        }
        return LocalDate.of(date.getYear(), date.getMonth() + 1, date.getDay());
    }

    static DateType toDateType(LocalDate date) {
        return date == null ? null : new DateType(date.toString());
    }

    private static TemporalPrecisionEnum precisionOf(Instant instant) {
        return instant.getNano() == 0 ? TemporalPrecisionEnum.SECOND : TemporalPrecisionEnum.MILLI;
    }
}
