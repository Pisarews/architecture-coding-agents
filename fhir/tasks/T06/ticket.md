# Delete patients

Support `DELETE /fhir/Patient/{id}`.

**Acceptance criteria**
- Deleting an existing patient with no Encounter, Observation or Appointment referring to them
  → HTTP 204.
- A later read of that patient → HTTP 410 with an OperationOutcome whose issue code is `deleted`,
  and the patient no longer appears in search results.
- Deleting a patient that is still referenced → HTTP 409 with issue code `conflict`, and nothing
  is deleted.
- Unknown id → HTTP 404 with issue code `not-found`.
- The CapabilityStatement lists the `delete` interaction for Patient only.
