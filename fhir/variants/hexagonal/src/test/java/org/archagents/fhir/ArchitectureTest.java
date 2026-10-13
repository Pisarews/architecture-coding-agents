package org.archagents.fhir;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "org.archagents.fhir", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "org.archagents.fhir";

    @ArchTest
    static final ArchRule domainDependsOnlyOnJavaAndItself = classes()
            .that().resideInAPackage(ROOT + ".domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", ROOT + ".domain..");

    @ArchTest
    static final ArchRule applicationDependsOnlyOnDomainAndItself = classes()
            .that().resideInAPackage(ROOT + ".application..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "java..",
                    ROOT + ".domain..",
                    ROOT + ".application..",
                    "org.springframework.transaction.annotation..");

    @ArchTest
    static final ArchRule webAdapterDoesNotAccessOutgoingAdapters = noClasses()
            .that().resideInAPackage(ROOT + ".adapter.in..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".adapter.out..");

    @ArchTest
    static final ArchRule outgoingAdaptersDoNotAccessIncomingAdapters = noClasses()
            .that().resideInAPackage(ROOT + ".adapter.out..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".adapter.in..");

    @ArchTest
    static final ArchRule webAdapterUsesApplicationOnlyThroughIncomingPorts = noClasses()
            .that().resideInAPackage(ROOT + ".adapter.in.web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + ".application.service..", ROOT + ".application.port.out..");

    @ArchTest
    static final ArchRule fhirModelOnlyInWebAdapter = noClasses()
            .that().resideOutsideOfPackage(ROOT + ".adapter.in.web..")
            .should().dependOnClassesThat().resideInAnyPackage("org.hl7.fhir..", "ca.uhn.fhir..");

    @ArchTest
    static final ArchRule jpaOnlyInPersistenceAdapter = noClasses()
            .that().resideOutsideOfPackage(ROOT + ".adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule noPackageCycles = slices()
            .matching(ROOT + ".(**)")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule noHapiServerModules = noClasses()
            .that().resideInAPackage(ROOT + "..")
            .should().dependOnClassesThat().resideInAnyPackage("ca.uhn.fhir.rest.server..", "ca.uhn.fhir.jpa..");

    @ArchTest
    static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
}
