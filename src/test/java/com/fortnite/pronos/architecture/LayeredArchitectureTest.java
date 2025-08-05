package com.fortnite.pronos.architecture;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.Architectures;

/**
 * Architecture tests to enforce layered architecture principles and ensure clean separation of
 * concerns across the application.
 */
public class LayeredArchitectureTest {

  private final JavaClasses classes = new ClassFileImporter().importPackages("com.fortnite.pronos");

  @Test
  void shouldFollowLayeredArchitecture() {
    Architectures.layeredArchitecture()
        .consideringAllDependencies()
        .layer("Controllers")
        .definedBy("..controller..")
        .layer("Services")
        .definedBy("..service..")
        .layer("Repositories")
        .definedBy("..repository..")
        .layer("Models")
        .definedBy("..model..")
        .layer("DTOs")
        .definedBy("..dto..")
        .layer("Config")
        .definedBy("..config..")
        .whereLayer("Controllers")
        .mayNotBeAccessedByAnyLayer()
        .whereLayer("Services")
        .mayOnlyBeAccessedByLayers("Controllers", "Services")
        .whereLayer("Repositories")
        .mayOnlyBeAccessedByLayers("Services", "Config")
        .whereLayer("Models")
        .mayOnlyBeAccessedByLayers("Services", "Repositories", "DTOs")
        .whereLayer("DTOs")
        .mayOnlyBeAccessedByLayers("Controllers", "Services")
        .whereLayer("Config")
        .mayNotAccessAnyLayer()
        .check(classes);
  }

  @Test
  void controllersShouldOnlyDependOnServices() {
    com.tngtech.archunit.lang.ArchRule rule =
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..service..",
                "..dto..",
                "..model..",
                "..repository..",
                "..core..",
                "java..",
                "org.springframework..",
                "org.slf4j..",
                "jakarta..",
                "lombok..",
                "io.swagger..",
                "org.junit..",
                "org.mockito..",
                "com.tngtech.archunit..",
                "com.fortnite.pronos.config..",
                "com.fortnite.pronos.exception..");

    rule.check(classes);
  }

  @Test
  void servicesShouldNotDependOnControllers() {
    com.tngtech.archunit.lang.ArchRule rule =
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("..service..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..controller..");

    rule.check(classes);
  }

  @Test
  void repositoriesShouldNotDependOnServices() {
    com.tngtech.archunit.lang.ArchRule rule =
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("..repository..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..service..", "..controller..");

    rule.check(classes);
  }

  @Test
  void modelsShouldNotDependOnOtherLayers() {
    com.tngtech.archunit.lang.ArchRule rule =
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("..model..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..model..",
                "java..",
                "jakarta..",
                "lombok..",
                "org.hibernate..",
                "com.fasterxml.jackson..");

    rule.check(classes);
  }
}
