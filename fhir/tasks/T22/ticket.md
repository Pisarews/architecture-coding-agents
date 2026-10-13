# Read a specific version (vread)

Support `GET /fhir/{type}/{id}/_history/{vid}` for all resource types.

**Acceptance criteria**
- Returns the resource exactly as it was at version `vid`, with that version's `meta` and `ETag`.
- Unknown version → HTTP 404 with an OperationOutcome whose issue code is `not-found`.
- The `Location` header returned by create already points to this URL and now resolves.
- The CapabilityStatement lists the `vread` interaction.
