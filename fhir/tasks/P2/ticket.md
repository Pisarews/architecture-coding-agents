# Search practitioners by given name

Front-desk staff often only know a doctor's first name. Support the search parameter `given` on
Practitioner.

**Acceptance criteria**
- `GET /fhir/Practitioner?given=<text>` matches practitioners whose given name starts with `<text>`,
  case-insensitively (same semantics as `given` on Patient).
- It can be combined with the existing Practitioner search parameters.
- The CapabilityStatement lists `given` (type `string`) for Practitioner.
