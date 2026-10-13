# The three architecture variants

This document is binding for both domains. Each variant implements the domain's `SPEC.md`
exactly. The variants differ **only** in how the code is organized. Root package:
`org.archagents.<domain>` (e.g. `org.archagents.fhir`).

Shared ground rules (all variants):
- Same `pom.xml` dependency set, same Flyway schema (byte-identical), same `application.properties` keys.
- Idiomatic, competent code of the given style. Neither a strawman nor gold-plated.
  Write what an experienced Spring developer following that style would write for production.
- No comments that explain the experiment, and no comments that point to other variants.
- Standard Spring wiring only (component scan, constructor injection, `@Bean` where the style calls for it).
- JUnit tests in `src/test/java` are allowed: a small set of meaningful unit tests, about the
  same number in each variant. ArchUnit rules sit in `src/test/java/.../ArchitectureTest.java`.
- Main class: `org.archagents.<domain>.Application`.

---

## NONE: no explicit architecture

How a typical Spring Boot tutorial or startup codebase looks after a few sprints. There is
no deliberate layering.

- Package by technical convenience: `controller`, `entity` (JPA entities), `repository`
  (Spring Data interfaces), `util` (shared helpers, e.g. error handling and search-parameter parsing).
- **Controllers hold the logic.** One `@RestController` per resource does parsing, validation,
  business rules, persistence calls and FHIR mapping (or JSON mapping for ORDERS) itself.
- Controllers call repositories directly. There is no service layer and there are no
  interfaces for own components.
- Mapping between external representation and entities is inline in the controller, or in
  static helpers on the entity or in `util`.
- Cross-cutting concerns may live in `util` (an exception handler, shared parsing helpers).
- No ArchUnit conformance rules: there is no target architecture. `ArchitectureTest` holds
  only the architecture-agnostic checks used by every variant (see below).

## LAYERED: controller → service → repository

Classic package-by-layer, strict top-down dependencies.

| Package | Contents |
|---|---|
| `web` | `@RestController`s, request/response mapping between the external representation (FHIR resources / JSON DTOs) and entities, exception → HTTP translation |
| `service` | `@Service` classes: validation, business rules, transactions, search logic; operate on entities |
| `repository` | Spring Data repositories, query methods/specifications |
| `entity` | JPA entities |
| `config` | configuration beans, if needed |

ArchUnit rules (`ArchitectureTest`):
- `layeredArchitecture()`: `web` may access `service` and `entity`. `service` may access
  `repository` and `entity`. `repository` may access `entity`. Nothing accesses `web`.
  `service` is accessed only by `web`. `repository` is accessed only by `service`.
- External-model types (`org.hl7.fhir..`, `ca.uhn.fhir..` for FHIR; the API DTO package for
  ORDERS) are used only in `web`.
- No package cycles.

## HEXAGONAL: ports and adapters

Following Hombergs, *Get Your Hands Dirty on Clean Architecture* (reference implementation
`thombergs/buckpal`).

| Package | Contents |
|---|---|
| `domain` | Domain model (plain Java classes/records, own value types, enums) and domain rules. **No Spring, JPA, Jakarta or HAPI imports.** |
| `application.port.in` | Use-case interfaces (inbound ports) + command/query objects |
| `application.port.out` | Persistence/integration interfaces (outbound ports) |
| `application.service` | Use-case implementations. They depend only on `domain` and ports. No Spring annotations (wired in `config`), except `@Transactional`, which is allowed |
| `adapter.in.web` | `@RestController`s, mapping of the external representation (FHIR resources / JSON DTOs) ↔ domain/commands, exception → HTTP translation |
| `adapter.out.persistence` | JPA entities, Spring Data repositories, entity ↔ domain mapping, implementations of outbound ports |
| `config` | `@Configuration` wiring use cases to ports |

ArchUnit rules (`ArchitectureTest`):
- `domain` depends only on `java..` and `domain`.
- `application` depends only on `java..`, `domain`, `application` (plus `org.springframework.transaction.annotation`).
- `adapter.in.web` must not access `adapter.out..`, and `adapter.out..` must not access `adapter.in..`.
- `adapter.in.web` accesses the application only through `application.port.in` (never `application.service`).
- HAPI/FHIR types only in `adapter.in.web`. `jakarta.persistence` only in `adapter.out.persistence`.
- No package cycles.

---

## Architecture-agnostic checks (in every variant's `ArchitectureTest`)
- `org.archagents.<domain>` must not depend on `ca.uhn.fhir.rest.server..` or `ca.uhn.fhir.jpa..` (FHIR).
- No field injection (`@Autowired` on fields).

These are baseline invariants, not erosion measures. Erosion is measured outside the
agent's workspace (see `PREREGISTRATION.md` §4.3).
