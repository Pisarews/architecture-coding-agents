package org.archagents.fhir.web;

import org.archagents.fhir.entity.EncounterEntity;
import org.archagents.fhir.service.EncounterService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
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
@RequestMapping("/fhir/Encounter")
public class EncounterController {

    private static final String RESOURCE_TYPE = "Encounter";

    private final EncounterService encounterService;
    private final EncounterMapper encounterMapper;

    public EncounterController(EncounterService encounterService, EncounterMapper encounterMapper) {
        this.encounterService = encounterService;
        this.encounterMapper = encounterMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Encounter> read(@PathVariable String id) {
        EncounterEntity encounter = encounterService.get(ResourceIds.parse(RESOURCE_TYPE, id));
        return FhirResponses.ok(encounterMapper.toResource(encounter));
    }

    @PostMapping
    public ResponseEntity<Encounter> create(@RequestBody Encounter encounter) {
        EncounterEntity created = encounterService.create(encounterMapper.toEntity(encounter));
        return FhirResponses.created(encounterMapper.toResource(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Encounter> update(@PathVariable String id, @RequestBody Encounter encounter) {
        long encounterId = ResourceIds.requireMatching(RESOURCE_TYPE, id, encounter);
        EncounterEntity updated = encounterService.update(encounterId, encounterMapper.toEntity(encounter));
        return FhirResponses.ok(encounterMapper.toResource(updated));
    }

    @GetMapping
    public ResponseEntity<Bundle> search(@RequestParam MultiValueMap<String, String> parameters) {
        return FhirResponses.searchSet(encounterService.search(parameters).stream().map(encounterMapper::toResource).toList());
    }
}
