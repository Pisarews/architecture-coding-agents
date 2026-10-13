package org.archagents.fhir.domain;

import java.time.Instant;

/** A resource as held by the server: its content together with the server-assigned id and version. */
public record StoredResource<T>(long id, ResourceMeta meta, T content) {

    public StoredResource<T> revise(T newContent, Instant updatedAt) {
        return new StoredResource<>(id, meta.next(updatedAt), newContent);
    }
}
