# Heart-rate observations must be plausible

Heart-rate readings of 0 or 1000 beats per minute have been entered by mistake. From now on an
Observation with LOINC code `8867-4` (heart rate) that has a `valueQuantity` must have a value
between 20 and 300 inclusive.

**Acceptance criteria**
- Creating or updating a heart-rate Observation with a value below 20 or above 300 → HTTP 422 with an
  OperationOutcome whose issue code is `business-rule`.
- Values 20 and 300 are accepted.
- Observations with other codes are not affected.
