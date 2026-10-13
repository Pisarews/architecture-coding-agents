# FHIR server

FHIR R4 REST API (JSON only) under `/fhir` for Patient, Practitioner, Encounter, Observation
and Appointment. Spring Boot 3.5, Java 21, Spring Data JPA, in-memory H2, Flyway. HAPI FHIR
(`hapi-fhir-structures-r4`) provides the FHIR model classes and the JSON parser only.

## Build, test, run

- Build the executable jar (runs all tests): `mvn -q package`
- Run tests only: `mvn test`
- Run the server: `java -jar target/*.jar --server.port=8080`, then `curl localhost:8080/fhir/metadata`

## Package layout (`org.archagents.fhir`)

- `Application` — Spring Boot entry point.
- `web` — `@RestController`s per resource type, metadata and fallback controllers, mappers between
  HAPI FHIR resources and entities, the FHIR JSON message converter and `FhirContext` bean,
  and the exception handler that produces `OperationOutcome` responses.
- `service` — `@Service` classes per resource type: validation and business rules, transactions,
  search-parameter parsing; plus the exceptions they throw.
- `repository` — Spring Data JPA repositories and JPA `Specification` factories used for search.
- `entity` — JPA entities and embeddables.
- `config` — general configuration beans (`Clock`).

Schema: `src/main/resources/db/migration/` (Flyway; Hibernate runs with `ddl-auto=validate`).
Tests: `src/test/java` holds unit tests and `ArchitectureTest` (ArchUnit rules).
