package org.archagents.fhir.domain;

import java.math.BigDecimal;

public record Quantity(BigDecimal value, String unit, String system, String code) {
}
