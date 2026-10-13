# Mirror patients to the regional FHIR server

After a Patient is created or updated, send it to the regional server at configuration property
`fhir.mirror.base-url` with `PUT {base-url}/Patient/{local-id}` and the Patient as FHIR JSON
body. Add an identifier `urn:archagents:local-id|<id>` (system `urn:archagents:local-id`, value
the local id) to the mirrored copy, but not to the stored patient.

**Acceptance criteria**
- One PUT per create or update, with the body as described.
- Mirror failures don't fail the request.
- The local response has no trace of the mirror identifier.
- If the property is not set, nothing is sent.
