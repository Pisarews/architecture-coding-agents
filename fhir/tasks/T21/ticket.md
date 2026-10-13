# Observation status transitions

Clinical safety: a `final` Observation can only be updated to `amended`, `corrected`, `final`
or `entered-in-error`. An `entered-in-error` Observation can no longer be changed at all.

**Acceptance criteria**
- A disallowed transition on update → HTTP 422 with an OperationOutcome whose issue code is
  `business-rule`, and nothing is changed.
- All other transitions behave as today.
