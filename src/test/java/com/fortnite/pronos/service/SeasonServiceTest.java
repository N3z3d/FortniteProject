package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Year;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Tests unitaires pour SeasonService */
@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

  @InjectMocks private SeasonService seasonService;

  @BeforeEach
  void setUp() {
    // Configuration des valeurs par défaut pour les tests
    ReflectionTestUtils.setField(seasonService, "configuredCurrentSeason", 2025);
    ReflectionTestUtils.setField(seasonService, "maxFutureYears", 5);
  }

  @Test
  void getCurrentSeason_ShouldReturnConfiguredSeason() {
    // When
    int currentSeason = seasonService.getCurrentSeason();

    // Then
    assertThat(currentSeason).isEqualTo(2025);
  }

  @Test
  void getCurrentSeason_WhenConfiguredSeasonIsNull_ShouldReturnDefaultSeason() {
    // Given
    ReflectionTestUtils.setField(seasonService, "configuredCurrentSeason", null);

    // When
    int currentSeason = seasonService.getCurrentSeason();

    // Then
    assertThat(currentSeason).isEqualTo(2025); // DEFAULT_CURRENT_SEASON
  }

  @Test
  void isValidSeason_WithValidSeason_ShouldReturnTrue() {
    // Given
    int validSeason = 2025;

    // When
    boolean isValid = seasonService.isValidSeason(validSeason);

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  void isValidSeason_WithSeasonTooOld_ShouldReturnFalse() {
    // Given
    int oldSeason = 2023;

    // When
    boolean isValid = seasonService.isValidSeason(oldSeason);

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  void isValidSeason_WithSeasonTooFuture_ShouldReturnFalse() {
    // Given
    int futureSeason = Year.now().getValue() + 10;

    // When
    boolean isValid = seasonService.isValidSeason(futureSeason);

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  void getMaxValidSeason_ShouldReturnCurrentYearPlusFutureYears() {
    // When
    int maxSeason = seasonService.getMaxValidSeason();

    // Then
    int expectedMaxSeason = Year.now().getValue() + 5;
    assertThat(maxSeason).isEqualTo(expectedMaxSeason);
  }

  @Test
  void getFirstSeason_ShouldReturn2024() {
    // When
    int firstSeason = seasonService.getFirstSeason();

    // Then
    assertThat(firstSeason).isEqualTo(2024);
  }

  @Test
  void isCurrentSeason_WithCurrentSeason_ShouldReturnTrue() {
    // Given
    int currentSeason = 2025;

    // When
    boolean isCurrent = seasonService.isCurrentSeason(currentSeason);

    // Then
    assertThat(isCurrent).isTrue();
  }

  @Test
  void isCurrentSeason_WithDifferentSeason_ShouldReturnFalse() {
    // Given
    int differentSeason = 2024;

    // When
    boolean isCurrent = seasonService.isCurrentSeason(differentSeason);

    // Then
    assertThat(isCurrent).isFalse();
  }

  @Test
  void isPastSeason_WithPastSeason_ShouldReturnTrue() {
    // Given
    int pastSeason = 2024;

    // When
    boolean isPast = seasonService.isPastSeason(pastSeason);

    // Then
    assertThat(isPast).isTrue();
  }

  @Test
  void isPastSeason_WithCurrentSeason_ShouldReturnFalse() {
    // Given
    int currentSeason = 2025;

    // When
    boolean isPast = seasonService.isPastSeason(currentSeason);

    // Then
    assertThat(isPast).isFalse();
  }

  @Test
  void isFutureSeason_WithFutureSeason_ShouldReturnTrue() {
    // Given
    int futureSeason = 2026;

    // When
    boolean isFuture = seasonService.isFutureSeason(futureSeason);

    // Then
    assertThat(isFuture).isTrue();
  }

  @Test
  void isFutureSeason_WithCurrentSeason_ShouldReturnFalse() {
    // Given
    int currentSeason = 2025;

    // When
    boolean isFuture = seasonService.isFutureSeason(currentSeason);

    // Then
    assertThat(isFuture).isFalse();
  }

  @Test
  void getPreviousSeason_WithValidSeason_ShouldReturnPreviousYear() {
    // Given
    int season = 2025;

    // When
    Integer previousSeason = seasonService.getPreviousSeason(season);

    // Then
    assertThat(previousSeason).isEqualTo(2024);
  }

  @Test
  void getPreviousSeason_WithFirstSeason_ShouldReturnNull() {
    // Given
    int firstSeason = 2024;

    // When
    Integer previousSeason = seasonService.getPreviousSeason(firstSeason);

    // Then
    assertThat(previousSeason).isNull();
  }

  @Test
  void getNextSeason_WithValidSeason_ShouldReturnNextYear() {
    // Given
    int season = 2025;

    // When
    Integer nextSeason = seasonService.getNextSeason(season);

    // Then
    assertThat(nextSeason).isEqualTo(2026);
  }

  @Test
  void getNextSeason_WithMaxSeason_ShouldReturnNull() {
    // Given
    int maxSeason = seasonService.getMaxValidSeason();

    // When
    Integer nextSeason = seasonService.getNextSeason(maxSeason);

    // Then
    assertThat(nextSeason).isNull();
  }

  @Test
  void getAllValidSeasons_ShouldReturnAllSeasonsFromFirstToMax() {
    // When
    List<Integer> validSeasons = seasonService.getAllValidSeasons();

    // Then
    assertThat(validSeasons).isNotEmpty();
    assertThat(validSeasons.get(0)).isEqualTo(2024);
    assertThat(validSeasons.get(validSeasons.size() - 1))
        .isEqualTo(seasonService.getMaxValidSeason());
    assertThat(validSeasons).containsSequence(2024, 2025, 2026);
  }

  @Test
  void getAvailableSeasons_ShouldReturnSeasonsFromFirstToCurrent() {
    // When
    List<Integer> availableSeasons = seasonService.getAvailableSeasons();

    // Then
    assertThat(availableSeasons).isNotEmpty();
    assertThat(availableSeasons.get(0)).isEqualTo(2024);
    assertThat(availableSeasons.get(availableSeasons.size() - 1)).isEqualTo(2025);
    assertThat(availableSeasons).containsSequence(2024, 2025);
  }

  @Test
  void validateAndNormalizeSeason_WithNullSeason_ShouldReturnCurrentSeason() {
    // When
    int normalizedSeason = seasonService.validateAndNormalizeSeason(null);

    // Then
    assertThat(normalizedSeason).isEqualTo(2025);
  }

  @Test
  void validateAndNormalizeSeason_WithValidSeason_ShouldReturnSameSeason() {
    // Given
    int validSeason = 2025;

    // When
    int normalizedSeason = seasonService.validateAndNormalizeSeason(validSeason);

    // Then
    assertThat(normalizedSeason).isEqualTo(validSeason);
  }

  @Test
  void validateAndNormalizeSeason_WithInvalidSeason_ShouldThrowException() {
    // Given
    int invalidSeason = 2023;

    // When & Then
    assertThatThrownBy(() -> seasonService.validateAndNormalizeSeason(invalidSeason))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Saison invalide: 2023");
  }

  @Test
  void getYearsBetweenSeasons_ShouldReturnAbsoluteDifference() {
    // When
    int yearsBetween1 = seasonService.getYearsBetweenSeasons(2024, 2026);
    int yearsBetween2 = seasonService.getYearsBetweenSeasons(2026, 2024);

    // Then
    assertThat(yearsBetween1).isEqualTo(2);
    assertThat(yearsBetween2).isEqualTo(2);
  }

  @Test
  void getYearsBetweenSeasons_WithSameSeason_ShouldReturnZero() {
    // When
    int yearsBetween = seasonService.getYearsBetweenSeasons(2025, 2025);

    // Then
    assertThat(yearsBetween).isEqualTo(0);
  }

  @Test
  void formatSeason_ShouldReturnFormattedString() {
    // Given
    int season = 2025;

    // When
    String formatted = seasonService.formatSeason(season);

    // Then
    assertThat(formatted).isEqualTo("Saison 2025");
  }

  @Test
  void getSeasonInfo_WithCurrentSeason_ShouldReturnCompleteInfo() {
    // Given
    int currentSeason = 2025;

    // When
    SeasonService.SeasonInfo info = seasonService.getSeasonInfo(currentSeason);

    // Then
    assertThat(info.getSeason()).isEqualTo(2025);
    assertThat(info.isCurrent()).isTrue();
    assertThat(info.isPast()).isFalse();
    assertThat(info.isFuture()).isFalse();
    assertThat(info.getPreviousSeason()).isEqualTo(2024);
    assertThat(info.getNextSeason()).isEqualTo(2026);
    assertThat(info.getFormattedName()).isEqualTo("Saison 2025");
  }

  @Test
  void getSeasonInfo_WithPastSeason_ShouldReturnCorrectFlags() {
    // Given
    int pastSeason = 2024;

    // When
    SeasonService.SeasonInfo info = seasonService.getSeasonInfo(pastSeason);

    // Then
    assertThat(info.getSeason()).isEqualTo(2024);
    assertThat(info.isCurrent()).isFalse();
    assertThat(info.isPast()).isTrue();
    assertThat(info.isFuture()).isFalse();
  }

  @Test
  void getSeasonInfo_WithFutureSeason_ShouldReturnCorrectFlags() {
    // Given
    int futureSeason = 2026;

    // When
    SeasonService.SeasonInfo info = seasonService.getSeasonInfo(futureSeason);

    // Then
    assertThat(info.getSeason()).isEqualTo(2026);
    assertThat(info.isCurrent()).isFalse();
    assertThat(info.isPast()).isFalse();
    assertThat(info.isFuture()).isTrue();
  }

  @Test
  void getSeasonInfo_WithFirstSeason_ShouldHaveNullPreviousSeason() {
    // Given
    int firstSeason = 2024;

    // When
    SeasonService.SeasonInfo info = seasonService.getSeasonInfo(firstSeason);

    // Then
    assertThat(info.getPreviousSeason()).isNull();
    assertThat(info.getNextSeason()).isEqualTo(2025);
  }

  @Test
  void getSeasonInfo_WithInvalidSeason_ShouldThrowException() {
    // Given
    int invalidSeason = 2023;

    // When & Then
    assertThatThrownBy(() -> seasonService.getSeasonInfo(invalidSeason))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void seasonInfo_ToString_ShouldReturnReadableString() {
    // Given
    SeasonService.SeasonInfo info = seasonService.getSeasonInfo(2025);

    // When
    String toString = info.toString();

    // Then
    assertThat(toString).contains("SeasonInfo");
    assertThat(toString).contains("season=2025");
    assertThat(toString).contains("current=true");
    assertThat(toString).contains("past=false");
    assertThat(toString).contains("future=false");
  }

  @Test
  void seasonInfo_Builder_ShouldCreateValidObject() {
    // When
    SeasonService.SeasonInfo info =
        SeasonService.SeasonInfo.builder()
            .season(2025)
            .isCurrent(true)
            .isPast(false)
            .isFuture(false)
            .previousSeason(2024)
            .nextSeason(2026)
            .formattedName("Saison 2025")
            .build();

    // Then
    assertThat(info.getSeason()).isEqualTo(2025);
    assertThat(info.isCurrent()).isTrue();
    assertThat(info.isPast()).isFalse();
    assertThat(info.isFuture()).isFalse();
    assertThat(info.getPreviousSeason()).isEqualTo(2024);
    assertThat(info.getNextSeason()).isEqualTo(2026);
    assertThat(info.getFormattedName()).isEqualTo("Saison 2025");
  }

  @Test
  void integration_ValidateCompleteWorkflow() {
    // Test d'intégration vérifiant un workflow complet

    // 1. Obtenir la saison courante
    int currentSeason = seasonService.getCurrentSeason();
    assertThat(currentSeason).isEqualTo(2025);

    // 2. Valider que c'est une saison valide
    assertThat(seasonService.isValidSeason(currentSeason)).isTrue();

    // 3. Obtenir les informations complètes
    SeasonService.SeasonInfo info = seasonService.getSeasonInfo(currentSeason);
    assertThat(info.isCurrent()).isTrue();

    // 4. Vérifier les saisons adjacentes
    assertThat(info.getPreviousSeason()).isEqualTo(2024);
    assertThat(info.getNextSeason()).isEqualTo(2026);

    // 5. Vérifier la liste des saisons disponibles
    List<Integer> availableSeasons = seasonService.getAvailableSeasons();
    assertThat(availableSeasons).contains(currentSeason);
    assertThat(availableSeasons).contains(2024);

    // 6. Vérifier le formatage
    assertThat(info.getFormattedName()).isEqualTo("Saison 2025");
  }
}
