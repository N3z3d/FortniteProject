package com.fortnite.pronos.architecture;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.Architectures;

/**
 * Architecture tests to enforce layered architecture principles and ensure clean separation of
 * concerns across the application.
 */
public class LayeredArchitectureTest {

  private final JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.fortnite.pronos");

  @Test
  void shouldFollowLayeredArchitecture() {
    // Hybrid layered architecture during migration to hexagonal
    Architectures.layeredArchitecture()
        .consideringAllDependencies()
        .layer("Controllers")
        .definedBy("..controller..")
        .optionalLayer("UseCases")
        .definedBy("..core..")
        .optionalLayer("Exceptions")
        .definedBy("..exception..")
        .optionalLayer("Domain")
        .definedBy("..domain..")
        .optionalLayer("Application")
        .definedBy("..application..")
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
        .whereLayer("UseCases")
        .mayOnlyBeAccessedByLayers("Controllers", "Services", "UseCases", "Config")
        .whereLayer("Exceptions")
        .mayOnlyBeAccessedByLayers("Controllers", "Services", "UseCases", "Config", "Exceptions")
        .whereLayer("Domain")
        .mayOnlyBeAccessedByLayers("Application", "Services", "UseCases", "Domain", "Config")
        .whereLayer("Application")
        .mayOnlyBeAccessedByLayers("Controllers", "Services", "UseCases", "Application", "Config")
        .whereLayer("Services")
        .mayOnlyBeAccessedByLayers(
            "Controllers", "Services", "UseCases", "Config", "DTOs", "Exceptions")
        .whereLayer("Repositories")
        .mayOnlyBeAccessedByLayers("Controllers", "Services", "UseCases", "Config")
        .whereLayer("Models")
        .mayOnlyBeAccessedByLayers(
            "Controllers",
            "Services",
            "Repositories",
            "DTOs",
            "UseCases",
            "Exceptions",
            "Config",
            "Domain",
            "Application")
        .whereLayer("DTOs")
        .mayOnlyBeAccessedByLayers(
            "Controllers", "Services", "UseCases", "Exceptions", "Config", "Application")
        .whereLayer("Config")
        .mayOnlyBeAccessedByLayers("Config", "Controllers", "Services", "UseCases", "Exceptions")
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
                "..controller..", // autoriser les DTO internes des contrôleurs
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
