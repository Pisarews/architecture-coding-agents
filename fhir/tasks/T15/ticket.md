# `Prefer: return=minimal` on create and update

Integrations upload large volumes and don't need echoed bodies. Support the `Prefer` header on
create and update for all resource types.

**Acceptance criteria**
- `Prefer: return=minimal` → empty body, with the same status code and the same `Location` and
  `ETag` headers as today.
- `Prefer: return=representation` or no `Prefer` header → as today.
- `Prefer: return=OperationOutcome` → same status code and headers as today, and the body is an
  OperationOutcome with exactly one issue of severity `information`.
