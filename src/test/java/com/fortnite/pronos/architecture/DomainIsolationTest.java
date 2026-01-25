package com.fortnite.pronos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture tests enforcing domain layer isolation. The pure domain package
 * (com.fortnite.pronos.domain) should contain only business logic without infrastructure concerns.
 *
 * <p>Note: The legacy core.domain package predates this architecture and is excluded.
 */
@DisplayName("Domain Isolation Tests")
class DomainIsolationTest {

  private static final String PURE_DOMAIN_PACKAGE = "com.fortnite.pronos.domain..";
  private static JavaClasses importedClasses;

  @BeforeAll
  static void setup() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.fortnite.pronos.domain");
  }

  @Test
  @DisplayName("Domain classes should not depend on JPA")
  void domainShouldNotDependOnJpa() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(PURE_DOMAIN_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "javax.persistence..");

    rule.because("Domain layer should be independent of persistence framework")
        .check(importedClasses);
  }

  @Test
  @DisplayName("Domain classes should not depend on Spring framework")
  void domainShouldNotDependOnSpring() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(PURE_DOMAIN_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..");

    rule.because("Domain layer should be independent of Spring framework").check(importedClasses);
  }

  @Test
  @DisplayName("Domain classes should not depend on Hibernate")
  void domainShouldNotDependOnHibernate() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(PURE_DOMAIN_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.hibernate..");

    rule.because("Domain layer should be independent of Hibernate").check(importedClasses);
  }

  @Test
  @DisplayName("Domain classes should not depend on Lombok")
  void domainShouldNotDependOnLombok() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(PURE_DOMAIN_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("lombok..");

    rule.because("Domain layer should use plain Java or records instead of Lombok")
        .check(importedClasses);
  }

  @Test
  @DisplayName("Domain classes can only depend on allowed packages")
  void domainCanOnlyDependOnAllowedPackages() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage(PURE_DOMAIN_PACKAGE)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                PURE_DOMAIN_PACKAGE,
                "com.fortnite.pronos.model..", // Allowed for enums like GameStatus
                "java..",
                "");

    rule.because("Domain should only depend on domain, model enums, and Java standard library")
        .check(importedClasses);
  }

  @Test
  @DisplayName("Domain classes should be final or records")
  void domainClassesShouldBeFinalOrRecords() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage(PURE_DOMAIN_PACKAGE)
            .and()
            .areNotRecords()
            .and()
            .areNotNestedClasses()
            .and()
            .areNotInterfaces() // Interfaces cannot be final
            .should()
            .haveModifier(JavaModifier.FINAL);

    rule.because("Domain classes should be final to prevent inheritance").check(importedClasses);
  }
}
