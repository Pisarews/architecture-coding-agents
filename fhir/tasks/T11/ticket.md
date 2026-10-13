# `_summary=count` on every search

Dashboards only need counts. All search endpoints must accept `_summary=count`.

**Acceptance criteria**
- `_summary=count` returns a Bundle of type `searchset` with the correct `total` and no entries.
- It combines with every existing search parameter.
- Other `_summary` values → HTTP 400 with an OperationOutcome whose issue code is `not-supported`.
