# FHIR domain — baseline behavioral specification

This file is the **single source of truth** for the baseline behavior that all three variants
(NONE, LAYERED, HEXAGONAL) implement identically. The black-box suite in `acceptance/`
enforces it. Architecture is deliberately **not** specified here: see `ARCHITECTURES.md`.

## 1. Platform (identical in every variant)

| Item | Value |
|---|---|
| Java | 21 (`maven.compiler.release=21`) |
| Spring Boot | 3.5.16 (parent POM) |
| Persistence | Spring Data JPA (Hibernate), H2 in-memory, Flyway, `spring.jpa.hibernate.ddl-auto=validate` |
| Schema | `schema/V1__baseline.sql`, copied byte-identical to `src/main/resources/db/migration/` |
| FHIR library | `ca.uhn.hapi.fhir:hapi-fhir-structures-r4:8.12.1` — model classes + `FhirContext.forR4()` JSON parser **only**. No HAPI server modules (`hapi-fhir-server`, JPA server, `RestfulServer`). |
| Web | Spring MVC (`spring-boot-starter-web`) |
| Build | Maven wrapper not required; `mvn package` produces an executable jar |
| Base path | `/fhir` |
| Port | `server.port` (the gate passes it on the command line) |

Allowed dependency set (exactly; the gate compares `pom.xml` dependency lists):
`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`,
`flyway-core`, `h2`, `hapi-fhir-structures-r4`, test scope: `spring-boot-starter-test`,
`archunit-junit5`.

## 2. General HTTP behavior

- Requests: `Content-Type: application/fhir+json` or `application/json`.
- Responses with a body: `Content-Type: application/fhir+json;charset=UTF-8`.
- FHIR version: R4 (4.0.1). JSON only.
- Server assigns ids: numeric strings from a DB sequence. Any `id` in a POST body is ignored.
- Every stored resource returns `meta.versionId` (string integer, starting `"1"`) and
  `meta.lastUpdated` (UTC instant).
- `ETag: W/"<versionId>"` on read, create and update responses.
- Only the elements listed in §4 are persisted and returned. Other elements in a request are
  ignored silently (not an error).
- Error responses always carry an `OperationOutcome` body with at least one `issue` of
  `severity=error`. Status code and `issue.code` follow this table:

| Situation | HTTP | issue.code |
|---|---|---|
| Body is not parseable JSON / not a FHIR resource / wrong `resourceType` for URL | 400 | `structure` |
| PUT body `id` missing or different from URL id | 400 | `invalid` |
| Unknown search parameter | 400 | `not-supported` |
| Malformed search parameter value (e.g. bad date) | 400 | `invalid` |
| Resource id not found (read, update) | 404 | `not-found` |
| Unknown resource type in URL | 404 | `not-supported` |
| Required element missing | 422 | `required` |
| Referenced resource does not exist | 422 | `not-found` |
| Business rule violated (§5) | 422 | `business-rule` |

## 3. Interactions

| Interaction | Request | Success |
|---|---|---|
| capabilities | `GET /fhir/metadata` | 200, `CapabilityStatement` (§6) |
| create | `POST /fhir/{type}` | 201, body = stored resource, `Location: <base>/fhir/{type}/{id}/_history/1`, `ETag` |
| read | `GET /fhir/{type}/{id}` | 200, body, `ETag` |
| update | `PUT /fhir/{type}/{id}` | 200, version incremented, body, `ETag`. Update of a non-existing id → 404 (no update-as-create) |
| search | `GET /fhir/{type}?params` | 200, `Bundle` (§7) |

No delete, no history, no `_count`/paging, no conditional operations, no XML, no `_format`.
These are deliberately left out (candidates for evolution tasks).

`<base>` = scheme + host + port of the request (as seen by the server), e.g. `http://localhost:8080`.

## 4. Resources and persisted elements

References are always of the form `"<Type>/<id>"` (relative). Search results and reads
return references in the same form.

### Patient
`identifier[]` (system, value) · `active` · `name[0]` (family, given[]) — only the first
name is stored · `telecom[]` (system ∈ phone|email, value) · `gender` (male|female|other|unknown)
· `birthDate`.

### Practitioner
`identifier[]` (system, value) · `active` · `name[0]` (family, given[]).

### Encounter
`status` (planned|in-progress|finished|cancelled) · `class` (Coding: system, code) ·
`subject` (Reference → Patient, required) · `participant[0].individual` (Reference →
Practitioner, optional) · `period` (start, end).

### Observation
`status` (registered|preliminary|final|amended|cancelled|entered-in-error) ·
`category[0].coding[0]` (system, code) · `code.coding[0]` (system, code, display) — required ·
`subject` (Reference → Patient, required) · `encounter` (Reference → Encounter, optional) ·
`effectiveDateTime` · `valueQuantity` (value, unit, system, code).

### Appointment
`status` (proposed|booked|cancelled|fulfilled|noshow) · `description` · `start` · `end` ·
`participant[]` (`actor` Reference → Patient or Practitioner, `status` accepted|declined|tentative|needs-action).

## 5. Validation and business rules (422)

