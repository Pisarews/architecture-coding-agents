# Body-weight observations must use mass units

Clinicians found body-weight observations recorded in "cm" and "[lb_av]". From now on an
Observation with LOINC code `29463-7` (body weight) that has a `valueQuantity` must use UCUM
system `http://unitsofmeasure.org` and code `kg` or `g`.

**Acceptance criteria**
- Creating or updating a body-weight Observation that violates this → HTTP 422 with an
  OperationOutcome whose issue code is `business-rule`.
- Body weight in `kg` or `g` is still accepted.
- Observations with other codes are not affected.
