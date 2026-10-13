# FHIR server

FHIR R4 REST API (JSON only) under `/fhir` for Patient, Practitioner, Encounter, Observation
and Appointment. Spring Boot 3.5, Java 21, Spring Data JPA, in-memory H2, Flyway. HAPI FHIR
(`hapi-fhir-structures-r4`) provides the FHIR model classes and the JSON parser only.

## Build, test, run

- Build the executable jar (runs all tests): `mvn -q package`
- Run tests only: `mvn test`
- Run the server: `java -jar target/*.jar --server.port=8080`, then `curl localhost:8080/fhir/metadata`

## Package layout (`org.archagents.fhir`)

- `domain` — clinical model as plain Java records and enums and value types, validation rules, search criteria.
- `application.port.in` — use-case interfaces and their exceptions.
- `application.port.out` — persistence interfaces.
- `application.service` — use-case implementations; transactional; no other framework dependencies.
- `adapter.in.web` — REST controllers under `/fhir`, FHIR JSON parsing/encoding, FHIR <-> domain
  mapping, search-parameter parsing, error responses as OperationOutcome.
- `adapter.out.persistence` — JPA entities, Spring Data repositories, entity <-> domain mapping,
  implementations of the outbound ports.
- `config` — Spring `@Configuration` that wires the application services to the ports.

Schema: `src/main/resources/db/migration/` (Flyway; Hibernate runs with `ddl-auto=validate`).
Tests: `src/test/java` holds unit tests and `ArchitectureTest` (ArchUnit rules).
