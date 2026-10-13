package org.archagents.fhir.domain;

import java.time.Instant;

public record ResourceMeta(int versionId, Instant lastUpdated) {

    public static ResourceMeta initial(Instant createdAt) {
        return new ResourceMeta(1, createdAt);
    }

    public ResourceMeta next(Instant updatedAt) {
        return new ResourceMeta(versionId + 1, updatedAt);
    }
}
