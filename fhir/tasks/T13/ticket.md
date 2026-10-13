# Finished encounters need an end time

**Acceptance criteria**
- An Encounter with status `finished` and no `period.end` → HTTP 422 with an OperationOutcome
  whose issue code is `required` (on create and on update).
- Encounters with other statuses are not affected.
