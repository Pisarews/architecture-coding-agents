# Inactive practitioners cannot be booked

**Acceptance criteria**
- An Appointment with status `booked` whose Practitioner participant has `active = false`
  → HTTP 422 with an OperationOutcome whose issue code is `business-rule` (on create and on update).
- Practitioners with `active` true or absent can be booked as before.
