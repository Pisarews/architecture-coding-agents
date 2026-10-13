package org.archagents.fhir.domain;

import java.time.LocalDate;

/** Compares a calendar date against {@code date} using {@code prefix}. */
public record DateCriterion(Prefix prefix, LocalDate date) {

    public enum Prefix {
        EQ, NE, LT, LE, GT, GE
    }
}
