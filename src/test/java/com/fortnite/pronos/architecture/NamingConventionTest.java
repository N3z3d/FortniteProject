package com.fortnite.pronos.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * Architecture tests to enforce naming conventions and annotation consistency across different
 * layers of the application.
 */
public class NamingConventionTest {

  private final JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.fortnite.pronos");

  @Test
  void controllersShouldHaveControllerSuffix() {
    ArchRule rule =
        ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("..controller..")
            .and()
            .areAnnotatedWith(RestController.class)
            .should()
            .haveSimpleNameContaining("Controller");

    rule.check(classes);
  }

  @Test
  void servicesShouldHaveServiceSuffix() {
    ArchRule rule =
        ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("..service..")
            .and()
            .areAnnotatedWith(Service.class)
            .should()
            .haveSimpleNameEndingWith("Service");

    rule.check(classes);
  }

  @Test
  void repositoriesShouldHaveRepositorySuffix() {
    ArchRule rule =
        ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("..repository..")
            .should()
            .haveSimpleNameEndingWith("Repository");

    rule.check(classes);
  }

  @Test
  void dtosShouldHaveProperSuffix() {
    ArchRule rule =
        ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("..dto..")
            .and()
            .areTopLevelClasses()
            .should()
            .haveSimpleNameEndingWith("Dto")
            .orShould()
            .haveSimpleNameEndingWith("Request")
            .orShould()
            .haveSimpleNameEndingWith("Response")
            .orShould()
            .haveSimpleNameEndingWith("DTO")
            .orShould()
            .haveSimpleNameContaining("Status")
            .orShould()
            .haveSimpleNameContaining("PlayerStats");

    rule.check(classes);
  }

  @Test
  void exceptionsShouldHaveExceptionSuffix() {
    ArchRule rule =
        ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("..exception..")
            .should()
            .haveSimpleNameEndingWith("Exception");

    rule.check(classes);
  }

  @Test
  void configurationClassesShouldHaveConfigSuffix() {
    ArchRule rule =
        ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("..config..")
            .and()
            .areAnnotatedWith("org.springframework.context.annotation.Configuration")
            .should()
            .haveSimpleNameEndingWith("Config")
            .orShould()
            .haveSimpleNameEndingWith("Configuration");

    rule.check(classes);
  }

  @Test
  void testClassesShouldHaveTestSuffix() {
    ArchRule rule =
        ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("..test..")
            .should()
            .haveSimpleNameEndingWith("Test")
            .orShould()
            .haveSimpleNameEndingWith("Tests");

    rule.allowEmptyShould(true).check(classes);
  }

  @Test
  void constantsShouldBeUpperCase() {
    ArchRule rule =
        ArchRuleDefinition.fields()
            .that()
            .areStatic()
            .and()
            .areFinal()
            .and()
            .arePublic()
            .should()
            .haveNameMatching("^[A-Z][A-Z0-9_]*$");

    rule.check(classes);
  }
}
