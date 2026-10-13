package org.archagents.fhir.adapter.in.web;

import java.util.List;
import org.archagents.fhir.application.port.in.FindAppointmentsQuery;
import org.archagents.fhir.application.port.in.SaveAppointmentUseCase;
import org.archagents.fhir.domain.AppointmentSearchCriteria;
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
class AppointmentController {

    private final SaveAppointmentUseCase saveAppointmentUseCase;
    private final FindAppointmentsQuery findAppointmentsQuery;
    private final AppointmentResourceMapper mapper;
    private final FhirJsonCodec codec;
    private final FhirResponses responses;

    AppointmentController(
            SaveAppointmentUseCase saveAppointmentUseCase,
            FindAppointmentsQuery findAppointmentsQuery,
            AppointmentResourceMapper mapper,
            FhirJsonCodec codec,
            FhirResponses responses) {
        this.saveAppointmentUseCase = saveAppointmentUseCase;
        this.findAppointmentsQuery = findAppointmentsQuery;
        this.mapper = mapper;
        this.codec = codec;
        this.responses = responses;
    }

    @PostMapping
    ResponseEntity<String> create(@RequestBody String body) {
        Appointment resource = codec.parse(body, Appointment.class);
        return responses.created(
                mapper.toResource(saveAppointmentUseCase.createAppointment(mapper.toDomain(resource))));
    }

    @GetMapping("/{id}")
    ResponseEntity<String> read(@PathVariable String id) {
        long appointmentId = FhirConversions.pathId("Appointment", id);
        return responses.read(mapper.toResource(findAppointmentsQuery.getAppointment(appointmentId)));
    }

    @PutMapping("/{id}")
    ResponseEntity<String> update(@PathVariable String id, @RequestBody String body) {
        Appointment resource = codec.parse(body, Appointment.class);
        long appointmentId = FhirConversions.updateId(resource, id);
        return responses.read(mapper.toResource(
                saveAppointmentUseCase.updateAppointment(appointmentId, mapper.toDomain(resource))));
    }

    @GetMapping
    ResponseEntity<String> search(@RequestParam MultiValueMap<String, String> query) {
        SearchParameters params = SearchParameters.of(SupportedResource.APPOINTMENT, query);
        AppointmentSearchCriteria criteria = new AppointmentSearchCriteria(
                params.references("patient", "Patient"),
                params.references("practitioner", "Practitioner"),
                params.codes("status"),
                params.dates("date"));
        List<Appointment> matches =
                findAppointmentsQuery.searchAppointments(criteria).stream().map(mapper::toResource).toList();
        return responses.searchSet(matches);
    }
}
