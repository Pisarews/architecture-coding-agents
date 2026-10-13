package org.archagents.fhir.domain;

import java.time.Instant;

public record Period(Instant start, Instant end) {

    public boolean endsBeforeStart() {
        return start != null && end != null && end.isBefore(start);
    }
}
