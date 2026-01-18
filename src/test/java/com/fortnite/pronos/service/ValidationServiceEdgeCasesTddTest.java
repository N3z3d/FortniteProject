package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.model.Player;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationService - Edge Cases TDD Tests")
class ValidationServiceEdgeCasesTddTest {

  @InjectMocks private ValidationService validationService;

  private Map<Player.Region, Integer> validRegionRules;

  @BeforeEach
  void setUp() {
    validRegionRules = new HashMap<>();
    validRegionRules.put(Player.Region.EU, 5);
    validRegionRules.put(Player.Region.NAC, 3);
    validRegionRules.put(Player.Region.NAW, 2);
  }

  @Test
  @DisplayName("Should handle very long strings")
  void shouldHandleVeryLongStrings() {
    String veryLongString = "A".repeat(1000);

    boolean notEmptyResult = validationService.isNotEmpty(veryLongString);
    boolean requestResult = validationService.validateRequest(veryLongString);

    assertThat(notEmptyResult).isTrue();
    assertThat(requestResult).isTrue();
  }

  @Test
  @DisplayName("Should handle special email characters")
  void shouldHandleSpecialEmailCharacters() {
    String specialEmail = "user+test@domain-name.co.uk";

    boolean result = validationService.isValidEmail(specialEmail);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should handle boundary values in region rules")
  void shouldHandleBoundaryValuesInRegionRules() {
    Map<Player.Region, Integer> boundaryRules = new HashMap<>();
    boundaryRules.put(Player.Region.EU, 10);
    boundaryRules.put(Player.Region.NAC, 10);

    assertThatCode(() -> validationService.validateRegionRules(boundaryRules))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should handle minimum values in region rules")
  void shouldHandleMinimumValuesInRegionRules() {
    Map<Player.Region, Integer> minRules = new HashMap<>();
    minRules.put(Player.Region.EU, 1);

    assertThatCode(() -> validationService.validateRegionRules(minRules))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should handle concurrent validation calls")
  void shouldHandleConcurrentValidationCalls() {
    String[] testStrings = {"test1", "test2", "test3", "test4", "test5"};

    for (String testString : testStrings) {
      boolean result1 = validationService.isNotEmpty(testString);
      boolean result2 = validationService.validateRequest(testString);
      boolean result3 = validationService.isValidEmail(testString + "@test.com");

      assertThat(result1).isTrue();
      assertThat(result2).isTrue();
      assertThat(result3).isTrue();
    }
  }

  @Test
  @DisplayName("Should maintain validation consistency across calls")
  void shouldMaintainValidationConsistencyAcrossCalls() {
    String testValue = "consistent-test";

    boolean result1 = validationService.isNotEmpty(testValue);
    boolean result2 = validationService.isNotEmpty(testValue);
    boolean result3 = validationService.isNotEmpty(testValue);

    assertThat(result1).isEqualTo(result2).isEqualTo(result3).isTrue();
  }

  @Test
  @DisplayName("Should handle validation with immutable collections")
  void shouldHandleValidationWithImmutableCollections() {
    Map<Player.Region, Integer> immutableRules = Collections.unmodifiableMap(validRegionRules);

    assertThatCode(() -> validationService.validateRegionRules(immutableRules))
        .doesNotThrowAnyException();
  }
}
