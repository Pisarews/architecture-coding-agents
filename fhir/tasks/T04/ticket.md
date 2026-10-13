# Notify on critical glucose values

When an Observation is **created** with LOINC `2339-0` (glucose) and `valueQuantity` greater
than 300 mg/dL (UCUM code `mg/dL`), send the stored Observation as FHIR JSON with an HTTP `POST`
to the URL in configuration property `fhir.notifications.critical-url`
(`Content-Type: application/fhir+json`).

**Acceptance criteria**
- Exactly one POST per qualifying create, and the body carries the server-assigned id.
- No POST for values ≤ 300, other codes, or updates.
- If the property is not set, nothing is sent.
- If the notification endpoint fails, the create still succeeds (201).
