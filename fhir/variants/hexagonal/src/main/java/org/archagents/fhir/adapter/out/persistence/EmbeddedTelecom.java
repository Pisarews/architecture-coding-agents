package org.archagents.fhir.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.archagents.fhir.domain.ContactPoint;
import org.archagents.fhir.domain.TelecomSystem;

@Embeddable
class EmbeddedTelecom {

    @Column(name = "telecom_system")
    private String system;

    @Column(name = "telecom_value")
    private String value;

    protected EmbeddedTelecom() {
    }

    private EmbeddedTelecom(String system, String value) {
        this.system = system;
        this.value = value;
    }

    static EmbeddedTelecom from(ContactPoint contactPoint) {
        String system = contactPoint.system() == null ? null : contactPoint.system().code();
        return new EmbeddedTelecom(system, contactPoint.value());
    }

    ContactPoint toDomain() {
        return new ContactPoint(system == null ? null : TelecomSystem.fromCode(system), value);
    }
}
