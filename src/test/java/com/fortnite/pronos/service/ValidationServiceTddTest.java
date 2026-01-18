package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.model.Player;

/**
 * TDD Tests for ValidationService - Business Critical Component
 *
 * <p>This test suite validates input validation, business rules, and data integrity using
 * RED-GREEN-REFACTOR TDD methodology. ValidationService handles request validation, region rules,
 * and game request validation essential for maintaining data quality and preventing invalid
 * operations in the fantasy league system.
 *
 * <p>Business Logic Areas: - Generic request and input validation - String validation - Game
 * creation and join request validation - Region distribution rules and constraints - Business
 * rules enforcement and error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationService - Business Critical TDD Tests")
class ValidationServiceTddTest {

  @InjectMocks private ValidationService validationService;

  private String validString;
  private String emptyString;
  private Map<Player.Region, Integer> validRegionRules;
  private Map<Player.Region, Integer> invalidRegionRules;

  @BeforeEach
  void setUp() {
    // Test data setup
    validString = "ValidString";
    emptyString = "";

    // Valid region rules setup
    validRegionRules = new HashMap<>();
    validRegionRules.put(Player.Region.EU, 5);
    validRegionRules.put(Player.Region.NAC, 3);
    validRegionRules.put(Player.Region.NAW, 2);

    // Invalid region rules setup
    invalidRegionRules = new HashMap<>();
    invalidRegionRules.put(Player.Region.EU, 15); // Too many players
    invalidRegionRules.put(Player.Region.NAC, 10);
  }

  @Nested
  @DisplayName("Generic Request Validation")
  class GenericValidationTests {

    @Test
    @DisplayName("Should validate non-null request successfully")
    void shouldValidateNonNullRequestSuccessfully() {
      // RED: Test basic request validation
      Object validRequest = new Object();

      boolean result = validationService.validateRequest(validRequest);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject null request")
    void shouldRejectNullRequest() {
      // RED: Test null request rejection
      boolean result = validationService.validateRequest(null);

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should validate different request types")
    void shouldValidateDifferentRequestTypes() {
      // RED: Test various request types
      String stringRequest = "test";
      Integer integerRequest = 42;
      Map<String, Object> mapRequest = new HashMap<>();

      boolean stringResult = validationService.validateRequest(stringRequest);
      boolean integerResult = validationService.validateRequest(integerRequest);
      boolean mapResult = validationService.validateRequest(mapRequest);

      assertThat(stringResult).isTrue();
      assertThat(integerResult).isTrue();
      assertThat(mapResult).isTrue();
    }

    @Test
    @DisplayName("Should handle complex object validation")
    void shouldHandleComplexObjectValidation() {
      // RED: Test complex object validation
      List<String> complexRequest = Arrays.asList("item1", "item2", "item3");

      boolean result = validationService.validateRequest(complexRequest);

      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("String and Input Validation")
  class StringValidationTests {

    @Test
    @DisplayName("Should validate non-empty string")
    void shouldValidateNonEmptyString() {
      // RED: Test valid string validation
      boolean result = validationService.isNotEmpty(validString);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject empty string")
    void shouldRejectEmptyString() {
      // RED: Test empty string rejection
      boolean result = validationService.isNotEmpty(emptyString);

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject null string")
    void shouldRejectNullString() {
      // RED: Test null string rejection
      boolean result = validationService.isNotEmpty(null);

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject whitespace-only string")
    void shouldRejectWhitespaceOnlyString() {
      // RED: Test whitespace string rejection
      String whitespaceString = "   ";
      boolean result = validationService.isNotEmpty(whitespaceString);

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should validate string with leading/trailing spaces")
    void shouldValidateStringWithLeadingTrailingSpaces() {
      // RED: Test string with spaces validation
      String stringWithSpaces = "  Valid Content  ";
      boolean result = validationService.isNotEmpty(stringWithSpaces);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should handle special characters in strings")
    void shouldHandleSpecialCharactersInStrings() {
      // RED: Test special characters validation
      String specialString = "Test@#$%^&*()_+";
      boolean result = validationService.isNotEmpty(specialString);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should handle unicode characters")
    void shouldHandleUnicodeCharacters() {
      // RED: Test unicode characters validation
      String unicodeString = "Tëst Üsér Náme";
      boolean result = validationService.isNotEmpty(unicodeString);

      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("Create Game Request Validation")
  class CreateGameValidationTests {

    @Test
    @DisplayName("Should validate valid create game request")
    void shouldValidateValidCreateGameRequest() {
      // RED: Test valid create game request
      CreateGameRequest validRequest = mock(CreateGameRequest.class);
      when(validRequest.getName()).thenReturn("Test Game");
      when(validRequest.isValid()).thenReturn(true);

      assertThatCode(() -> validationService.validateCreateGameRequest(validRequest))
          .doesNotThrowAnyException();

      verify(validRequest).isValid();
      verify(validRequest).getName();
    }

    @Test
    @DisplayName("Should throw exception for null create game request")
    void shouldThrowExceptionForNullCreateGameRequest() {
      // RED: Test null request rejection
      assertThatThrownBy(() -> validationService.validateCreateGameRequest(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("La requête de création de game ne peut pas être null");
    }

    @Test
    @DisplayName("Should throw exception for invalid create game request")
    void shouldThrowExceptionForInvalidCreateGameRequest() {
      // RED: Test invalid request rejection
      CreateGameRequest invalidRequest = mock(CreateGameRequest.class);
      when(invalidRequest.getName()).thenReturn("Invalid Game");
      when(invalidRequest.isValid()).thenReturn(false);
      when(invalidRequest.getValidationErrors())
          .thenReturn(Arrays.asList("Name too short", "Invalid settings"));

      assertThatThrownBy(() -> validationService.validateCreateGameRequest(invalidRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Erreurs de validation")
          .hasMessageContaining("Name too short")
          .hasMessageContaining("Invalid settings");

      verify(invalidRequest).isValid();
      verify(invalidRequest).getValidationErrors();
    }

    @Test
    @DisplayName("Should handle empty validation errors list")
    void shouldHandleEmptyValidationErrorsList() {
      // RED: Test edge case with empty errors
      CreateGameRequest validRequestWithEmptyErrors = mock(CreateGameRequest.class);
      when(validRequestWithEmptyErrors.getName()).thenReturn("Valid Game");
      when(validRequestWithEmptyErrors.isValid()).thenReturn(true);

      assertThatCode(() -> validationService.validateCreateGameRequest(validRequestWithEmptyErrors))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle single validation error")
    void shouldHandleSingleValidationError() {
      // RED: Test single error handling
      CreateGameRequest singleErrorRequest = mock(CreateGameRequest.class);
      when(singleErrorRequest.getName()).thenReturn("Error Game");
      when(singleErrorRequest.isValid()).thenReturn(false);
      when(singleErrorRequest.getValidationErrors()).thenReturn(Arrays.asList("Single error"));

      assertThatThrownBy(() -> validationService.validateCreateGameRequest(singleErrorRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Single error");
    }

    @Test
    @DisplayName("Should handle multiple validation errors")
    void shouldHandleMultipleValidationErrors() {
      // RED: Test multiple errors handling
      CreateGameRequest multiErrorRequest = mock(CreateGameRequest.class);
      when(multiErrorRequest.getName()).thenReturn("Multi Error Game");
      when(multiErrorRequest.isValid()).thenReturn(false);
      when(multiErrorRequest.getValidationErrors())
          .thenReturn(Arrays.asList("Error 1", "Error 2", "Error 3"));

      assertThatThrownBy(() -> validationService.validateCreateGameRequest(multiErrorRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Error 1")
          .hasMessageContaining("Error 2")
          .hasMessageContaining("Error 3");
    }
  }

  @Nested
  @DisplayName("Join Game Request Validation")
  class JoinGameValidationTests {

    @Test
    @DisplayName("Should validate valid join game request")
    void shouldValidateValidJoinGameRequest() {
      // RED: Test valid join game request
      JoinGameRequest validRequest = mock(JoinGameRequest.class);
      when(validRequest.getGameId()).thenReturn(UUID.randomUUID());
      when(validRequest.isValid()).thenReturn(true);

      assertThatCode(() -> validationService.validateJoinGameRequest(validRequest))
          .doesNotThrowAnyException();

      verify(validRequest).isValid();
      verify(validRequest).getGameId();
    }

    @Test
    @DisplayName("Should throw exception for null join game request")
    void shouldThrowExceptionForNullJoinGameRequest() {
      // RED: Test null request rejection
      assertThatThrownBy(() -> validationService.validateJoinGameRequest(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("La requête de rejoindre une game ne peut pas être null");
    }

    @Test
    @DisplayName("Should throw exception for invalid join game request")
    void shouldThrowExceptionForInvalidJoinGameRequest() {
      // RED: Test invalid request rejection
      JoinGameRequest invalidRequest = mock(JoinGameRequest.class);
      when(invalidRequest.getGameId()).thenReturn(UUID.randomUUID());
      when(invalidRequest.isValid()).thenReturn(false);
      when(invalidRequest.getValidationErrors())
          .thenReturn(Arrays.asList("Game not found", "User already in game"));

      assertThatThrownBy(() -> validationService.validateJoinGameRequest(invalidRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Erreurs de validation")
          .hasMessageContaining("Game not found")
          .hasMessageContaining("User already in game");

      verify(invalidRequest).isValid();
      verify(invalidRequest).getValidationErrors();
    }

    @Test
    @DisplayName("Should handle join request with valid game ID")
    void shouldHandleJoinRequestWithValidGameId() {
      // RED: Test game ID validation
      UUID testGameId = UUID.randomUUID();
      JoinGameRequest requestWithId = mock(JoinGameRequest.class);
      when(requestWithId.getGameId()).thenReturn(testGameId);
      when(requestWithId.isValid()).thenReturn(true);

      assertThatCode(() -> validationService.validateJoinGameRequest(requestWithId))
          .doesNotThrowAnyException();

      verify(requestWithId).getGameId();
    }

    @Test
    @DisplayName("Should handle complex join validation errors")
    void shouldHandleComplexJoinValidationErrors() {
      // RED: Test complex error scenarios
      JoinGameRequest complexErrorRequest = mock(JoinGameRequest.class);
      when(complexErrorRequest.getGameId()).thenReturn(UUID.randomUUID());
      when(complexErrorRequest.isValid()).thenReturn(false);
      when(complexErrorRequest.getValidationErrors())
          .thenReturn(
              Arrays.asList(
                  "Game is full",
                  "Game has already started",
                  "User does not meet requirements",
                  "Invalid user season"));

      assertThatThrownBy(() -> validationService.validateJoinGameRequest(complexErrorRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Game is full")
          .hasMessageContaining("Game has already started")
          .hasMessageContaining("User does not meet requirements")
          .hasMessageContaining("Invalid user season");
    }
  }

  @Nested
  @DisplayName("Region Rules Validation")
  class RegionRulesValidationTests {

    @Test
    @DisplayName("Should validate correct region rules")
    void shouldValidateCorrectRegionRules() {
      // RED: Test valid region rules
      assertThatCode(() -> validationService.validateRegionRules(validRegionRules))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle null region rules")
    void shouldHandleNullRegionRules() {
      // RED: Test null region rules handling
      assertThatCode(() -> validationService.validateRegionRules(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle empty region rules")
    void shouldHandleEmptyRegionRules() {
      // RED: Test empty region rules handling
      Map<Player.Region, Integer> emptyRules = new HashMap<>();

      assertThatCode(() -> validationService.validateRegionRules(emptyRules))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception for null region")
    void shouldThrowExceptionForNullRegion() {
      // RED: Test null region rejection
      Map<Player.Region, Integer> nullRegionRules = new HashMap<>();
      nullRegionRules.put(null, 5);

      assertThatThrownBy(() -> validationService.validateRegionRules(nullRegionRules))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("La région ne peut pas être null");
    }

    @Test
    @DisplayName("Should throw exception for null player count")
    void shouldThrowExceptionForNullPlayerCount() {
      // RED: Test null player count rejection
      Map<Player.Region, Integer> nullCountRules = new HashMap<>();
      nullCountRules.put(Player.Region.EU, null);

      assertThatThrownBy(() -> validationService.validateRegionRules(nullCountRules))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Le nombre de joueurs par région doit être positif");
    }

    @Test
    @DisplayName("Should throw exception for zero player count")
    void shouldThrowExceptionForZeroPlayerCount() {
      // RED: Test zero player count rejection
      Map<Player.Region, Integer> zeroCountRules = new HashMap<>();
      zeroCountRules.put(Player.Region.NAC, 0);

      assertThatThrownBy(() -> validationService.validateRegionRules(zeroCountRules))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Le nombre de joueurs par région doit être positif");
    }

    @Test
    @DisplayName("Should throw exception for negative player count")
    void shouldThrowExceptionForNegativePlayerCount() {
      // RED: Test negative player count rejection
      Map<Player.Region, Integer> negativeCountRules = new HashMap<>();
      negativeCountRules.put(Player.Region.NAW, -5);

      assertThatThrownBy(() -> validationService.validateRegionRules(negativeCountRules))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Le nombre de joueurs par région doit être positif");
    }

    @Test
    @DisplayName("Should throw exception for too many players per region")
    void shouldThrowExceptionForTooManyPlayersPerRegion() {
      // RED: Test region limit validation
      Map<Player.Region, Integer> tooManyPerRegion = new HashMap<>();
      tooManyPerRegion.put(Player.Region.BR, 15);

      assertThatThrownBy(() -> validationService.validateRegionRules(tooManyPerRegion))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Le nombre de joueurs par région ne peut pas dépasser 10");
    }

    @Test
    @DisplayName("Should throw exception for too many total players")
    void shouldThrowExceptionForTooManyTotalPlayers() {
      // RED: Test total players limit
      assertThatThrownBy(() -> validationService.validateRegionRules(invalidRegionRules))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Le nombre total de joueurs ne peut pas dépasser 20");
    }

    @Test
    @DisplayName("Should validate maximum allowed players per region")
    void shouldValidateMaximumAllowedPlayersPerRegion() {
      // RED: Test boundary conditions
      Map<Player.Region, Integer> maxPerRegion = new HashMap<>();
      maxPerRegion.put(Player.Region.EU, 10);
      maxPerRegion.put(Player.Region.NAC, 10);

      assertThatCode(() -> validationService.validateRegionRules(maxPerRegion))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should validate maximum total players")
    void shouldValidateMaximumTotalPlayers() {
      // RED: Test total limit boundary
      Map<Player.Region, Integer> maxTotalPlayers = new HashMap<>();
      maxTotalPlayers.put(Player.Region.EU, 5);
      maxTotalPlayers.put(Player.Region.NAC, 5);
      maxTotalPlayers.put(Player.Region.NAW, 5);
      maxTotalPlayers.put(Player.Region.BR, 5);

      assertThatCode(() -> validationService.validateRegionRules(maxTotalPlayers))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle all available regions")
    void shouldHandleAllAvailableRegions() {
      // RED: Test all regions validation
      Map<Player.Region, Integer> allRegions = new HashMap<>();
      allRegions.put(Player.Region.EU, 3);
      allRegions.put(Player.Region.NAC, 3);
      allRegions.put(Player.Region.NAW, 3);
      allRegions.put(Player.Region.BR, 3);
      allRegions.put(Player.Region.ASIA, 3);
      allRegions.put(Player.Region.OCE, 3);
      allRegions.put(Player.Region.ME, 2);

      assertThatCode(() -> validationService.validateRegionRules(allRegions))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle single region configuration")
    void shouldHandleSingleRegionConfiguration() {
      // RED: Test single region setup
      Map<Player.Region, Integer> singleRegion = new HashMap<>();
      singleRegion.put(Player.Region.EU, 8);

      assertThatCode(() -> validationService.validateRegionRules(singleRegion))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Performance and Integration")
  class PerformanceIntegrationTests {

    @Test
    @DisplayName("Should validate large region configurations efficiently")
    void shouldValidateLargeRegionConfigurationsEfficiently() {
      // RED: Test performance with complex configurations
      Map<Player.Region, Integer> complexRules = new HashMap<>();
      for (Player.Region region : Player.Region.values()) {
        complexRules.put(region, 2); // Total will be <= 20
      }

      assertThatCode(() -> validationService.validateRegionRules(complexRules))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle bulk email validation")
    void shouldHandleBulkEmailValidation() {
      // RED: Test bulk operations
      String[] emailsToValidate = {
        "user1@test.com",
        "user2@example.org",
        "user3@domain.co.uk",
        "invalid-email",
        "user4@test.net"
      };

      int validCount = 0;
      for (String email : emailsToValidate) {
        if (validationService.isValidEmail(email)) {
          validCount++;
        }
      }

      assertThat(validCount).isEqualTo(4); // 4 valid, 1 invalid
    }

    @Test
    @DisplayName("Should integrate validation methods correctly")
    void shouldIntegrateValidationMethodsCorrectly() {
      // RED: Test integration between validation methods
      String testData = "test@example.com";

      // All related validations should be consistent
      boolean requestValid = validationService.validateRequest(testData);
      boolean stringValid = validationService.isNotEmpty(testData);
      boolean emailValid = validationService.isValidEmail(testData);

      assertThat(requestValid).isTrue();
      assertThat(stringValid).isTrue();
      assertThat(emailValid).isTrue();
    }

    @Test
    @DisplayName("Should maintain validation state independence")
    void shouldMaintainValidationStateIndependence() {
      // RED: Test state independence
      String validInput = "valid@test.com";
      String invalidInput = "invalid";

      // Validating invalid input should not affect valid input validation
      boolean invalid1 = validationService.isValidEmail(invalidInput);
      boolean valid1 = validationService.isValidEmail(validInput);
      boolean invalid2 = validationService.isValidEmail(invalidInput);
      boolean valid2 = validationService.isValidEmail(validInput);

      assertThat(invalid1).isFalse();
      assertThat(invalid2).isFalse();
      assertThat(valid1).isTrue();
      assertThat(valid2).isTrue();
    }
  }
}
