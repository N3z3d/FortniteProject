package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Year;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fortnite.pronos.service.SeasonService.SeasonInfo;

/**
 * TDD Tests for SeasonService - Business Critical Component
 *
 * <p>This test suite validates season management, validation, and business rules using
 * RED-GREEN-REFACTOR TDD methodology. SeasonService handles season logic, validation, navigation,
 * and information essential for the fantasy league temporal operations.
 *
 * <p>Business Logic Areas: - Current season management and configuration - Season validation and
 * boundary checks - Season navigation (previous/next) and relationships - Season information and
 * formatting - Temporal business rules and constraints
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeasonService - Business Critical TDD Tests")
class SeasonServiceTddTest {

  private SeasonService seasonService;

  // Test constants
  private static final int TEST_CURRENT_SEASON = 2025;
  private static final int TEST_MAX_FUTURE_YEARS = 5;
  private static final int FIRST_SEASON = 2024;
  private final int currentYear = Year.now().getValue();

  @BeforeEach
  void setUp() {
    seasonService = new SeasonService();
    // Configure test values using reflection
    ReflectionTestUtils.setField(seasonService, "configuredCurrentSeason", TEST_CURRENT_SEASON);
    ReflectionTestUtils.setField(seasonService, "maxFutureYears", TEST_MAX_FUTURE_YEARS);
  }

  @Nested
  @DisplayName("Current Season Management")
  class CurrentSeasonTests {

    @Test
    @DisplayName("Should return configured current season")
    void shouldReturnConfiguredCurrentSeason() {
      // RED: Test current season retrieval
      int result = seasonService.getCurrentSeason();

      assertThat(result).isEqualTo(TEST_CURRENT_SEASON);
    }

    @Test
    @DisplayName("Should return default season when not configured")
    void shouldReturnDefaultSeasonWhenNotConfigured() {
      // RED: Test default fallback
      ReflectionTestUtils.setField(seasonService, "configuredCurrentSeason", null);

      int result = seasonService.getCurrentSeason();

      assertThat(result).isEqualTo(2025); // Default value
    }

    @Test
    @DisplayName("Should identify current season correctly")
    void shouldIdentifyCurrentSeasonCorrectly() {
      // RED: Test current season identification
      boolean isCurrentSeason = seasonService.isCurrentSeason(TEST_CURRENT_SEASON);
      boolean isNotCurrentSeason = seasonService.isCurrentSeason(TEST_CURRENT_SEASON - 1);

      assertThat(isCurrentSeason).isTrue();
      assertThat(isNotCurrentSeason).isFalse();
    }

    @Test
    @DisplayName("Should handle season configuration changes dynamically")
    void shouldHandleSeasonConfigurationChangesDynamically() {
      // RED: Test dynamic configuration
      int originalSeason = seasonService.getCurrentSeason();
      assertThat(originalSeason).isEqualTo(TEST_CURRENT_SEASON);

      // Change configuration
      int newSeason = 2026;
      ReflectionTestUtils.setField(seasonService, "configuredCurrentSeason", newSeason);

      int updatedSeason = seasonService.getCurrentSeason();
      assertThat(updatedSeason).isEqualTo(newSeason);
    }
  }

  @Nested
  @DisplayName("Season Validation")
  class SeasonValidationTests {

    @Test
    @DisplayName("Should validate seasons within valid range")
    void shouldValidateSeasonsWithinValidRange() {
      // RED: Test valid season range
      boolean validFirst = seasonService.isValidSeason(FIRST_SEASON);
      boolean validCurrent = seasonService.isValidSeason(TEST_CURRENT_SEASON);
      boolean validMax = seasonService.isValidSeason(seasonService.getMaxValidSeason());

      assertThat(validFirst).isTrue();
      assertThat(validCurrent).isTrue();
      assertThat(validMax).isTrue();
    }

    @Test
    @DisplayName("Should reject seasons outside valid range")
    void shouldRejectSeasonsOutsideValidRange() {
      // RED: Test invalid season rejection
      boolean tooOld = seasonService.isValidSeason(FIRST_SEASON - 1);
      boolean tooFuture = seasonService.isValidSeason(seasonService.getMaxValidSeason() + 1);

      assertThat(tooOld).isFalse();
      assertThat(tooFuture).isFalse();
    }

    @Test
    @DisplayName("Should calculate correct maximum valid season")
    void shouldCalculateCorrectMaximumValidSeason() {
      // RED: Test max season calculation
      int maxSeason = seasonService.getMaxValidSeason();
      int expectedMax = currentYear + TEST_MAX_FUTURE_YEARS;

      assertThat(maxSeason).isEqualTo(expectedMax);
    }

    @Test
    @DisplayName("Should return correct first season")
    void shouldReturnCorrectFirstSeason() {
      // RED: Test first season retrieval
      int firstSeason = seasonService.getFirstSeason();

      assertThat(firstSeason).isEqualTo(FIRST_SEASON);
    }

    @Test
    @DisplayName("Should validate and normalize null season to current")
    void shouldValidateAndNormalizeNullSeasonToCurrent() {
      // RED: Test null season normalization
      int normalizedSeason = seasonService.validateAndNormalizeSeason(null);

      assertThat(normalizedSeason).isEqualTo(TEST_CURRENT_SEASON);
    }

    @Test
    @DisplayName("Should validate and return valid season unchanged")
    void shouldValidateAndReturnValidSeasonUnchanged() {
      // RED: Test valid season passthrough
      int validSeason = 2024;
      int result = seasonService.validateAndNormalizeSeason(validSeason);

      assertThat(result).isEqualTo(validSeason);
    }

    @Test
    @DisplayName("Should throw exception for invalid season")
    void shouldThrowExceptionForInvalidSeason() {
      // RED: Test invalid season exception
      int invalidSeason = FIRST_SEASON - 1;

      assertThatThrownBy(() -> seasonService.validateAndNormalizeSeason(invalidSeason))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Saison invalide")
          .hasMessageContaining(String.valueOf(invalidSeason));
    }
  }

  @Nested
  @DisplayName("Season Classification")
  class SeasonClassificationTests {

    @Test
    @DisplayName("Should correctly identify past seasons")
    void shouldCorrectlyIdentifyPastSeasons() {
      // RED: Test past season identification
      boolean isPast = seasonService.isPastSeason(TEST_CURRENT_SEASON - 1);
      boolean isNotPast = seasonService.isPastSeason(TEST_CURRENT_SEASON);

      assertThat(isPast).isTrue();
      assertThat(isNotPast).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify future seasons")
    void shouldCorrectlyIdentifyFutureSeasons() {
      // RED: Test future season identification
      boolean isFuture = seasonService.isFutureSeason(TEST_CURRENT_SEASON + 1);
      boolean isNotFuture = seasonService.isFutureSeason(TEST_CURRENT_SEASON);

      assertThat(isFuture).isTrue();
      assertThat(isNotFuture).isFalse();
    }

    @Test
    @DisplayName("Should handle edge cases in season classification")
    void shouldHandleEdgeCasesInSeasonClassification() {
      // RED: Test edge cases
      int currentSeason = TEST_CURRENT_SEASON;

      // Current season should not be past or future
      assertThat(seasonService.isPastSeason(currentSeason)).isFalse();
      assertThat(seasonService.isFutureSeason(currentSeason)).isFalse();
      assertThat(seasonService.isCurrentSeason(currentSeason)).isTrue();

      // Boundary seasons
      assertThat(seasonService.isPastSeason(currentSeason - 1)).isTrue();
      assertThat(seasonService.isFutureSeason(currentSeason + 1)).isTrue();
    }
  }

  @Nested
  @DisplayName("Season Navigation")
  class SeasonNavigationTests {

    @Test
    @DisplayName("Should return correct previous season")
    void shouldReturnCorrectPreviousSeason() {
      // RED: Test previous season calculation
      Integer previousSeason = seasonService.getPreviousSeason(TEST_CURRENT_SEASON);

      assertThat(previousSeason).isEqualTo(TEST_CURRENT_SEASON - 1);
    }

    @Test
    @DisplayName("Should return null for first season previous")
    void shouldReturnNullForFirstSeasonPrevious() {
      // RED: Test first season edge case
      Integer previousSeason = seasonService.getPreviousSeason(FIRST_SEASON);

      assertThat(previousSeason).isNull();
    }

    @Test
    @DisplayName("Should return correct next season")
    void shouldReturnCorrectNextSeason() {
      // RED: Test next season calculation
      Integer nextSeason = seasonService.getNextSeason(TEST_CURRENT_SEASON);

      assertThat(nextSeason).isEqualTo(TEST_CURRENT_SEASON + 1);
    }

    @Test
    @DisplayName("Should return null when next season exceeds maximum")
    void shouldReturnNullWhenNextSeasonExceedsMaximum() {
      // RED: Test maximum season boundary
      int maxSeason = seasonService.getMaxValidSeason();
      Integer nextSeason = seasonService.getNextSeason(maxSeason);

      assertThat(nextSeason).isNull();
    }

    @Test
    @DisplayName("Should calculate years between seasons correctly")
    void shouldCalculateYearsBetweenSeasonsCorrectly() {
      // RED: Test season difference calculation
      int years1 = seasonService.getYearsBetweenSeasons(2024, 2027);
      int years2 = seasonService.getYearsBetweenSeasons(2027, 2024);
      int yearsSame = seasonService.getYearsBetweenSeasons(2025, 2025);

      assertThat(years1).isEqualTo(3);
      assertThat(years2).isEqualTo(3); // Should be absolute
      assertThat(yearsSame).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("Season Lists and Collections")
  class SeasonCollectionTests {

    @Test
    @DisplayName("Should return all valid seasons")
    void shouldReturnAllValidSeasons() {
      // RED: Test valid seasons list
      List<Integer> validSeasons = seasonService.getAllValidSeasons();
      int expectedSize = seasonService.getMaxValidSeason() - FIRST_SEASON + 1;

      assertThat(validSeasons).hasSize(expectedSize);
      assertThat(validSeasons.get(0)).isEqualTo(FIRST_SEASON);
      assertThat(validSeasons.get(validSeasons.size() - 1))
          .isEqualTo(seasonService.getMaxValidSeason());
    }

    @Test
    @DisplayName("Should return available seasons (past and current only)")
    void shouldReturnAvailableSeasonsPastAndCurrentOnly() {
      // RED: Test available seasons list
      List<Integer> availableSeasons = seasonService.getAvailableSeasons();
      int expectedSize = TEST_CURRENT_SEASON - FIRST_SEASON + 1;

      assertThat(availableSeasons).hasSize(expectedSize);
      assertThat(availableSeasons.get(0)).isEqualTo(FIRST_SEASON);
      assertThat(availableSeasons.get(availableSeasons.size() - 1)).isEqualTo(TEST_CURRENT_SEASON);

      // Should not contain future seasons
      assertThat(availableSeasons).doesNotContain(TEST_CURRENT_SEASON + 1);
    }

    @Test
    @DisplayName("Should generate seasons in correct order")
    void shouldGenerateSeasonsInCorrectOrder() {
      // RED: Test season ordering
      List<Integer> seasons = seasonService.getAllValidSeasons();

      // Verify ascending order
      for (int i = 0; i < seasons.size() - 1; i++) {
        assertThat(seasons.get(i + 1)).isEqualTo(seasons.get(i) + 1);
      }
    }

    @Test
    @DisplayName("Should handle dynamic configuration changes in lists")
    void shouldHandleDynamicConfigurationChangesInLists() {
      // RED: Test dynamic list updates
      List<Integer> originalAvailable = seasonService.getAvailableSeasons();
      int originalSize = originalAvailable.size();

      // Change current season
      ReflectionTestUtils.setField(
          seasonService, "configuredCurrentSeason", TEST_CURRENT_SEASON + 1);

      List<Integer> updatedAvailable = seasonService.getAvailableSeasons();
      assertThat(updatedAvailable).hasSize(originalSize + 1);
      assertThat(updatedAvailable.get(updatedAvailable.size() - 1))
          .isEqualTo(TEST_CURRENT_SEASON + 1);
    }
  }

  @Nested
  @DisplayName("Season Information and Formatting")
  class SeasonInfoTests {

    @Test
    @DisplayName("Should format season correctly")
    void shouldFormatSeasonCorrectly() {
      // RED: Test season formatting
      String formatted = seasonService.formatSeason(TEST_CURRENT_SEASON);

      assertThat(formatted).isEqualTo("Saison " + TEST_CURRENT_SEASON);
    }

    @Test
    @DisplayName("Should create complete season info for current season")
    void shouldCreateCompleteSeasonInfoForCurrentSeason() {
      // RED: Test current season info creation
      SeasonInfo info = seasonService.getSeasonInfo(TEST_CURRENT_SEASON);

      assertThat(info.getSeason()).isEqualTo(TEST_CURRENT_SEASON);
      assertThat(info.isCurrent()).isTrue();
      assertThat(info.isPast()).isFalse();
      assertThat(info.isFuture()).isFalse();
      assertThat(info.getPreviousSeason()).isEqualTo(TEST_CURRENT_SEASON - 1);
      assertThat(info.getNextSeason()).isEqualTo(TEST_CURRENT_SEASON + 1);
      assertThat(info.getFormattedName()).isEqualTo("Saison " + TEST_CURRENT_SEASON);
    }

    @Test
    @DisplayName("Should create complete season info for past season")
    void shouldCreateCompleteSeasonInfoForPastSeason() {
      // RED: Test past season info creation
      // Use FIRST_SEASON (2024) as past season since TEST_CURRENT_SEASON is 2025
      int pastSeason = FIRST_SEASON; // 2024 is past relative to 2025
      SeasonInfo info = seasonService.getSeasonInfo(pastSeason);

      assertThat(info.getSeason()).isEqualTo(pastSeason);
      assertThat(info.isCurrent()).isFalse();
      assertThat(info.isPast()).isTrue();
      assertThat(info.isFuture()).isFalse();
      assertThat(info.getPreviousSeason()).isNull(); // 2024 is first season, no previous
      assertThat(info.getNextSeason()).isEqualTo(pastSeason + 1);
    }

    @Test
    @DisplayName("Should create complete season info for future season")
    void shouldCreateCompleteSeasonInfoForFutureSeason() {
      // RED: Test future season info creation
      int futureSeason = TEST_CURRENT_SEASON + 1;
      SeasonInfo info = seasonService.getSeasonInfo(futureSeason);

      assertThat(info.getSeason()).isEqualTo(futureSeason);
      assertThat(info.isCurrent()).isFalse();
      assertThat(info.isPast()).isFalse();
      assertThat(info.isFuture()).isTrue();
      assertThat(info.getPreviousSeason()).isEqualTo(futureSeason - 1);
      assertThat(info.getNextSeason()).isEqualTo(futureSeason + 1);
    }

    @Test
    @DisplayName("Should handle first season info correctly")
    void shouldHandleFirstSeasonInfoCorrectly() {
      // RED: Test first season edge case
      SeasonInfo info = seasonService.getSeasonInfo(FIRST_SEASON);

      assertThat(info.getSeason()).isEqualTo(FIRST_SEASON);
      assertThat(info.getPreviousSeason()).isNull(); // No previous season
      assertThat(info.getNextSeason()).isEqualTo(FIRST_SEASON + 1);
    }

    @Test
    @DisplayName("Should handle last valid season info correctly")
    void shouldHandleLastValidSeasonInfoCorrectly() {
      // RED: Test last season edge case
      int maxSeason = seasonService.getMaxValidSeason();
      SeasonInfo info = seasonService.getSeasonInfo(maxSeason);

      assertThat(info.getSeason()).isEqualTo(maxSeason);
      assertThat(info.getPreviousSeason()).isEqualTo(maxSeason - 1);
      assertThat(info.getNextSeason()).isNull(); // No next season
    }

    @Test
    @DisplayName("Should throw exception for invalid season info request")
    void shouldThrowExceptionForInvalidSeasonInfoRequest() {
      // RED: Test invalid season info
      int invalidSeason = FIRST_SEASON - 1;

      assertThatThrownBy(() -> seasonService.getSeasonInfo(invalidSeason))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Saison invalide");
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle extreme season values")
    void shouldHandleExtremeSeasonValues() {
      // RED: Test extreme values
      boolean veryOld = seasonService.isValidSeason(1900);
      boolean veryFuture = seasonService.isValidSeason(3000);

      assertThat(veryOld).isFalse();
      assertThat(veryFuture).isFalse();
    }

    @Test
    @DisplayName("Should handle zero and negative seasons")
    void shouldHandleZeroAndNegativeSeasons() {
      // RED: Test invalid inputs
      boolean zero = seasonService.isValidSeason(0);
      boolean negative = seasonService.isValidSeason(-2024);

      assertThat(zero).isFalse();
      assertThat(negative).isFalse();
    }

    @Test
    @DisplayName("Should maintain consistency across method calls")
    void shouldMaintainConsistencyAcrossMethodCalls() {
      // RED: Test consistency
      int currentSeason = seasonService.getCurrentSeason();

      // Multiple calls should return same result
      assertThat(seasonService.getCurrentSeason()).isEqualTo(currentSeason);
      assertThat(seasonService.isCurrentSeason(currentSeason)).isTrue();

      // Validation should be consistent
      assertThat(seasonService.isValidSeason(currentSeason)).isTrue();
      assertThat(seasonService.validateAndNormalizeSeason(currentSeason)).isEqualTo(currentSeason);
    }

    @Test
    @DisplayName("Should handle configuration edge cases")
    void shouldHandleConfigurationEdgeCases() {
      // RED: Test configuration boundaries

      // Max future years = 0
      ReflectionTestUtils.setField(seasonService, "maxFutureYears", 0);
      int maxSeason = seasonService.getMaxValidSeason();
      assertThat(maxSeason).isEqualTo(currentYear);

      // Very large max future years
      ReflectionTestUtils.setField(seasonService, "maxFutureYears", 100);
      int largeMaxSeason = seasonService.getMaxValidSeason();
      assertThat(largeMaxSeason).isEqualTo(currentYear + 100);
    }

    @Test
    @DisplayName("Should validate SeasonInfo toString method")
    void shouldValidateSeasonInfoToStringMethod() {
      // RED: Test toString implementation
      SeasonInfo info = seasonService.getSeasonInfo(TEST_CURRENT_SEASON);
      String toString = info.toString();

      assertThat(toString).contains("SeasonInfo");
      assertThat(toString).contains(String.valueOf(TEST_CURRENT_SEASON));
      assertThat(toString).contains("current=true");
      assertThat(toString).contains("past=false");
      assertThat(toString).contains("future=false");
    }
  }

  @Nested
  @DisplayName("Performance and Business Rules")
  class PerformanceAndBusinessTests {

    @Test
    @DisplayName("Should handle large season ranges efficiently")
    void shouldHandleLargeSeasonRangesEfficiently() {
      // RED: Test performance with large ranges
      ReflectionTestUtils.setField(seasonService, "maxFutureYears", 50);

      List<Integer> allSeasons = seasonService.getAllValidSeasons();

      // Should generate large list without issues
      assertThat(allSeasons.size()).isGreaterThan(50);
      assertThat(allSeasons.get(0)).isEqualTo(FIRST_SEASON);
      assertThat(allSeasons.get(allSeasons.size() - 1)).isEqualTo(currentYear + 50);
    }

    @Test
    @DisplayName("Should enforce business rule constraints")
    void shouldEnforceBusinessRuleConstraints() {
      // RED: Test business rules

      // Rule: Cannot have seasons before first season
      assertThat(seasonService.isValidSeason(FIRST_SEASON - 1)).isFalse();

      // Rule: Cannot go too far into future
      int tooFar = currentYear + 100;
      assertThat(seasonService.isValidSeason(tooFar)).isFalse();

      // Rule: Current season is always valid
      assertThat(seasonService.isValidSeason(seasonService.getCurrentSeason())).isTrue();
    }

    @Test
    @DisplayName("Should maintain immutable season logic")
    void shouldMaintainImmutableSeasonLogic() {
      // RED: Test immutability of calculations
      int season = TEST_CURRENT_SEASON;

      // Multiple calls should not affect state
      seasonService.isValidSeason(season);
      seasonService.isPastSeason(season);
      seasonService.isFutureSeason(season);

      // State should remain consistent
      assertThat(seasonService.getCurrentSeason()).isEqualTo(TEST_CURRENT_SEASON);
      assertThat(seasonService.getFirstSeason()).isEqualTo(FIRST_SEASON);
    }
  }
}
