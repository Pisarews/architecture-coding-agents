package org.archagents.fhir;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;

@AnalyzeClasses(packages = "org.archagents.fhir", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule no_hapi_server_modules = noClasses()
            .that().resideInAPackage("org.archagents.fhir..")
            .should().dependOnClassesThat().resideInAnyPackage("ca.uhn.fhir.rest.server..", "ca.uhn.fhir.jpa..");

    @ArchTest
    static final ArchRule no_field_injection = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
}
