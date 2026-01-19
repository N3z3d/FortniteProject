package com.fortnite.pronos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture tests enforcing Dependency Inversion Principle (DIP).
 *
 * <p>DIP states: - High-level modules should not depend on low-level modules. Both should depend on
 * abstractions. - Abstractions should not depend on details. Details should depend on abstractions.
 *
 * <p>In this codebase: - Services depend on repository INTERFACES (Spring Data) - not concrete
 * implementations - Domain services (core.domain) have NO infrastructure dependencies - Use cases
 * orchestrate but should not contain persistence logic
 */
@DisplayName("DIP - Dependency Inversion Principle Tests")
class DependencyInversionTest {

  private static JavaClasses importedClasses;

  @BeforeAll
  static void setup() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.fortnite.pronos");
  }

  @Nested
  @DisplayName("Rule 1: Services must depend on abstractions")
  class ServiceAbstractionRules {

    @Test
    @DisplayName("Services should not directly use EntityManager")
    void servicesShouldNotUseEntityManager() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..service..")
              .should()
              .dependOnClassesThat()
              .haveFullyQualifiedName("jakarta.persistence.EntityManager");

      rule.because("Services should use repository interfaces, not EntityManager directly (DIP)")
          .check(importedClasses);
    }

    @Test
    @DisplayName("Services should not use native query builder")
    void servicesShouldNotUseNativeQueryBuilder() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..service..")
              .should()
              .dependOnClassesThat()
              .haveFullyQualifiedName("jakarta.persistence.Query");

      rule.because(
              "Services should not build native queries - use repository methods instead (DIP)")
          .check(importedClasses);
    }

    @Test
    @DisplayName("Services should not use CriteriaBuilder directly")
    void servicesShouldNotUseCriteriaBuilder() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..service..")
              .should()
              .dependOnClassesThat()
              .resideInAnyPackage("jakarta.persistence.criteria..");

      rule.because(
              "Services should not use JPA Criteria API - use repository methods instead (DIP)")
          .check(importedClasses);
    }
  }

  @Nested
  @DisplayName("Rule 2: Domain layer purity")
  class DomainPurityRules {

    @Test
    @DisplayName("Domain services should not depend on repositories")
    void domainServicesShouldNotDependOnRepositories() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..core.domain..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage("..repository..");

      rule.because(
              "Domain services should contain pure business logic with no persistence dependencies")
          .check(importedClasses);
    }

    @Test
    @DisplayName("Domain services should not depend on Spring Data")
    void domainServicesShouldNotDependOnSpringData() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..core.domain..")
              .should()
              .dependOnClassesThat()
              .resideInAnyPackage("org.springframework.data..");

      rule.because("Domain layer must be framework-agnostic (no Spring Data)")
          .check(importedClasses);
    }

    @Test
    @DisplayName("Domain services should not depend on JPA")
    void domainServicesShouldNotDependOnJpa() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..core.domain..")
              .should()
              .dependOnClassesThat()
              .resideInAnyPackage("jakarta.persistence..", "javax.persistence..");

      rule.because("Domain layer must not depend on JPA (persistence is infrastructure)")
          .check(importedClasses);
    }
  }

  @Nested
  @DisplayName("Rule 3: Repository interface contracts")
  class RepositoryContractRules {

    @Test
    @DisplayName("Repositories should be interfaces")
    void repositoriesShouldBeInterfaces() {
      ArchRule rule = classes().that().resideInAPackage("..repository..").should().beInterfaces();

      rule.because("Repositories must be interfaces to allow DIP - services depend on abstractions")
          .check(importedClasses);
    }

    @Test
    @DisplayName("Repositories should extend JpaRepository or Spring Data interfaces")
    void repositoriesShouldExtendSpringDataInterfaces() {
      ArchRule rule =
          classes()
              .that()
              .resideInAPackage("..repository..")
              .and()
              .areInterfaces()
              .and()
              .haveSimpleNameEndingWith("Repository")
              .should()
              .beAssignableTo(org.springframework.data.repository.Repository.class);

      rule.because("Repository interfaces should extend Spring Data Repository hierarchy")
          .check(importedClasses);
    }
  }

  @Nested
  @DisplayName("Rule 4: Layer isolation")
  class LayerIsolationRules {

    @Test
    @DisplayName("Controllers should not directly depend on repositories")
    void controllersShouldNotDependOnRepositories() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..controller..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage("..repository..");

      rule.because("Controllers should depend on services, not repositories (layered architecture)")
          .check(importedClasses);
    }

    @Test
    @DisplayName("DTOs should not depend on repositories")
    void dtosShouldNotDependOnRepositories() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..dto..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage("..repository..");

      rule.because("DTOs are data transfer objects and should not have repository dependencies")
          .check(importedClasses);
    }

    @Test
    @DisplayName("DTOs should not depend on services")
    void dtosShouldNotDependOnServices() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..dto..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage("..service..");

      rule.because("DTOs should not depend on services - they are simple data containers")
          .check(importedClasses);
    }
  }

  @Nested
  @DisplayName("Rule 5: Error handling isolation")
  class ErrorHandlingRules {

    @Test
    @DisplayName("Core exceptions should not depend on Spring")
    void coreExceptionsShouldNotDependOnSpring() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..core.error..")
              .should()
              .dependOnClassesThat()
              .resideInAnyPackage("org.springframework..");

      rule.because("Core exceptions should be framework-agnostic").check(importedClasses);
    }
  }
}
