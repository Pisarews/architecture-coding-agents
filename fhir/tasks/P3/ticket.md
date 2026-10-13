# Advertise the FHIR version on every response

Client libraries want to detect the server's FHIR version without calling `/metadata` first.

**Acceptance criteria**
- Every response from `/fhir/**` — successful or error — carries the header `X-FHIR-Version: 4.0.1`.
