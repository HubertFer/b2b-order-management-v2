package com.humberto.b2b.ordermanagement;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.humberto.b2b.ordermanagement", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] USE_CASE_PACKAGES = {"..application..", "..notification.outbox..", "..notification.inbox.."};

    @ArchTest
    static final ArchRule layersRespectDependencyDirection = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Api").definedBy("..api..")
            .layer("Application").definedBy(USE_CASE_PACKAGES)
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Api").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Api", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Api", "Application", "Infrastructure");

    @ArchTest
    static final ArchRule domainDependsOnlyOnJavaAndDomain = classes()
            .that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "..domain..");

    @ArchTest
    static final ArchRule useCasesAreNotSpringComponents = noClasses()
            .that().resideInAnyPackage(USE_CASE_PACKAGES)
            .or().resideInAPackage("..domain..")
            .should().beMetaAnnotatedWith(Component.class);

    @ArchTest
    static final ArchRule useCasesDoNotDependOnPersistenceOrWeb = noClasses()
            .that().resideInAnyPackage(USE_CASE_PACKAGES)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.springframework.data..",
                    "org.springframework.jdbc..",
                    "org.springframework.kafka..",
                    "org.apache.kafka..",
                    "org.springframework.web..",
                    "org.springframework.http.."
            );

    @ArchTest
    static final ArchRule orderModuleDoesNotKnowPartnerModule = noClasses()
            .that().resideInAPackage("com.humberto.b2b.ordermanagement.order..")
            .should().dependOnClassesThat().resideInAPackage("com.humberto.b2b.ordermanagement.partner..");
}
