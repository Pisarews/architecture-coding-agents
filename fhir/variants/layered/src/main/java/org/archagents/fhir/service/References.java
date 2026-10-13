package org.archagents.fhir.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.repository.CrudRepository;

/**
 * Resolution of relative {@code <type>/<id>} references to stored resources.
 */
final class References {

    private static final Pattern RELATIVE_REFERENCE = Pattern.compile("([A-Za-z]+)/(\\d{1,18})");

    private References() {
    }

    record Target(String resourceType, long id) {
    }

    static Optional<Target> parse(String reference) {
        Matcher matcher = RELATIVE_REFERENCE.matcher(reference);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new Target(matcher.group(1), Long.parseLong(matcher.group(2))));
    }

    /**
     * Returns the id of the referenced resource, which must be of the expected type and exist.
     */
    static long resolve(String reference, String expectedType, CrudRepository<?, Long> repository) {
        return parse(reference)
                .filter(target -> target.resourceType().equals(expectedType))
                .map(Target::id)
                .filter(repository::existsById)
                .orElseThrow(() -> ResourceValidationException.referenceNotFound(reference));
    }
}
