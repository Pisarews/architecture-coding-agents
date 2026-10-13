package org.archagents.fhir.web;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Reads and writes HAPI FHIR resources as FHIR JSON.
 */
public class FhirJsonMessageConverter extends AbstractHttpMessageConverter<IBaseResource> {

    public static final MediaType FHIR_JSON = new MediaType("application", "fhir+json", StandardCharsets.UTF_8);

    private final FhirContext fhirContext;

    public FhirJsonMessageConverter(FhirContext fhirContext) {
        super(StandardCharsets.UTF_8, FHIR_JSON, MediaType.APPLICATION_JSON);
        this.fhirContext = fhirContext;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return IBaseResource.class.isAssignableFrom(clazz);
    }

    @Override
    protected IBaseResource readInternal(Class<? extends IBaseResource> clazz, HttpInputMessage inputMessage)
            throws IOException {
        IBaseResource resource;
        try (Reader reader = new InputStreamReader(inputMessage.getBody(), StandardCharsets.UTF_8)) {
            resource = fhirContext.newJsonParser().parseResource(reader);
        } catch (DataFormatException e) {
            throw new HttpMessageNotReadableException("Body is not a valid FHIR JSON resource: " + e.getMessage(), e, inputMessage);
        }
        if (!clazz.isInstance(resource)) {
            String expected = fhirContext.getResourceType(clazz);
            String actual = fhirContext.getResourceType(resource);
            throw new HttpMessageNotReadableException(
                    "Expected resource type " + expected + " but found " + actual, inputMessage);
        }
        Optional<String> unsupportedCode = SupportedCodes.findUnsupported(resource);
        if (unsupportedCode.isPresent()) {
            throw new HttpMessageNotReadableException(unsupportedCode.get(), inputMessage);
        }
        return clazz.cast(resource);
    }

    @Override
    protected void writeInternal(IBaseResource resource, HttpOutputMessage outputMessage) throws IOException {
        Writer writer = new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8);
        fhirContext.newJsonParser().encodeResourceToWriter(resource, writer);
        writer.flush();
    }
}
