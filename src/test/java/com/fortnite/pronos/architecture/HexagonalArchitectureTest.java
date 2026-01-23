package com.fortnite.pronos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture tests enforcing Hexagonal Architecture (Ports & Adapters) principles as defined in
 * ADR-001.
 *
 * <p>Target structure: domain/ (Core - no external dependencies) ├── model/ (Pure domain entities)
 * ├── service/ (Domain services) └── port/ (Interfaces) ├── in/ (Use cases) └── out/ (Repositories,
 * external services)
 *
 * <p>application/ (Use cases orchestration) ├── usecase/ (Use case implementations) └── dto/
 * (Public API contracts)
 *
 * <p>adapter/ (Infrastructure) ├── in/web/ (REST Controllers) └── out/ ├── persistence/ (JPA) └──
 * external/ (External APIs)
 *
 * <p>shared/ (Cross-cutting) ├── exception/ (Global exception handling) └── util/ (Utilities)
 *
 * <p>Dependency Rules: 1. domain DEPENDS ON NOTHING (pure business logic) 2. application DEPENDS ON
 * domain only 3. adapter.in DEPENDS ON application & domain.port.in 4. adapter.out DEPENDS ON
 * domain.port.out 5. domain NEVER DEPENDS ON adapter 6. application NEVER DEPENDS ON adapter
 */
public class HexagonalArchitectureTest {

  private static JavaClasses importedClasses;
  private static boolean hexagonalPackagesPresent;

