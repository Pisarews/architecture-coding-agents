package org.archagents.fhir.adapter.in.web;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.LenientErrorHandler;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

@Component
class FhirJsonCodec {

    private final FhirContext fhirContext = FhirContext.forR4();

    <T extends IBaseResource> T parse(String body, Class<T> expectedType) {
        IBaseResource resource;
        try {
            resource = newParser().parseResource(body);
        } catch (DataFormatException | IllegalArgumentException e) {
            throw FhirRequestException.structure("Body is not a valid FHIR JSON resource: " + e.getMessage());
        }
        String expectedName = fhirContext.getResourceType(expectedType);
        if (!expectedType.isInstance(resource)) {
            throw FhirRequestException.structure(
                    "Expected resourceType " + expectedName + " but got " + fhirContext.getResourceType(resource));
        }
        return expectedType.cast(resource);
    }

    String encode(IBaseResource resource) {
        return newParser().encodeResourceToString(resource);
    }

    private IParser newParser() {
        return fhirContext.newJsonParser().setParserErrorHandler(new RejectInvalidValues());
    }

    /** Unknown elements are ignored, but values that do not fit their datatype (e.g. unknown codes) are not. */
    private static final class RejectInvalidValues extends LenientErrorHandler {

        RejectInvalidValues() {
            super(false);
        }

        @Override
        public void invalidValue(IParseLocation location, String value, String error) {
            throw new DataFormatException("Invalid value '" + value + "': " + error);
        }
    }
}
