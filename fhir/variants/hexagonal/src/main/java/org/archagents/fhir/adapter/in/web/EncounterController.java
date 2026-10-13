package org.archagents.fhir.adapter.in.web;

import java.util.List;
import org.archagents.fhir.application.port.in.FindEncountersQuery;
import org.archagents.fhir.application.port.in.SaveEncounterUseCase;
import org.archagents.fhir.domain.EncounterSearchCriteria;
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
class EncounterController {

    private final SaveEncounterUseCase saveEncounterUseCase;
    private final FindEncountersQuery findEncountersQuery;
    private final EncounterResourceMapper mapper;
    private final FhirJsonCodec codec;
    private final FhirResponses responses;

    EncounterController(
            SaveEncounterUseCase saveEncounterUseCase,
            FindEncountersQuery findEncountersQuery,
            EncounterResourceMapper mapper,
            FhirJsonCodec codec,
            FhirResponses responses) {
        this.saveEncounterUseCase = saveEncounterUseCase;
        this.findEncountersQuery = findEncountersQuery;
        this.mapper = mapper;
        this.codec = codec;
        this.responses = responses;
    }

    @PostMapping
    ResponseEntity<String> create(@RequestBody String body) {
        Encounter resource = codec.parse(body, Encounter.class);
        return responses.created(mapper.toResource(saveEncounterUseCase.createEncounter(mapper.toDomain(resource))));
    }

    @GetMapping("/{id}")
    ResponseEntity<String> read(@PathVariable String id) {
        long encounterId = FhirConversions.pathId("Encounter", id);
        return responses.read(mapper.toResource(findEncountersQuery.getEncounter(encounterId)));
    }

    @PutMapping("/{id}")
    ResponseEntity<String> update(@PathVariable String id, @RequestBody String body) {
        Encounter resource = codec.parse(body, Encounter.class);
        long encounterId = FhirConversions.updateId(resource, id);
        return responses.read(
                mapper.toResource(saveEncounterUseCase.updateEncounter(encounterId, mapper.toDomain(resource))));
    }

    @GetMapping
    ResponseEntity<String> search(@RequestParam MultiValueMap<String, String> query) {
        SearchParameters params = SearchParameters.of(SupportedResource.ENCOUNTER, query);
        EncounterSearchCriteria criteria = new EncounterSearchCriteria(
                params.references("patient", "Patient"),
                params.codes("status"));
        List<Encounter> matches =
                findEncountersQuery.searchEncounters(criteria).stream().map(mapper::toResource).toList();
        return responses.searchSet(matches);
    }
}
