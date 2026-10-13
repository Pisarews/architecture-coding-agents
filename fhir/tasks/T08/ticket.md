# SMS reminder when an appointment is booked

When an Appointment ends up with status `booked` (on create, or on an update from any other
status to `booked`), send an SMS through the gateway at configuration property
`fhir.sms.gateway-url`. For each Patient participant with a `phone` telecom, send `POST` with
JSON `{"to": "<phone>", "text": "Appointment on <YYYY-MM-DD> at <HH:mm> UTC"}` using the
appointment start.

**Acceptance criteria**
- One POST per patient with a phone.
- No POST for patients without a phone, for appointments that are not booked, or for updates
  that keep status `booked`.
- A gateway failure does not fail the request.
