package org.archagents.metrics;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tngtech.archunit.lang.ArchRule;

/**
 * The frozen conformance rule sets (PREREGISTRATION §4.3, H6), copied rule by rule from the baseline
 * {@code src/test/java/org/archagents/fhir/ArchitectureTest.java} of fhir/variants/layered and
 * fhir/variants/hexagonal. Keys are the field names used there. The agents may edit the copy in their
 * workspace; conformance is always measured against these.
 */
final class ConformanceRules {

    private ConformanceRules() {
    }

    static final Map<String, ArchRule> LAYERED;
    static final Map<String, ArchRule> HEXAGONAL;

    static {
        Map<String, ArchRule> l = new LinkedHashMap<>();
        l.put("layersAreRespected", layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Web").definedBy("org.archagents.fhir.web..")
                .layer("Service").definedBy("org.archagents.fhir.service..")
                .layer("Repository").definedBy("org.archagents.fhir.repository..")
                .layer("Entity").definedBy("org.archagents.fhir.entity..")
                .whereLayer("Web").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Web")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                .whereLayer("Web").mayOnlyAccessLayers("Service", "Entity")
                .whereLayer("Service").mayOnlyAccessLayers("Repository", "Entity")
                .whereLayer("Repository").mayOnlyAccessLayers("Entity")
                .whereLayer("Entity").mayNotAccessAnyLayer());
        l.put("fhirModelOnlyInWeb", noClasses()
                .that().resideOutsideOfPackage("org.archagents.fhir.web..")
                .should().dependOnClassesThat().resideInAnyPackage("org.hl7.fhir..", "ca.uhn.fhir.."));
        l.put("noPackageCycles", slices()
                .matching("org.archagents.fhir.(*)..")
                .should().beFreeOfCycles());
        l.put("noHapiServerModules", noClasses()
                .that().resideInAPackage("org.archagents.fhir..")
                .should().dependOnClassesThat().resideInAnyPackage("ca.uhn.fhir.rest.server..", "ca.uhn.fhir.jpa.."));
        l.put("noFieldInjection", NO_CLASSES_SHOULD_USE_FIELD_INJECTION);
        LAYERED = Collections.unmodifiableMap(l);

        String root = "org.archagents.fhir";
        Map<String, ArchRule> h = new LinkedHashMap<>();
        h.put("domainDependsOnlyOnJavaAndItself", classes()
                .that().resideInAPackage(root + ".domain..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("java..", root + ".domain.."));
        h.put("applicationDependsOnlyOnDomainAndItself", classes()
                .that().resideInAPackage(root + ".application..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..",
                        root + ".domain..",
                        root + ".application..",
                        "org.springframework.transaction.annotation.."));
        h.put("webAdapterDoesNotAccessOutgoingAdapters", noClasses()
                .that().resideInAPackage(root + ".adapter.in..")
                .should().dependOnClassesThat().resideInAPackage(root + ".adapter.out.."));
        h.put("outgoingAdaptersDoNotAccessIncomingAdapters", noClasses()
                .that().resideInAPackage(root + ".adapter.out..")
                .should().dependOnClassesThat().resideInAPackage(root + ".adapter.in.."));
        h.put("webAdapterUsesApplicationOnlyThroughIncomingPorts", noClasses()
                .that().resideInAPackage(root + ".adapter.in.web..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        root + ".application.service..", root + ".application.port.out.."));
        h.put("fhirModelOnlyInWebAdapter", noClasses()
                .that().resideOutsideOfPackage(root + ".adapter.in.web..")
                .should().dependOnClassesThat().resideInAnyPackage("org.hl7.fhir..", "ca.uhn.fhir.."));
        h.put("jpaOnlyInPersistenceAdapter", noClasses()
                .that().resideOutsideOfPackage(root + ".adapter.out.persistence..")
                .should().dependOnClassesThat().resideInAPackage("jakarta.persistence.."));
        h.put("noPackageCycles", slices()
                .matching(root + ".(**)")
                .should().beFreeOfCycles());
        h.put("noHapiServerModules", noClasses()
                .that().resideInAPackage(root + "..")
                .should().dependOnClassesThat().resideInAnyPackage("ca.uhn.fhir.rest.server..", "ca.uhn.fhir.jpa.."));
        h.put("noFieldInjection", NO_CLASSES_SHOULD_USE_FIELD_INJECTION);
        HEXAGONAL = Collections.unmodifiableMap(h);
    }
}
