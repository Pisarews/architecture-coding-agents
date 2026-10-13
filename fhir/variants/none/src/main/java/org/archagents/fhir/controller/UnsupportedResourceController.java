package org.archagents.fhir.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.archagents.fhir.util.FhirException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UnsupportedResourceController {

    private static final Set<String> KNOWN_TYPES = Set.of("Patient", "Practitioner", "Encounter", "Observation", "Appointment", "metadata");

    @RequestMapping({"/fhir/{type}", "/fhir/{type}/{id}"})
    public void unsupported(@PathVariable String type, HttpServletRequest request) {
        String interaction = request.getMethod() + " " + request.getRequestURI();
        if (KNOWN_TYPES.contains(type)) {
            throw FhirException.methodNotAllowed("Unsupported interaction: " + interaction);
        }
        throw FhirException.unsupportedResource("Unsupported resource type: " + interaction);
    }
}
