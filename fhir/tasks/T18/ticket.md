# Paging for search results

Support the `_count` parameter (page size, 1–100) and paging links on all searches.

**Acceptance criteria**
- With `_count`, at most `_count` entries are returned, and `total` still counts all matches.
- `Bundle.link` has a `self` link and, when more results exist, a `next` link.
- Following `next` returns the next page in the same order (by id ascending), with no
  duplicates or gaps.
- `_count` outside 1–100 → HTTP 400 with an OperationOutcome whose issue code is `invalid`.
- Without `_count`, behavior is unchanged.
