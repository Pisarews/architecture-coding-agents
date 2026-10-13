# Conditional read

Support `If-None-Match` on read (`GET /fhir/{type}/{id}`) for all resource types.

**Acceptance criteria**
- `If-None-Match: W/"<current versionId>"` → HTTP 304 with no body and the `ETag` header.
- `If-None-Match` with a different version → HTTP 200 as today.
- Every read response also carries a `Last-Modified` header (RFC 1123 format) derived from
  `meta.lastUpdated`.
