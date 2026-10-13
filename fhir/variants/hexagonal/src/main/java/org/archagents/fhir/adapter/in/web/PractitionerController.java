package org.archagents.fhir.adapter.in.web;

import java.util.List;
import org.archagents.fhir.application.port.in.FindPractitionersQuery;
import org.archagents.fhir.application.port.in.SavePractitionerUseCase;
import org.archagents.fhir.domain.PractitionerSearchCriteria;
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
class PractitionerController {

    private final SavePractitionerUseCase savePractitionerUseCase;
    private final FindPractitionersQuery findPractitionersQuery;
    private final PractitionerResourceMapper mapper;
    private final FhirJsonCodec codec;
    private final FhirResponses responses;

    PractitionerController(
            SavePractitionerUseCase savePractitionerUseCase,
            FindPractitionersQuery findPractitionersQuery,
            PractitionerResourceMapper mapper,
            FhirJsonCodec codec,
            FhirResponses responses) {
        this.savePractitionerUseCase = savePractitionerUseCase;
        this.findPractitionersQuery = findPractitionersQuery;
        this.mapper = mapper;
        this.codec = codec;
        this.responses = responses;
    }

    @PostMapping
    ResponseEntity<String> create(@RequestBody String body) {
        Practitioner resource = codec.parse(body, Practitioner.class);
        return responses.created(
                mapper.toResource(savePractitionerUseCase.createPractitioner(mapper.toDomain(resource))));
    }

    @GetMapping("/{id}")
    ResponseEntity<String> read(@PathVariable String id) {
        long practitionerId = FhirConversions.pathId("Practitioner", id);
        return responses.read(mapper.toResource(findPractitionersQuery.getPractitioner(practitionerId)));
    }

    @PutMapping("/{id}")
    ResponseEntity<String> update(@PathVariable String id, @RequestBody String body) {
        Practitioner resource = codec.parse(body, Practitioner.class);
        long practitionerId = FhirConversions.updateId(resource, id);
        return responses.read(mapper.toResource(
                savePractitionerUseCase.updatePractitioner(practitionerId, mapper.toDomain(resource))));
    }

    @GetMapping
    ResponseEntity<String> search(@RequestParam MultiValueMap<String, String> query) {
        SearchParameters params = SearchParameters.of(SupportedResource.PRACTITIONER, query);
        PractitionerSearchCriteria criteria = new PractitionerSearchCriteria(
                params.strings("family"),
                params.tokens("identifier"));
        List<Practitioner> matches =
                findPractitionersQuery.searchPractitioners(criteria).stream().map(mapper::toResource).toList();
        return responses.searchSet(matches);
    }
}
