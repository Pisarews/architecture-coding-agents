# Validate LOINC codes against a terminology server

For Observations whose `code.coding[0].system` is `http://loinc.org`, validate the code on create
and on update. Call
`GET {fhir.terminology.base-url}/CodeSystem/$validate-code?url=http://loinc.org&code=<code>`,
where `fhir.terminology.base-url` is a configuration property. The terminology server returns a
FHIR `Parameters` resource with a boolean parameter `result`.

**Acceptance criteria**
- `result = false` → HTTP 422 with an OperationOutcome whose issue code is `code-invalid`, and
  nothing is stored or changed.
- `result = true` → the request proceeds as today.
- Terminology server unreachable or answering 5xx → the Observation is accepted (fail open).
- If the property is not configured, no validation happens.
- Codings with another system are not validated.
