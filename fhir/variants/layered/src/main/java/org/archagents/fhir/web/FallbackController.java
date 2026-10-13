package org.archagents.fhir.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles {@code /fhir/{type}} requests that no resource controller mapping matches:
 * unknown resource types and unsupported interactions on known ones.
 */
@RestController
public class FallbackController {

    @RequestMapping({"/fhir/{type}", "/fhir/{type}/{id}"})
    public void unmatched(@PathVariable String type, HttpServletRequest request)
            throws HttpRequestMethodNotSupportedException {
        if (MetadataController.isSupportedResourceType(type)) {
            throw new HttpRequestMethodNotSupportedException(request.getMethod(), List.of());
        }
        throw new UnsupportedResourceTypeException(type);
    }
}
