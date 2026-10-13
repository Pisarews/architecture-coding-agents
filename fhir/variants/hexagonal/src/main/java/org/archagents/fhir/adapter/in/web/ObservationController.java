package org.archagents.fhir.adapter.in.web;

import java.util.List;
import org.archagents.fhir.application.port.in.FindObservationsQuery;
import org.archagents.fhir.application.port.in.SaveObservationUseCase;
import org.archagents.fhir.domain.ObservationSearchCriteria;
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
class ObservationController {

    private final SaveObservationUseCase saveObservationUseCase;
    private final FindObservationsQuery findObservationsQuery;
    private final ObservationResourceMapper mapper;
    private final FhirJsonCodec codec;
    private final FhirResponses responses;

    ObservationController(
            SaveObservationUseCase saveObservationUseCase,
            FindObservationsQuery findObservationsQuery,
            ObservationResourceMapper mapper,
            FhirJsonCodec codec,
            FhirResponses responses) {
        this.saveObservationUseCase = saveObservationUseCase;
        this.findObservationsQuery = findObservationsQuery;
        this.mapper = mapper;
        this.codec = codec;
        this.responses = responses;
    }

    @PostMapping
    ResponseEntity<String> create(@RequestBody String body) {
        Observation resource = codec.parse(body, Observation.class);
        return responses.created(
                mapper.toResource(saveObservationUseCase.createObservation(mapper.toDomain(resource))));
    }

    @GetMapping("/{id}")
    ResponseEntity<String> read(@PathVariable String id) {
        long observationId = FhirConversions.pathId("Observation", id);
        return responses.read(mapper.toResource(findObservationsQuery.getObservation(observationId)));
    }

    @PutMapping("/{id}")
    ResponseEntity<String> update(@PathVariable String id, @RequestBody String body) {
        Observation resource = codec.parse(body, Observation.class);
        long observationId = FhirConversions.updateId(resource, id);
        return responses.read(mapper.toResource(
                saveObservationUseCase.updateObservation(observationId, mapper.toDomain(resource))));
    }

    @GetMapping
    ResponseEntity<String> search(@RequestParam MultiValueMap<String, String> query) {
        SearchParameters params = SearchParameters.of(SupportedResource.OBSERVATION, query);
        ObservationSearchCriteria criteria = new ObservationSearchCriteria(
                params.references("patient", "Patient"),
                params.tokens("code"),
                params.codes("category"));
        List<Observation> matches =
                findObservationsQuery.searchObservations(criteria).stream().map(mapper::toResource).toList();
        return responses.searchSet(matches);
    }
}
