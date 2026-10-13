package org.archagents.fhir.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.archagents.fhir.domain.Identifier;

@Embeddable
class EmbeddedIdentifier {

    @Column(name = "ident_system")
    private String system;

    @Column(name = "ident_value")
    private String value;

    protected EmbeddedIdentifier() {
    }

    private EmbeddedIdentifier(String system, String value) {
        this.system = system;
        this.value = value;
    }

    static EmbeddedIdentifier from(Identifier identifier) {
        return new EmbeddedIdentifier(identifier.system(), identifier.value());
    }

    Identifier toDomain() {
        return new Identifier(system, value);
    }
}
