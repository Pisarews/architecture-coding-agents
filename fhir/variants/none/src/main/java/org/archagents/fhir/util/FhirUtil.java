package org.archagents.fhir.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.parser.DataFormatException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BaseDateTimeType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class FhirUtil {

    public static final FhirContext CONTEXT = FhirContext.forR4();
    public static final MediaType FHIR_JSON = MediaType.parseMediaType("application/fhir+json;charset=UTF-8");

    private FhirUtil() {
    }

    public static <T extends Resource> T parse(String body, Class<T> type) {
        IBaseResource parsed;
        try {
            parsed = CONTEXT.newJsonParser().parseResource(body);
        } catch (DataFormatException e) {
            throw FhirException.structure("Unable to parse request body: " + e.getMessage());
        }
        String expected = CONTEXT.getResourceType(type);
        if (!type.isInstance(parsed)) {
            throw FhirException.structure("Expected resource of type " + expected + " but got " + CONTEXT.getResourceType(parsed));
        }
        return type.cast(parsed);
    }

    /** Rejects codes that are valid in FHIR R4 but outside the subset this server supports. */
    public static void requireSupportedCode(String element, Enumeration<?> value, Collection<String> supported) {
        if (value != null && value.hasValue() && !supported.contains(value.getValueAsString())) {
            throw FhirException.structure("Unsupported code '" + value.getValueAsString() + "' for " + element
                    + " (supported: " + String.join(", ", supported) + ")");
        }
    }

    public static String encode(IBaseResource resource) {
        return CONTEXT.newJsonParser().encodeResourceToString(resource);
    }

    public static long parseId(String type, String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw FhirException.notFound(type, id);
        }
    }

    public static void requireMatchingId(Resource resource, String urlId) {
        if (!resource.hasIdElement() || !urlId.equals(resource.getIdElement().getIdPart())) {
            throw FhirException.invalid("Resource id must be present and match the id in the URL (" + urlId + ")");
        }
    }

    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public static Meta meta(int version, Instant lastUpdated) {
        return new Meta().setVersionId(String.valueOf(version)).setLastUpdatedElement(instant(lastUpdated));
    }

    public static String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/fhir").toUriString();
    }

    public static ResponseEntity<String> response(HttpStatus status, IBaseResource resource) {
        return ResponseEntity.status(status).contentType(FHIR_JSON).body(encode(resource));
    }

    public static ResponseEntity<String> ok(Resource resource) {
        return ResponseEntity.ok()
                .contentType(FHIR_JSON)
                .eTag(etag(resource))
                .body(encode(resource));
    }

    public static ResponseEntity<String> created(Resource resource) {
        String location = baseUrl() + "/" + resource.fhirType() + "/" + resource.getIdElement().getIdPart()
                + "/_history/" + resource.getMeta().getVersionId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(FHIR_JSON)
                .eTag(etag(resource))
                .header("Location", location)
                .body(encode(resource));
    }

    public static ResponseEntity<String> searchset(List<? extends Resource> resources) {
        String base = baseUrl();
        Bundle bundle = new Bundle().setType(Bundle.BundleType.SEARCHSET).setTotal(resources.size());
        for (Resource resource : resources) {
            bundle.addEntry()
                    .setFullUrl(base + "/" + resource.fhirType() + "/" + resource.getIdElement().getIdPart())
                    .setResource(resource)
                    .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
        }
        return response(HttpStatus.OK, bundle);
    }

    private static String etag(Resource resource) {
        return "W/\"" + resource.getMeta().getVersionId() + "\"";
    }

    public static Instant toInstant(BaseDateTimeType value) {
        return value != null && value.getValue() != null ? value.getValue().toInstant() : null;
    }

    public static LocalDate toLocalDate(DateType value) {
        return value != null && value.getValue() != null
                ? LocalDate.of(value.getYear(), value.getMonth() + 1, value.getDay())
                : null;
    }

    public static InstantType instant(Instant value) {
        if (value == null) {
            return null;
        }
        InstantType instant = new InstantType(Date.from(value), precision(value));
        instant.setTimeZoneZulu(true);
        return instant;
    }

    public static DateTimeType dateTime(Instant value) {
        if (value == null) {
            return null;
        }
        DateTimeType dateTime = new DateTimeType(Date.from(value), precision(value));
        dateTime.setTimeZoneZulu(true);
        return dateTime;
    }

    public static DateType date(LocalDate value) {
        return value == null ? null : new DateType(value.toString());
    }

    public static BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    private static TemporalPrecisionEnum precision(Instant value) {
        return value.getNano() == 0 ? TemporalPrecisionEnum.SECOND : TemporalPrecisionEnum.MILLI;
    }
}
