package com.example.demo.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture validation tests using ArchUnit.
 * These tests enforce architectural rules and package dependencies.
 */
class ArchitectureTest {

    private JavaClasses classes;

    @BeforeEach
    void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.example.demo");
    }

    @Test
    void shouldFollowLayeredArchitecture() {
        layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controller").definedBy("..controller..")
                .layer("Service").definedBy("..service..")
                .layer("Converter").definedBy("..converter..")
                .layer("Listener").definedBy("..listener..")
                .layer("DTO").definedBy("..dto..")
                .layer("Parser").definedBy("..parser..")
                .layer("Config").definedBy("..config..")

                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Listener", "Config")
                .whereLayer("Converter").mayOnlyBeAccessedByLayers("Listener")
                .whereLayer("Parser").mayOnlyBeAccessedByLayers("Converter")
                .whereLayer("DTO").mayOnlyBeAccessedByLayers("Service", "Converter", "Parser", "Listener", "Controller")

                .check(classes);
    }

    @Test
    void controllersShouldBeInControllerPackage() {
        ArchRule rule = classes()
                .that().haveNameMatching(".*Controller")
                .should().resideInAPackage("..controller..")
                .as("Controllers should reside in controller package");

        rule.check(classes);
    }

    @Test
    void servicesShouldBeInServicePackage() {
        ArchRule rule = classes()
                .that().haveNameMatching(".*Service")
                .should().resideInAPackage("..service..")
                .as("Services should reside in service package");

        rule.check(classes);
    }

    @Test
    void convertersShouldBeInConverterPackage() {
        ArchRule rule = classes()
                .that().haveNameMatching(".*Converter")
                .should().resideInAPackage("..converter..")
                .as("Converters should reside in converter package");

        rule.check(classes);
    }

    @Test
    void listenersShouldBeInListenerPackage() {
        ArchRule rule = classes()
                .that().haveNameMatching(".*Listener")
                .should().resideInAPackage("..listener..")
                .as("Listeners should reside in listener package");

        rule.check(classes);
    }

    @Test
    void dtosShouldBeInDtoPackage() {
        ArchRule rule = classes()
                .that().haveNameMatching(".*Message")
                .or().haveSimpleNameEndingWith("DTO")
                .or().haveSimpleNameEndingWith("Dto")
                .should().resideInAPackage("..dto..")
                .as("DTOs should reside in dto package");

        rule.check(classes);
    }

    @Test
    void servicesShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..")
                .as("Services should not depend on controllers");

        rule.check(classes);
    }

    @Test
    void servicesShouldNotDependOnListeners() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..listener..")
                .as("Services should not depend on listeners");

        rule.check(classes);
    }

    @Test
    void shouldHaveNoFieldInjection() {
        ArchRule rule = com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
                .as("Field injection should not be used - use constructor injection instead");

        rule.check(classes);
    }

    @Test
    void serviceClassesShouldHaveSpringServiceAnnotation() {
        ArchRule rule = classes()
                .that().resideInAPackage("..service..")
                .and().haveNameMatching(".*Service")
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
                .as("Service classes should be annotated with @Service");

        rule.check(classes);
    }

    @Test
    void controllerClassesShouldHaveSpringControllerAnnotation() {
        ArchRule rule = classes()
                .that().resideInAPackage("..controller..")
                .and().haveNameMatching(".*Controller")
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .as("Controller classes should be annotated with @RestController");

        rule.check(classes);
    }

    @Test
    void convertersShouldImplementConverter() {
        ArchRule rule = classes()
                .that().resideInAPackage("..converter..")
                .and().haveNameMatching(".*Converter")
                .should().beAnnotatedWith(org.springframework.stereotype.Component.class)
                .as("Converter classes should be annotated with @Component");

        rule.check(classes);
    }

    @Test
    void listenersShouldHaveComponentAnnotation() {
        ArchRule rule = classes()
                .that().resideInAPackage("..listener..")
                .and().haveNameMatching(".*Listener")
                .should().beAnnotatedWith(org.springframework.stereotype.Component.class)
                .as("Listener classes should be annotated with @Component");

        rule.check(classes);
    }
}
