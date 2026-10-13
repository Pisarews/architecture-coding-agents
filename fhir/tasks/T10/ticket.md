# Patient address and city search

Store `Patient.address` (every address: `line[]`, `city`, `postalCode`, `country`) and
support the search parameter `address-city` (case-insensitive prefix match on any address city).

**Acceptance criteria**
- Addresses round-trip on read and update.
- `GET /fhir/Patient?address-city=<text>` works alone and combined with the other Patient search
  parameters.
- The CapabilityStatement lists `address-city` (type `string`) for Patient.
