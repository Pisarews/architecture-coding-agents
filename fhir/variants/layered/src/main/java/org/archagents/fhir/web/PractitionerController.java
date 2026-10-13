package org.archagents.fhir.web;

import org.archagents.fhir.entity.PractitionerEntity;
import org.archagents.fhir.service.PractitionerService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir/Practitioner")
public class PractitionerController {

    private static final String RESOURCE_TYPE = "Practitioner";

    private final PractitionerService practitionerService;
    private final PractitionerMapper practitionerMapper;

    public PractitionerController(PractitionerService practitionerService, PractitionerMapper practitionerMapper) {
        this.practitionerService = practitionerService;
        this.practitionerMapper = practitionerMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Practitioner> read(@PathVariable String id) {
        PractitionerEntity practitioner = practitionerService.get(ResourceIds.parse(RESOURCE_TYPE, id));
        return FhirResponses.ok(practitionerMapper.toResource(practitioner));
    }

    @PostMapping
    public ResponseEntity<Practitioner> create(@RequestBody Practitioner practitioner) {
        PractitionerEntity created = practitionerService.create(practitionerMapper.toEntity(practitioner));
        return FhirResponses.created(practitionerMapper.toResource(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Practitioner> update(@PathVariable String id, @RequestBody Practitioner practitioner) {
        long practitionerId = ResourceIds.requireMatching(RESOURCE_TYPE, id, practitioner);
        PractitionerEntity updated = practitionerService.update(practitionerId, practitionerMapper.toEntity(practitioner));
        return FhirResponses.ok(practitionerMapper.toResource(updated));
    }

    @GetMapping
    public ResponseEntity<Bundle> search(@RequestParam MultiValueMap<String, String> parameters) {
        return FhirResponses.searchSet(practitionerService.search(parameters).stream().map(practitionerMapper::toResource).toList());
    }
}
