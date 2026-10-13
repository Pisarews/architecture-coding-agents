# Search observations by date

Support the `date` search parameter on Observation. It matches `effectiveDateTime` with the
same prefixes as the existing date parameters (`eq` default, `ne`, `lt`, `le`, `gt`, `ge`). The
value is a date (`YYYY-MM-DD`) compared to the UTC date part of `effectiveDateTime`.
Observations without `effectiveDateTime` never match a `date` search.

**Acceptance criteria**
- `GET /fhir/Observation?date=<value>` works alone and combined with `patient` and `code`.
- The CapabilityStatement lists `date` (type `date`) for Observation.
- A malformed value → HTTP 400 with an OperationOutcome whose issue code is `invalid`.
