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
- `controller` — `@RestController`s per resource type, plus metadata and fallback controllers.
  Each parses the request, validates it, applies business rules, maps between HAPI FHIR
  resources and entities, calls the repositories and builds the response.
- `entity` — JPA entities and embeddables.
- `repository` — Spring Data JPA repositories; search uses JPA `Specification`s.
- `util` — shared helpers: FHIR JSON parsing/encoding, the exception handler that produces
  `OperationOutcome` responses, search-parameter and reference parsing.

Schema: `src/main/resources/db/migration/` (Flyway; Hibernate runs with `ddl-auto=validate`).
Tests: `src/test/java` holds unit tests and `ArchitectureTest` (ArchUnit rules).
