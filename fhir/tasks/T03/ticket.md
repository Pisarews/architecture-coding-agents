# Optimistic locking on update

Two front-desk users overwrite each other's changes. Every `PUT` must honor an optional
`If-Match` header (`W/"<versionId>"`, as returned in `ETag`).

**Acceptance criteria**
- `If-Match` with the current version → normal update.
- `If-Match` with any other version → HTTP 412 with an OperationOutcome whose issue code is
  `conflict`, and nothing is changed.
- No `If-Match` header → update as today.
- This applies to all five resource types.
