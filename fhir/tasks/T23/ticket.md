# Audit trail as AuditEvent resources

Every successful create and update is recorded as a FHIR `AuditEvent` with these fields:
- `type`: coding `http://terminology.hl7.org/CodeSystem/audit-event-type|rest`
- `action`: `C` (create) or `U` (update)
- `recorded`: timestamp
- `outcome`: `0`
- `entity[0].what`: reference with version, e.g. `Patient/12/_history/2`

Support `GET /fhir/AuditEvent/{id}` and search on AuditEvent with parameter `entity` (a
reference without version, e.g. `Patient/12`).

**Acceptance criteria**
- Each create or update produces exactly one AuditEvent. Failed requests produce none.
- AuditEvents are read-only: `POST` and `PUT` → HTTP 405 with an OperationOutcome whose issue
  code is `not-supported`.
- The CapabilityStatement lists AuditEvent with the interactions `read` and `search-type`.
