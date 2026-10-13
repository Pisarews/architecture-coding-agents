package org.archagents.fhir;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "org.archagents.fhir", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layersAreRespected = layeredArchitecture()
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
            .whereLayer("Entity").mayNotAccessAnyLayer();

    @ArchTest
    static final ArchRule fhirModelOnlyInWeb = noClasses()
            .that().resideOutsideOfPackage("org.archagents.fhir.web..")
            .should().dependOnClassesThat().resideInAnyPackage("org.hl7.fhir..", "ca.uhn.fhir..");

    @ArchTest
    static final ArchRule noPackageCycles = slices()
            .matching("org.archagents.fhir.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule noHapiServerModules = noClasses()
            .that().resideInAPackage("org.archagents.fhir..")
            .should().dependOnClassesThat().resideInAnyPackage("ca.uhn.fhir.rest.server..", "ca.uhn.fhir.jpa..");

    @ArchTest
    static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
}