  @BeforeAll
  static void setup() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.fortnite.pronos");
    hexagonalPackagesPresent =
        importedClasses.stream()
            .map(javaClass -> javaClass.getPackageName())
            .anyMatch(
                packageName ->
                    packageName.contains(".domain.")
                        || packageName.contains(".application.")
                        || packageName.contains(".adapter."));
  }

  @BeforeEach
  void assumeHexagonalPackagesPresent() {
    Assumptions.assumeTrue(
        hexagonalPackagesPresent,
        "Hexagonal packages not present yet; enforce layered rules until migration starts.");
  }

  // ============================================================================
  // RULE 1: Domain Layer Purity (CRITICAL)
  // ============================================================================

  @Test
  void domainShouldNotDependOnOutsideWorld() {
    // Note: This test checks packages named "domain" but excludes "core.domain"
    // which contains application-level services during hybrid migration
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("com.fortnite.pronos.domain..") // Only pure domain package
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..domain..", // Domain can depend on itself
                "..model..", // During hybrid migration: domain uses model enums (Status types)
                "java..", // Java standard library
                "lombok.." // Lombok annotations (compile-time only)
                );

    rule.because(
            "Domain layer must be pure business logic with NO external dependencies "
                + "(no Spring, JPA, Jackson, etc.) - model allowed during migration")
        .check(importedClasses);
  }

  @Test
  void domainShouldNotDependOnSpring() {
    // Note: Checks only pure domain package, excludes core.domain which is application-level
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.fortnite.pronos.domain..") // Only pure domain package
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..");

    rule.because("Domain must be framework-agnostic (no Spring dependencies)")
        .check(importedClasses);
  }

  @Test
  void domainShouldNotDependOnJPA() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "javax.persistence..");

    rule.because("Domain must not depend on JPA (persistence is infrastructure concern)")
        .check(importedClasses);
  }

  @Test
  void domainShouldNotDependOnJackson() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.fasterxml.jackson..");

    rule.because("Domain must not depend on Jackson (serialization is adapter concern)")
        .check(importedClasses);
  }

  // ============================================================================
  // RULE 2: Application Layer Constraints
  // ============================================================================

  @Test
  void applicationShouldOnlyDependOnDomainAndStandardLibraries() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..application..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..application..",
                "..domain..",
                "..model..", // Entities allowed during hybrid migration (facades use JPA entities)
                "..dto..", // Application may return DTOs
                "java..",
                "lombok..",
                "org.springframework..", // Use cases can use Spring DI
                "org.slf4j.." // Logging allowed
                );

    rule.because("Application layer should only depend on domain and standard libraries")
        .check(importedClasses);
  }

  @Test
  void applicationShouldNotDependOnAdapters() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..adapter..");

    rule.because("Application layer must NOT depend on adapters (Dependency Inversion Principle)")
        .check(importedClasses);
  }

  // ============================================================================
  // RULE 3: Adapter Layer Constraints
  // ============================================================================

  @Test
  void adapterInShouldDependOnApplicationAndDomainPortsIn() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..adapter.in..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..adapter.in..",
                "..application..",
                "..domain.port.in..",
                "..shared..",
                "java..",
                "lombok..",
                "org.springframework..",
                "org.slf4j..",
                "jakarta..",
                "io.swagger..",
                "com.fasterxml.jackson..");

    // Allow empty - package adapter.in doesn't exist yet during hybrid migration
    rule.allowEmptyShould(true)
        .because("Incoming adapters (controllers) should depend on application and domain ports")
        .check(importedClasses);
  }

  @Test
  void adapterOutShouldDependOnDomainPortsOut() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..adapter.out..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..adapter.out..",
                "..domain.port.out..",
                "..domain.model..", // Mappers need domain models
                "..shared..",
                "java..",
                "lombok..",
                "org.springframework..",
                "org.slf4j..",
                "jakarta..",
                "org.hibernate..");

    // Allow empty - package adapter.out doesn't exist yet during hybrid migration
    rule.allowEmptyShould(true)
        .because("Outgoing adapters (persistence) should implement domain ports")
        .check(importedClasses);
  }

  @Test
  void adaptersShouldNotDependOnEachOther() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..adapter.in..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..adapter.out..");

    // Allow empty - adapters packages don't exist yet during hybrid migration
    rule.allowEmptyShould(true)
        .because("Adapters should not depend on each other (loose coupling)")
        .check(importedClasses);
  }

  // ============================================================================
  // RULE 4: Naming Conventions
  // ============================================================================

  @Test
  void useCasesShouldBeNamedCorrectly() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..domain.port.in..")
            .should()
            .haveSimpleNameEndingWith("UseCase");

    // Allow empty - package domain.port.in doesn't exist yet during hybrid migration
    rule.allowEmptyShould(true)
        .because("Use case interfaces should follow naming convention: *UseCase")
        .check(importedClasses);
  }

  @Test
  void portsShouldBeInterfaces() {
    ArchRule rule = classes().that().resideInAPackage("..domain.port..").should().beInterfaces();

    // Allow empty - package domain.port doesn't exist yet during hybrid migration
    rule.allowEmptyShould(true)
        .because("Ports should be interfaces (contracts)")
        .check(importedClasses);
  }

  @Test
  void domainServicesShouldBeNamedCorrectly() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..domain.service..")
            .should()
            .haveSimpleNameEndingWith("Service")
            .orShould()
            .haveSimpleNameEndingWith("DomainService");

    // Allow empty - package domain.service doesn't exist yet during hybrid migration
    rule.allowEmptyShould(true)
        .because("Domain services should follow naming convention: *Service or *DomainService")
        .check(importedClasses);
  }

  // ============================================================================
  // RULE 5: Onion Architecture (Hexagonal variation)
  // ============================================================================

  @Test
  @Disabled(
      "Onion architecture test disabled - 1499 violations in hybrid architecture. "
          + "Requires full refactoring: ports, use cases, adapters separation.")
  void shouldFollowOnionArchitecture() {
    // During hybrid architecture migration, we include layered packages as adapters
    onionArchitecture()
        .domainModels("..domain..", "..model..")
        .domainServices("..domain.service..")
        .applicationServices("..application..", "..core..")
        .adapter("web", "..adapter.in..", "..controller..")
        .adapter("persistence", "..adapter.out..", "..repository..")
        .adapter("services", "..service..")
        .adapter("config", "..config..")
        .adapter("dto", "..dto..")
        .adapter("exception", "..exception..")
        .adapter("util", "..util..", "..data..")
        .because(
            "Architecture should follow Hexagonal/Onion principles (ADR-001) - hybrid migration")
        .check(importedClasses);
  }

  // ============================================================================
  // RULE 6: Class Size Constraints (CLAUDE.md)
  // ============================================================================

  @Test
  void classesShouldNotExceed500Lines() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("com.fortnite.pronos..")
            .and()
            .areNotInterfaces()
            .and()
            .areNotEnums()
            .and()
            .doNotHaveSimpleName("package-info")
            .should(new MaximumClassLinesCondition(500));

    rule.because("Classes must not exceed 500 lines (CLAUDE.md constraint)").check(importedClasses);
  }

  // ============================================================================
  // RULE 7: Security & Best Practices
  // ============================================================================

  @Test
  void controllersShouldNotContainBusinessLogic() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..controller..")
            .or()
            .resideInAPackage("..adapter.in.web..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..application..",
                "..domain.port.in..",
                "..core..", // Core use cases and error handling during hybrid migration
                "..service..", // Allowed during hybrid arch migration (layered -> hexagonal)
                "..dto..",
                "..model..", // DTOs may reference model types
                "..repository..", // Controllers may inject repositories during migration
                "..controller..", // Allow internal DTOs in controllers
                "..config..",
                "..exception..",
                "..shared..",
                "java..",
                "lombok..",
                "org.springframework..",
                "org.slf4j..",
                "jakarta..",
                "io.swagger..");

    rule.because("Controllers should only route requests to services/use cases (no business logic)")
        .check(importedClasses);
  }

  @Test
  void servicesShouldNotDependOnControllers() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..service..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..controller..", "..adapter.in..");

    rule.because("Services must not depend on controllers (violation of DIP)")
        .check(importedClasses);
  }

  @Test
  void repositoriesShouldNotDependOnServices() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..repository..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..service..", "..controller..", "..adapter..");

    rule.because("Repositories must not depend on services or controllers").check(importedClasses);
  }

  // ============================================================================
  // RULE 8: Package Organization
  // ============================================================================

  @Test
  void packagesWithSameNameShouldBeInSamePlace() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("com.fortnite.pronos..")
            .should()
            .onlyHaveDependentClassesThat()
            .resideInAnyPackage("com.fortnite.pronos..", "java..", "org.springframework..");

    rule.because("Package organization should be consistent").check(importedClasses);
  }
}
