# Verify practitioners against the licensing registry

When a Practitioner is created with an identifier of system `urn:npwz`, check it against the
licensing registry at configuration property `fhir.registry.base-url`:
`GET {base-url}/licenses/{value}`. The registry returns 200 with `{"status":"ACTIVE"|"SUSPENDED"}`
or 404.

**Acceptance criteria**
- Registry 404 → HTTP 422 with an OperationOutcome whose issue code is `business-rule`.
- `SUSPENDED` → the practitioner is stored with `active = false`.
- `ACTIVE` → the practitioner is stored with `active = true`.
- If the registry is unreachable or returns 5xx → HTTP 503 with issue code `transient`, and
  nothing is stored.
- Practitioners without a `urn:npwz` identifier are not checked.
- Updates are not checked.
