# Search encounters by practitioner and date

Add two Encounter search parameters:
- `practitioner` (type `reference`): the Encounter's participant practitioner, in the same forms
  as `patient` (`<id>` or `Practitioner/<id>`).
- `date` (type `date`): matches if the given UTC date falls within `[period.start, period.end]`,
  compared by date part. An open end (no `period.end`) means "until now". Only the prefix `eq`
  and the default (no prefix) are supported.

**Acceptance criteria**
- `GET /fhir/Encounter?practitioner=<ref>` and `GET /fhir/Encounter?date=<YYYY-MM-DD>` work alone
  and together with `patient` and `status`.
- The CapabilityStatement lists `practitioner` (type `reference`) and `date` (type `date`) for
  Encounter.
