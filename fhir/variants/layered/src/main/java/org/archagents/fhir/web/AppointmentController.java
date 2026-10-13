package org.archagents.fhir.web;

import org.archagents.fhir.entity.AppointmentEntity;
import org.archagents.fhir.service.AppointmentService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Appointment;
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
@RequestMapping("/fhir/Appointment")
public class AppointmentController {

    private static final String RESOURCE_TYPE = "Appointment";

    private final AppointmentService appointmentService;
    private final AppointmentMapper appointmentMapper;

    public AppointmentController(AppointmentService appointmentService, AppointmentMapper appointmentMapper) {
        this.appointmentService = appointmentService;
        this.appointmentMapper = appointmentMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> read(@PathVariable String id) {
        AppointmentEntity appointment = appointmentService.get(ResourceIds.parse(RESOURCE_TYPE, id));
        return FhirResponses.ok(appointmentMapper.toResource(appointment));
    }

    @PostMapping
    public ResponseEntity<Appointment> create(@RequestBody Appointment appointment) {
        AppointmentEntity created = appointmentService.create(appointmentMapper.toEntity(appointment));
        return FhirResponses.created(appointmentMapper.toResource(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Appointment> update(@PathVariable String id, @RequestBody Appointment appointment) {
        long appointmentId = ResourceIds.requireMatching(RESOURCE_TYPE, id, appointment);
        AppointmentEntity updated = appointmentService.update(appointmentId, appointmentMapper.toEntity(appointment));
        return FhirResponses.ok(appointmentMapper.toResource(updated));
    }

    @GetMapping
    public ResponseEntity<Bundle> search(@RequestParam MultiValueMap<String, String> parameters) {
        return FhirResponses.searchSet(appointmentService.search(parameters).stream().map(appointmentMapper::toResource).toList());
    }
}
