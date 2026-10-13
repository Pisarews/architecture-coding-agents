package org.archagents.fhir.adapter.in.web;

import java.util.List;
import org.archagents.fhir.application.port.in.FindPatientsQuery;
import org.archagents.fhir.application.port.in.SavePatientUseCase;
import org.archagents.fhir.domain.PatientSearchCriteria;
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
class PatientController {

    private final SavePatientUseCase savePatientUseCase;
    private final FindPatientsQuery findPatientsQuery;
    private final PatientResourceMapper mapper;
    private final FhirJsonCodec codec;
    private final FhirResponses responses;

    PatientController(
            SavePatientUseCase savePatientUseCase,
            FindPatientsQuery findPatientsQuery,
            PatientResourceMapper mapper,
            FhirJsonCodec codec,
            FhirResponses responses) {
        this.savePatientUseCase = savePatientUseCase;
        this.findPatientsQuery = findPatientsQuery;
        this.mapper = mapper;
        this.codec = codec;
        this.responses = responses;
    }

    @PostMapping
    ResponseEntity<String> create(@RequestBody String body) {
        Patient resource = codec.parse(body, Patient.class);
        return responses.created(mapper.toResource(savePatientUseCase.createPatient(mapper.toDomain(resource))));
    }

    @GetMapping("/{id}")
    ResponseEntity<String> read(@PathVariable String id) {
        long patientId = FhirConversions.pathId("Patient", id);
        return responses.read(mapper.toResource(findPatientsQuery.getPatient(patientId)));
    }

    @PutMapping("/{id}")
    ResponseEntity<String> update(@PathVariable String id, @RequestBody String body) {
        Patient resource = codec.parse(body, Patient.class);
        long patientId = FhirConversions.updateId(resource, id);
        return responses.read(mapper.toResource(savePatientUseCase.updatePatient(patientId, mapper.toDomain(resource))));
    }

    @GetMapping
    ResponseEntity<String> search(@RequestParam MultiValueMap<String, String> query) {
        SearchParameters params = SearchParameters.of(SupportedResource.PATIENT, query);
        PatientSearchCriteria criteria = new PatientSearchCriteria(
                params.strings("family"),
                params.strings("given"),
                params.tokens("identifier"),
                params.codes("gender"),
                params.dates("birthdate"));
        List<Patient> matches = findPatientsQuery.searchPatients(criteria).stream().map(mapper::toResource).toList();
        return responses.searchSet(matches);
    }
}
