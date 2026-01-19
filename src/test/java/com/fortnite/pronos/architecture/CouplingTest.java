package com.fortnite.pronos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Architecture tests enforcing coupling limits.
 *
 * <p>CLAUDE.md requires no class to have more than 7 injected dependencies. This test enforces that
 * rule by checking private final fields (typical pattern for constructor injection).
 */
@DisplayName("Coupling Tests - Dependency Limits")
class CouplingTest {

  private static final int MAX_DEPENDENCIES = 7;
  private static JavaClasses importedClasses;

  @BeforeAll
  static void setup() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.fortnite.pronos");
  }

  @Test
  @DisplayName("Services should not have more than 7 injected dependencies")
  void servicesShouldNotHaveMoreThanSevenDependencies() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..service..")
            .and()
            .areAnnotatedWith(org.springframework.stereotype.Service.class)
            .should(haveAtMostNInjectedDependencies(MAX_DEPENDENCIES));

    rule.because(
            "Classes with more than 7 dependencies indicate low cohesion "
                + "and should be split (CLAUDE.md requirement)")
        .check(importedClasses);
  }

  @Test
  @DisplayName("Controllers should not have more than 7 injected dependencies")
  void controllersShouldNotHaveMoreThanSevenDependencies() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..controller..")
            .should(haveAtMostNInjectedDependencies(MAX_DEPENDENCIES));

    rule.because("Controllers with too many dependencies should delegate to facade services")
        .check(importedClasses);
  }

  @Test
  @DisplayName("DataInitializationService should have 6 or fewer dependencies after refactoring")
  void dataInitializationServiceShouldHaveReducedDependencies() {
    // Direct assertion to verify the refactoring was successful
    try {
      Class<?> clazz = Class.forName("com.fortnite.pronos.service.DataInitializationService");
      long dependencyCount = countInjectedDependencies(clazz);

      assertTrue(
          dependencyCount <= 6,
          String.format(
              "DataInitializationService should have at most 6 dependencies after refactoring, "
                  + "but has %d",
              dependencyCount));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("DataInitializationService not found", e);
    }
  }

  private static ArchCondition<JavaClass> haveAtMostNInjectedDependencies(int maxDependencies) {
    return new ArchCondition<>("have at most " + maxDependencies + " injected dependencies") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        try {
          Class<?> clazz = Class.forName(javaClass.getName());
          long dependencyCount = countInjectedDependencies(clazz);

          if (dependencyCount > maxDependencies) {
            String message =
                String.format(
                    "%s has %d dependencies (max allowed: %d)",
                    javaClass.getSimpleName(), dependencyCount, maxDependencies);
            events.add(SimpleConditionEvent.violated(javaClass, message));
          }
        } catch (ClassNotFoundException e) {
          // Skip classes that can't be loaded
        }
      }
    };
  }

  private static long countInjectedDependencies(Class<?> clazz) {
    return Arrays.stream(clazz.getDeclaredFields())
        .filter(CouplingTest::isInjectedDependency)
        .count();
  }

  private static boolean isInjectedDependency(Field field) {
    int modifiers = field.getModifiers();
    // Private final non-static fields are typically injected via constructor
    return Modifier.isPrivate(modifiers)
        && Modifier.isFinal(modifiers)
        && !Modifier.isStatic(modifiers)
        && !field.getType().isPrimitive()
        && !field.getType().equals(String.class)
        && !field.getName().startsWith("log")
        && !field.getName().equals("serialVersionUID");
  }
}
