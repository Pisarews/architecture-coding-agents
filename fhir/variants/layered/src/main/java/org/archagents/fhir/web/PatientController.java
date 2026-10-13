package org.archagents.fhir.web;

import org.archagents.fhir.entity.PatientEntity;
import org.archagents.fhir.service.PatientService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
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
@RequestMapping("/fhir/Patient")
public class PatientController {

    private static final String RESOURCE_TYPE = "Patient";

    private final PatientService patientService;
    private final PatientMapper patientMapper;

    public PatientController(PatientService patientService, PatientMapper patientMapper) {
        this.patientService = patientService;
        this.patientMapper = patientMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> read(@PathVariable String id) {
        PatientEntity patient = patientService.get(ResourceIds.parse(RESOURCE_TYPE, id));
        return FhirResponses.ok(patientMapper.toResource(patient));
    }

    @PostMapping
    public ResponseEntity<Patient> create(@RequestBody Patient patient) {
        PatientEntity created = patientService.create(patientMapper.toEntity(patient));
        return FhirResponses.created(patientMapper.toResource(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> update(@PathVariable String id, @RequestBody Patient patient) {
        long patientId = ResourceIds.requireMatching(RESOURCE_TYPE, id, patient);
        PatientEntity updated = patientService.update(patientId, patientMapper.toEntity(patient));
        return FhirResponses.ok(patientMapper.toResource(updated));
    }

    @GetMapping
    public ResponseEntity<Bundle> search(@RequestParam MultiValueMap<String, String> parameters) {
        return FhirResponses.searchSet(patientService.search(parameters).stream().map(patientMapper::toResource).toList());
    }
}
