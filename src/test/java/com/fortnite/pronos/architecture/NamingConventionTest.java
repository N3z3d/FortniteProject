package com.fortnite.pronos.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * Architecture tests to enforce naming conventions and annotation consistency across different
 * layers of the application.
 */
public class NamingConventionTest {

  private final JavaClasses classes = new ClassFileImporter().importPackages("com.fortnite.pronos");

  @Test
  void controllersShouldHaveControllerSuffix() {
    ArchRule rule =
        ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("..controller..")
            .and()
            .areAnnotatedWith(RestController.class)
            .should()
            .haveSimpleNameEndingWith("Controller");

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
            .and()
            .areAnnotatedWith(Repository.class)
            .or()
            .areInterfaces()
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
            .should()
            .haveSimpleNameEndingWith("Dto")
            .orShould()
            .haveSimpleNameEndingWith("Request")
            .orShould()
            .haveSimpleNameEndingWith("Response");

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
            .or()
            .haveSimpleNameContaining("Test")
            .should()
            .haveSimpleNameEndingWith("Test")
            .orShould()
            .haveSimpleNameEndingWith("Tests");

    rule.check(classes);
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
