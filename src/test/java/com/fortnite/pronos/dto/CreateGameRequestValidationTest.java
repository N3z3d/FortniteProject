package com.fortnite.pronos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.model.Player;

/** Tests de validation pour CreateGameRequest */
public class CreateGameRequestValidationTest {

  private CreateGameRequest request;

  @BeforeEach
  void setUp() {
    request = new CreateGameRequest();
  }

  @Test
  void should_BeValid_When_MinimalValidData() {
    // Given
    request.setName("Test Game");
    request.setMaxParticipants(5);

    // When
    boolean isValid = request.isValid();

    // Then
    assertThat(isValid).isTrue();
    assertThat(request.getValidationErrors()).isEmpty();
  }

  @Test
  void should_BeInvalid_When_NameIsNull() {
    // Given
    request.setName(null);
    request.setMaxParticipants(5);

    // When
    boolean isValid = request.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(request.getValidationErrors())
        .containsExactly("Le nom de la game ne peut pas être vide");
  }

  @Test
  void should_BeInvalid_When_NameIsEmpty() {
    // Given
    request.setName("");
    request.setMaxParticipants(5);

    // When
    boolean isValid = request.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(request.getValidationErrors())
        .containsExactly("Le nom de la game ne peut pas être vide");
  }

  @Test
  void should_BeInvalid_When_NameIsTooShort() {
    // Given
    request.setName("AB");
    request.setMaxParticipants(5);

    // When
    boolean isValid = request.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(request.getValidationErrors())
        .contains("Le nom de la game doit contenir entre 3 et 50 caractères");
  }

  @Test
  void should_BeInvalid_When_NameIsTooLong() {
    // Given
    request.setName("A".repeat(51));
    request.setMaxParticipants(5);

    // When
    boolean isValid = request.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(request.getValidationErrors())
        .contains("Le nom de la game doit contenir entre 3 et 50 caractères");
  }

  @Test
  void should_BeInvalid_When_MaxParticipantsIsNull() {
    // Given
    request.setName("Test Game");
    request.setMaxParticipants(null);

    // When
    boolean isValid = request.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(request.getValidationErrors())
        .contains("Le nombre maximum de participants est requis");
  }

  @Test
  void should_BeInvalid_When_MaxParticipantsIsTooLow() {
    // Given
    request.setName("Test Game");
    request.setMaxParticipants(1);

    // When
    boolean isValid = request.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(request.getValidationErrors())
        .contains("Le nombre maximum de participants doit être entre 2 et 20");
  }

  @Test
  void should_BeInvalid_When_MaxParticipantsIsTooHigh() {
    // Given
    request.setName("Test Game");
    request.setMaxParticipants(21);

    // When
    boolean isValid = request.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(request.getValidationErrors())
        .contains("Le nombre maximum de participants doit être entre 2 et 20");
  }

  @Test
  void should_BeValid_When_ValidRegionRules() {
    // Given
    request.setName("Test Game");
    request.setMaxParticipants(10);

    Map<Player.Region, Integer> regionRules = new HashMap<>();
    regionRules.put(Player.Region.EU, 5);
    regionRules.put(Player.Region.NAC, 5);
    request.setRegionRules(regionRules);

    // When
    boolean isValid = request.isValid();

    // Then
    assertThat(isValid).isTrue();
    assertThat(request.getValidationErrors()).isEmpty();
  }

  @Test
  void should_BeInvalid_When_RegionRulesExceedMaxParticipants() {
    // Given
    request.setName("Test Game");
    request.setMaxParticipants(5);

    Map<Player.Region, Integer> regionRules = new HashMap<>();
    regionRules.put(Player.Region.EU, 5);
    regionRules.put(Player.Region.NAC, 5); // Total = 10 > maxParticipants = 5
    request.setRegionRules(regionRules);

    // When
    boolean isValid = request.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(request.getValidationErrors())
        .anyMatch(error -> error.contains("Le total des joueurs par région"));
  }

  @Test
  void should_UseDefaults_When_OptionalFieldsAreNull() {
    // Given
    request.setName("Test Game");
    request.setMaxParticipants(5);
    request.setCurrentSeason(null);
    request.setDraftTimeLimit(null);
    request.setAutoPickDelay(null);

    // When
    int season = request.getCurrentSeasonOrDefault();
    int draftTime = request.getDraftTimeLimitOrDefault();
    int autoPickTime = request.getAutoPickDelayOrDefault();

    // Then
    assertThat(season).isEqualTo(2025);
    assertThat(draftTime).isEqualTo(300); // 5 minutes
    assertThat(autoPickTime).isEqualTo(43200); // 12 hours
  }

  @Test
  void should_ReturnCorrectTotalPlayers_When_RegionRulesSet() {
    // Given
    request.setName("Test Game");
    request.setMaxParticipants(10);

    Map<Player.Region, Integer> regionRules = new HashMap<>();
    regionRules.put(Player.Region.EU, 3);
    regionRules.put(Player.Region.NAC, 2);
    regionRules.put(Player.Region.ASIA, 4);
    request.setRegionRules(regionRules);

    // When
    int totalPlayers = request.getTotalPlayersFromRegionRules();

    // Then
    assertThat(totalPlayers).isEqualTo(9);
  }
}
