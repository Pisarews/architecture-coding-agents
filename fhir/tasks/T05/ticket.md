# Appointment duration limits

Booked appointments shorter than 5 minutes or longer than 4 hours are data-entry errors.

**Acceptance criteria**
- Creating or updating an Appointment with status `booked` whose duration (`end` − `start`) is
  less than 5 minutes or more than 240 minutes → HTTP 422 with an OperationOutcome whose issue
  code is `business-rule`.
- Exactly 5 and exactly 240 minutes are allowed.
- Appointments with other statuses are not limited.
