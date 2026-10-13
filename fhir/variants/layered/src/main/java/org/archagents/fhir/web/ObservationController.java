package org.archagents.fhir.web;

import org.archagents.fhir.entity.ObservationEntity;
import org.archagents.fhir.service.ObservationService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
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
@RequestMapping("/fhir/Observation")
public class ObservationController {

    private static final String RESOURCE_TYPE = "Observation";

    private final ObservationService observationService;
    private final ObservationMapper observationMapper;

    public ObservationController(ObservationService observationService, ObservationMapper observationMapper) {
        this.observationService = observationService;
        this.observationMapper = observationMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Observation> read(@PathVariable String id) {
        ObservationEntity observation = observationService.get(ResourceIds.parse(RESOURCE_TYPE, id));
        return FhirResponses.ok(observationMapper.toResource(observation));
    }

    @PostMapping
    public ResponseEntity<Observation> create(@RequestBody Observation observation) {
        ObservationEntity created = observationService.create(observationMapper.toEntity(observation));
        return FhirResponses.created(observationMapper.toResource(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Observation> update(@PathVariable String id, @RequestBody Observation observation) {
        long observationId = ResourceIds.requireMatching(RESOURCE_TYPE, id, observation);
        ObservationEntity updated = observationService.update(observationId, observationMapper.toEntity(observation));
        return FhirResponses.ok(observationMapper.toResource(updated));
    }

    @GetMapping
    public ResponseEntity<Bundle> search(@RequestParam MultiValueMap<String, String> parameters) {
        return FhirResponses.searchSet(observationService.search(parameters).stream().map(observationMapper::toResource).toList());
    }
}
