# FHIR domain — task sequence T01–T24 (DRAFT)

Status: **frozen for Phase 1** (ticket.md files in T##/ are authoritative; small wording clarifications listed in their commit history). Freeze before registration. Each task will get its own directory
`T##/` with `ticket.md` (the text below, verbatim), `meta.yaml`, and hidden tests in
`../hidden-tests/T##/`. Every hidden test must **fail on the baseline** of all three variants
and pass on a reference solution in all three.

Ticket-writing rules: describe externally observable behavior only. Never name files,
classes, packages, or layers. The HTTP contract and configuration property names are
external interface, so they may be named. Each ticket ends with acceptance criteria that the
agent can see; the hidden tests check exactly these criteria and nothing more.

Strata: **D** domain rule (1 unit), **F** feature (3+ units), **C** cross-cutting (many units),
**I** integration (new outbound adapter). 6 of each, interleaved.

For integration tasks, the harness starts the app with the listed property pointing at a
mock server that the hidden tests control.

## Phase 1 selection (12 of 24, 3 per stratum, original order kept)

`T01 T02 T03 T04 T06 T07 T08 T09 T10 T11 T12 T21`

| Stratum | Tasks |
|---|---|
| D | T01, T09, T21 |
| F | T02, T06, T10 |
| C | T03, T07, T11 |
| I | T04, T08, T12 |

The other 12 are reserved for Phase 2.

## All tasks

| # | Stratum | Title | Depends on |
|---|---|---|---|
| T01 | D | Body-weight observations must use mass units | — |
| T02 | F | Search observations by date | — |
| T03 | C | Optimistic locking on update (If-Match) | — |
| T04 | I | Notify on critical glucose values | — |
| T05 | D | Appointment duration limits | — |
| T06 | F | Delete patients | — |
| T07 | C | Request correlation id | — |
| T08 | I | SMS reminder when an appointment is booked | — |
| T09 | D | Inactive practitioners cannot be booked | — |
| T10 | F | Patient address and city search | — |
| T11 | C | `_summary=count` on every search | — |
| T12 | I | Verify practitioners against the licensing registry | — |
| T13 | D | Finished encounters need an end time | — |
| T14 | F | Search encounters by practitioner and date | — |
| T15 | C | `Prefer: return=minimal` on create and update | — |
| T16 | I | Validate LOINC codes against a terminology server | — |
| T17 | D | Deceased patients | — |
| T18 | F | Paging for search results | — |
| T19 | C | Conditional read (If-None-Match → 304) | T03 (soft) |
| T20 | I | Change feed webhook | — |
| T21 | D | Observation status transitions | — |
| T22 | F | Read a specific version (vread) | — |
| T23 | C | Audit trail as AuditEvent resources | — |
| T24 | I | Mirror patients to the regional FHIR server | — |

---

### T01 · D · Body-weight observations must use mass units
Clinicians found body-weight observations recorded in "cm" and "[lb_av]". From now on an
Observation with LOINC code `29463-7` (body weight) that has a `valueQuantity` must use UCUM
system `http://unitsofmeasure.org` and code `kg` or `g`.
**Acceptance:** create or update that violates this → 422, OperationOutcome issue code
`business-rule`. Body weight in `kg` or `g` is still accepted. Other observation codes are unaffected.

### T02 · F · Search observations by date
Support the `date` search parameter on Observation. It matches `effectiveDateTime` with the
same prefixes as the existing date parameters (`eq` default, `ne`, `lt`, `le`, `gt`, `ge`). The
value is a date (`YYYY-MM-DD`) compared to the UTC date part of `effectiveDateTime`.
Observations without `effectiveDateTime` never match a `date` search.
**Acceptance:** search works alone and combined with `patient`/`code`. The CapabilityStatement
lists `date` (type `date`) for Observation. A malformed value → 400 `invalid`.

### T03 · C · Optimistic locking on update
Two front-desk users overwrite each other's changes. Every `PUT` must honor an optional
`If-Match` header (`W/"<versionId>"`, as returned in `ETag`).
**Acceptance:** matching version → normal update. Non-matching version → 412, OperationOutcome
issue code `conflict`, and nothing is changed. No header → update as today. This applies to all
five resource types.

### T04 · I · Notify on critical glucose values
When an Observation is **created** with LOINC `2339-0` (glucose) and `valueQuantity` greater
than 300 mg/dL (UCUM code `mg/dL`), send the stored Observation as FHIR JSON with an HTTP `POST`
to the URL in configuration property `fhir.notifications.critical-url`
(`Content-Type: application/fhir+json`).
**Acceptance:** exactly one POST per qualifying create, and the body carries the server-assigned
id. No POST for values ≤ 300, other codes, or updates. If the property is not set, nothing is
sent. If the notification endpoint fails, the create still succeeds (201).

### T05 · D · Appointment duration limits
Booked appointments shorter than 5 minutes or longer than 4 hours are data-entry errors.
**Acceptance:** creating or updating an Appointment with status `booked` and duration
< 5 min or > 240 min → 422 `business-rule`. Exactly 5 and exactly 240 minutes are allowed. Other
statuses are not limited.

### T06 · F · Delete patients
Support `DELETE /fhir/Patient/{id}`.
**Acceptance:** an existing patient with no Encounter, Observation or Appointment referring to
them → 204. A later read → 410 with OperationOutcome code `deleted`, and the patient no longer
appears in search results. A patient that is still referenced → 409, code `conflict`, nothing
deleted. Unknown id → 404 `not-found`. The CapabilityStatement lists the `delete` interaction
for Patient only.

### T07 · C · Request correlation id
Operations wants to trace requests across systems. Every response must carry the header
`X-Request-Id`.
**Acceptance:** if the request has `X-Request-Id`, the response echoes it unchanged. Otherwise
the server generates a new UUID. On error responses, the OperationOutcome's first issue has
`diagnostics` that contain the request id.

### T08 · I · SMS reminder when an appointment is booked
When an Appointment ends up with status `booked` (on create, or on an update from any other
status to `booked`), send an SMS through the gateway at configuration property
`fhir.sms.gateway-url`. For each Patient participant with a `phone` telecom, send `POST` with
JSON `{"to": "<phone>", "text": "Appointment on <YYYY-MM-DD> at <HH:mm> UTC"}` using the
appointment start.
**Acceptance:** one POST per patient with a phone. No POST for patients without a phone, for
non-booked appointments, or for updates that keep status `booked`. A gateway failure does not
fail the request.

### T09 · D · Inactive practitioners cannot be booked
**Acceptance:** an Appointment with status `booked` whose Practitioner participant has
`active = false` → 422 `business-rule` (on create and on update). Practitioners with `active`
true or absent can be booked as before.

### T10 · F · Patient address and city search
Store `Patient.address` (every address: `line[]`, `city`, `postalCode`, `country`) and
support search parameter `address-city` (case-insensitive prefix match on any address city).
**Acceptance:** addresses round-trip on read and update. Search works alone and combined
with other Patient parameters. The CapabilityStatement lists `address-city` (type `string`).

### T11 · C · `_summary=count` on every search
Dashboards only need counts. All search endpoints must accept `_summary=count`.
**Acceptance:** a Bundle of type `searchset` with the correct `total` and no entries. It
combines with every existing search parameter. Other `_summary` values → 400 `not-supported`.

### T12 · I · Verify practitioners against the licensing registry
When a Practitioner is created with an identifier of system `urn:npwz`, check it against the
licensing registry at configuration property `fhir.registry.base-url`:
`GET {base-url}/licenses/{value}`. The registry returns 200 with `{"status":"ACTIVE"|"SUSPENDED"}`
or 404.
**Acceptance:** 404 → 422 `business-rule`. `SUSPENDED` → the practitioner is stored with
`active = false`. `ACTIVE` → stored with `active = true`. If the registry is unreachable or
returns 5xx → 503, OperationOutcome code `transient`, and nothing is stored. Practitioners
without a `urn:npwz` identifier are not checked. Updates are not checked.

### T13 · D · Finished encounters need an end time
**Acceptance:** an Encounter with status `finished` and no `period.end` → 422 `required` (on
create and update). Other statuses are unaffected.

### T14 · F · Search encounters by practitioner and date
Add Encounter search parameters `practitioner` (reference, same forms as `patient`) and `date`
(matches if the UTC date given falls within `[period.start, period.end]` by date part. An
open end means "until now". Prefixes `eq` and the default only).
**Acceptance:** both parameters work alone and together with `patient` and `status`. The
CapabilityStatement lists them.

### T15 · C · `Prefer: return=minimal`
Integrations upload large volumes and don't need echoed bodies. Support the `Prefer` header
on create and update for all resource types.
**Acceptance:** `Prefer: return=minimal` → empty body, same status, `Location` and `ETag`
headers as today. `return=representation` or no header → as today.
`return=OperationOutcome` → an OperationOutcome body with one `information` issue.

### T16 · I · Validate LOINC codes against a terminology server
For Observations whose `code.coding[0].system` is `http://loinc.org`, validate the code on
create and update. Call `GET {fhir.terminology.base-url}/CodeSystem/$validate-code?url=http://loinc.org&code=<code>`.
The terminology server returns a FHIR `Parameters` resource with a boolean parameter `result`.
**Acceptance:** `result = false` → 422 `code-invalid`. `true` → proceed as today. Server
unreachable or 5xx → the Observation is accepted (fail open). If the property is not
configured, no validation happens. Non-LOINC codings are not validated.

### T17 · D · Deceased patients
Store `Patient.deceasedBoolean` or `Patient.deceasedDateTime`.
**Acceptance:** both forms round-trip. `deceasedDateTime` in the future → 422 `business-rule`.
Creating or updating a `booked` Appointment that involves a deceased Patient → 422
`business-rule`.

### T18 · F · Paging for search results
Support `_count` (page size, 1–100) and paging links on all searches.
**Acceptance:** with `_count`, at most `_count` entries are returned. `total` still counts all
matches. `Bundle.link` has `self` and, when more results exist, `next`. Following `next`
returns the next page in the same order (by id ascending), with no duplicates or gaps.
`_count` outside 1–100 → 400 `invalid`. Without `_count`, behavior is unchanged.

### T19 · C · Conditional read
Support `If-None-Match` on read for all resource types.
**Acceptance:** `If-None-Match: W/"<current versionId>"` → 304 with no body and the `ETag`
header. A different version → 200 as today. Every read response also carries a
`Last-Modified` header (RFC 1123) derived from `meta.lastUpdated`.

### T20 · I · Change feed webhook
Downstream analytics wants a change feed. After every successful create, update (and delete,
if supported) of any resource, `POST` JSON `{"type":"<ResourceType>","id":"<id>","versionId":"<v>","action":"create"|"update"|"delete"}`
to configuration property `fhir.events.webhook-url`.
**Acceptance:** one POST per change, sent after the change is committed. No POST for failed
requests. Webhook failure does not fail the request. If the property is not set, nothing is sent.

### T21 · D · Observation status transitions
Clinical safety: a `final` Observation can only be updated to `amended`, `corrected`, `final`
or `entered-in-error`. An `entered-in-error` Observation can no longer be changed at all.
**Acceptance:** disallowed transitions on update → 422 `business-rule`, nothing changed. All
other transitions behave as today.

### T22 · F · Read a specific version (vread)
Support `GET /fhir/{type}/{id}/_history/{vid}` for all resource types.
**Acceptance:** returns the resource exactly as it was at version `vid`, with that version's
`meta` and `ETag`. Unknown version → 404 `not-found`. The `Location` header after
create already points to this URL and now resolves. The CapabilityStatement lists
`vread`.

### T23 · C · Audit trail
Every successful create and update is recorded as a FHIR `AuditEvent`. Fields: `type` coding
`http://terminology.hl7.org/CodeSystem/audit-event-type|rest`, `action` `C` or `U`,
`recorded` timestamp, `outcome` `0`, `entity[0].what` = reference with version
(`Patient/12/_history/2`). Support `GET /fhir/AuditEvent/{id}` and search `entity` (reference
without version, e.g. `Patient/12`).
**Acceptance:** each create/update produces exactly one AuditEvent. Failed requests produce
none. AuditEvents are read-only (POST/PUT → 405). The CapabilityStatement lists AuditEvent
with `read` and `search-type`.

### T24 · I · Mirror patients to the regional FHIR server
After a Patient is created or updated, send it to the regional server at configuration
property `fhir.mirror.base-url` with `PUT {base-url}/Patient/{local-id}` and FHIR JSON body.
Add an identifier `urn:archagents:local-id|<id>` to the mirrored copy (not to the stored
patient).
**Acceptance:** one PUT per create/update, with the body as described. Mirror failures don't
fail the request, and the local response has no trace of the mirror identifier. If the
property is not set, nothing is sent.