| # | Resource | Rule | issue.code |
|---|---|---|---|
| R1 | Patient | Must have a `name[0].family` **or** at least one `identifier` with a `value` | required |
| R2 | Patient | `birthDate` must not be in the future | business-rule |
| R3 | Practitioner | `name[0].family` required | required |
| R4 | Encounter | `status` required; `subject` required | required |
| R5 | Encounter | `subject` Patient and `participant[0].individual` Practitioner (if present) must exist | not-found |
| R6 | Encounter | if both present, `period.end` ≥ `period.start` | business-rule |
| R7 | Observation | `status`, `code.coding[0].code`, `subject` required | required |
| R8 | Observation | `subject` Patient and `encounter` Encounter (if present) must exist | not-found |
| R9 | Observation | if `valueQuantity` present, `valueQuantity.value` required | required |
| R10 | Appointment | `status`, `start`, `end`, ≥1 `participant` with `actor` required | required |
| R11 | Appointment | `end` > `start` | business-rule |
| R12 | Appointment | every participant actor must exist | not-found |
| R13 | Appointment | a Practitioner must not be in two `booked` Appointments whose `[start, end)` intervals overlap (checked on create and update, ignoring the appointment being updated) | business-rule |

The same rules apply to create and update. Rules are checked in table order; the first
violated rule determines the response.

## 6. CapabilityStatement

`GET /fhir/metadata` returns `status=active`, `kind=instance`, `fhirVersion=4.0.1`,
`format=["json"]`, one `rest` entry with `mode=server` and, per resource type in §4, a
`resource` entry with `type`, `interaction` codes `read`, `create`, `update`, `search-type`,
and `searchParam` entries (name + type) exactly as listed in §7.

## 7. Search

Result: `Bundle` with `type=searchset`, `total` = number of matches, and one `entry` per
match (`fullUrl` = `<base>/fhir/{type}/{id}`, `resource`, `search.mode=match`), ordered by
id ascending. No paging: all matches are returned. Multiple parameters are combined with AND.
A parameter repeated in the query is an AND of its values.

| Resource | Parameter | Type | Semantics |
|---|---|---|---|
| Patient | `family` | string | case-insensitive prefix match on `name[0].family` |
| Patient | `given` | string | case-insensitive prefix match on any given name |
| Patient | `identifier` | token | `system|value` exact, or `value` exact (any system) |
| Patient | `gender` | token | exact |
| Patient | `birthdate` | date | `YYYY-MM-DD` with optional prefix `eq` (default), `ne`, `lt`, `le`, `gt`, `ge` |
| Practitioner | `family` | string | as Patient |
| Practitioner | `identifier` | token | as Patient |
| Encounter | `patient` | reference | `<id>` or `Patient/<id>` |
| Encounter | `status` | token | exact |
| Observation | `patient` | reference | as Encounter |
| Observation | `code` | token | `system|code` exact, or `code` exact |
| Observation | `category` | token | code exact (system ignored) |
| Appointment | `patient` | reference | Appointment has a participant actor `Patient/<id>` |
| Appointment | `practitioner` | reference | Appointment has a participant actor `Practitioner/<id>` |
| Appointment | `status` | token | exact |
| Appointment | `date` | date | compares the **date part** of `start` (UTC) with the same prefixes as `birthdate` |

Any other parameter (including `_count`, `_sort`, `_include`) → 400 `not-supported`.
A search with no parameters returns all resources of the type.

## 8. Clarifications (binding; resolved during baseline construction)

| # | Question | Decision |
|---|---|---|
| C1 | Appointment participant without `actor` | Every participant must have an `actor`; otherwise 422 `required` (part of R10) |
| C2 | Invalid code for a bound enumeration (e.g. `Encounter.status = "bogus"`) | Rejected by the JSON parser → 400 `structure`. No extra 422 rules beyond §5 |
| C3 | Order of checks on PUT | parse (400 `structure`) → body id missing/mismatched (400 `invalid`) → unknown id (404) → rules R1–R13 (422) |
| C4 | Malformed reference search value (`patient=abc`, `patient=Practitioner/1`) | 400 `invalid` |
| C5 | Reference of the wrong type in a body (e.g. `Encounter.subject = Practitioner/1`) | 422 `not-found` |
| C6 | Token search given as `system|code` for `category` | system part ignored, code compared |
| C7 | Unsupported HTTP method on a known resource type (e.g. `DELETE /fhir/Patient/1`) | 405, OperationOutcome `not-supported` |
| C8 | Unknown resource type (any method) | 404 `not-supported` |
| C9 | Rule order vs reference problems | Reference checks (wrong type, non-numeric id, missing target) are R5/R8/R12 and are evaluated **in table order**, after required-element rules; on PUT they come after the 404 existence check (C3) |
| C10 | Token search `|code` (empty system) | matches only rows whose system is absent (FHIR semantics) |
| C11 | Code valid in FHIR R4 but not in the §4 list (e.g. `Encounter.status = arrived`, `telecom.system = fax`, `Appointment.status = pending`) | 400 `structure`, same as C2; on PUT this wins over the 404 (it is a parse-stage error per C3) |
| C12 | Non-numeric id in URL (`GET/PUT /fhir/Patient/abc`) | 404 `not-found` |
| C13 | Token search value that is not a known code (`gender=bogus`) | not an error: 200 with an empty searchset |
