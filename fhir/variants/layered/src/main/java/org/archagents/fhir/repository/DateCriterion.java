package org.archagents.fhir.repository;

import java.time.LocalDate;

public record DateCriterion(Comparator comparator, LocalDate date) {

    public enum Comparator { EQ, NE, LT, LE, GT, GE }
}
