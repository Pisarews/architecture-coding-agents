package org.archagents.fhir.util;

import org.hl7.fhir.r4.model.Reference;

/** A relative reference of the form {@code Type/id} with a numeric id. */
public record ResourceRef(String type, long id) {

    /** Returns the parsed reference, or {@code null} if it is not a relative {@code Type/id} reference. */
    public static ResourceRef parse(String reference) {
        if (reference == null) {
            return null;
        }
        String[] parts = reference.split("/");
        if (parts.length != 2 || parts[0].isEmpty()) {
            return null;
        }
        try {
            return new ResourceRef(parts[0], Long.parseLong(parts[1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isPresent(Reference reference) {
        return reference != null && reference.hasReference();
    }

    public Reference toReference() {
        return new Reference(type + "/" + id);
    }

    @Override
    public String toString() {
        return type + "/" + id;
    }
}
