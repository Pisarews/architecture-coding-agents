package org.archagents.fhir.domain;

import java.util.regex.Pattern;

/**
 * Relative reference to another resource, {@code <type>/<id>}. The parts are kept as received so that a
 * reference to an unknown type or a non-numeric id can be reported as unresolvable.
 */
public record Reference(String type, String id) {

    private static final Pattern NUMERIC_ID = Pattern.compile("\\d{1,18}");

    public static Reference to(ResourceType type, long id) {
        return new Reference(type.typeName(), Long.toString(id));
    }

    public boolean pointsTo(ResourceType resourceType) {
        return resourceType.typeName().equals(type) && id != null && NUMERIC_ID.matcher(id).matches();
    }

    public long idAsLong() {
        return Long.parseLong(id);
    }

    @Override
    public String toString() {
        return type + "/" + id;
    }
}
