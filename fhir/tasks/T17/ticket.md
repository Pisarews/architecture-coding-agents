# Deceased patients

Store `Patient.deceasedBoolean` or `Patient.deceasedDateTime`.

**Acceptance criteria**
- Both forms round-trip on create, read and update.
- `deceasedDateTime` in the future → HTTP 422 with an OperationOutcome whose issue code is
  `business-rule`.
- Creating or updating an Appointment with status `booked` that has a deceased Patient as a
  participant → HTTP 422 with issue code `business-rule`.
