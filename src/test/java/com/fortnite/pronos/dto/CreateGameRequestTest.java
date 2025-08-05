package com.fortnite.pronos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.model.Player;

/** Tests TDD pour CreateGameRequest */
@DisplayName("Tests TDD - CreateGameRequest")
class CreateGameRequestTest {

  private CreateGameRequest createGameRequest;

  @BeforeEach
  void setUp() {
    createGameRequest = new CreateGameRequest();
    createGameRequest.setName("Test Game");
    createGameRequest.setMaxParticipants(8);
    createGameRequest.setDescription("Une game de test pour les pronostics Fortnite");
    createGameRequest.setIsPrivate(false);
    createGameRequest.setAutoStartDraft(true);
    createGameRequest.setDraftTimeLimit(300); // 5 minutes
    createGameRequest.setAutoPickDelay(43200); // 12 heures
    createGameRequest.setCurrentSeason(2025);
  }

  @Test
  @DisplayName("Devrait créer un CreateGameRequest avec tous les champs")
  void shouldCreateCreateGameRequestWithAllFields() {
    // Then
    assertThat(createGameRequest.getName()).isEqualTo("Test Game");
    assertThat(createGameRequest.getMaxParticipants()).isEqualTo(8);
    assertThat(createGameRequest.getDescription())
        .isEqualTo("Une game de test pour les pronostics Fortnite");
    assertThat(createGameRequest.getIsPrivate()).isFalse();
    assertThat(createGameRequest.getAutoStartDraft()).isTrue();
    assertThat(createGameRequest.getDraftTimeLimit()).isEqualTo(300);
    assertThat(createGameRequest.getAutoPickDelay()).isEqualTo(43200);
    assertThat(createGameRequest.getCurrentSeason()).isEqualTo(2025);
  }

  @Test
  @DisplayName("Devrait mettre à jour les champs du CreateGameRequest")
  void shouldUpdateCreateGameRequestFields() {
    // Given
    String newName = "Updated Game Name";
    int newMaxParticipants = 10;
    String newDescription = "Description mise à jour";
    boolean newIsPrivate = true;

    // When
    createGameRequest.setName(newName);
    createGameRequest.setMaxParticipants(newMaxParticipants);
    createGameRequest.setDescription(newDescription);
    createGameRequest.setIsPrivate(newIsPrivate);

    // Then
    assertThat(createGameRequest.getName()).isEqualTo(newName);
    assertThat(createGameRequest.getMaxParticipants()).isEqualTo(newMaxParticipants);
    assertThat(createGameRequest.getDescription()).isEqualTo(newDescription);
    assertThat(createGameRequest.getIsPrivate()).isEqualTo(newIsPrivate);
  }

  @Test
  @DisplayName("Devrait valider un CreateGameRequest avec des valeurs correctes")
  void shouldValidateCreateGameRequestWithCorrectValues() {
    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
    assertThat(createGameRequest.getValidationErrors()).isEmpty();
  }

  @Test
  @DisplayName("Ne devrait pas valider un CreateGameRequest avec un nom vide")
  void shouldNotValidateCreateGameRequestWithEmptyName() {
    // Given
    createGameRequest.setName("");

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("Le nom de la game ne peut pas être vide");
  }

  @Test
  @DisplayName("Ne devrait pas valider un CreateGameRequest avec un nom null")
  void shouldNotValidateCreateGameRequestWithNullName() {
    // Given
    createGameRequest.setName(null);

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("Le nom de la game ne peut pas être vide");
  }

  @Test
  @DisplayName("Ne devrait pas valider un CreateGameRequest avec un nom trop court")
  void shouldNotValidateCreateGameRequestWithTooShortName() {
    // Given
    createGameRequest.setName("A");

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("Le nom de la game doit contenir entre 3 et 50 caractères");
  }

  @Test
  @DisplayName("Ne devrait pas valider un CreateGameRequest avec un nom trop long")
  void shouldNotValidateCreateGameRequestWithTooLongName() {
    // Given
    createGameRequest.setName("A".repeat(51));

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("Le nom de la game doit contenir entre 3 et 50 caractères");
  }

  @Test
  @DisplayName("Ne devrait pas valider un CreateGameRequest avec maxParticipants null")
  void shouldNotValidateCreateGameRequestWithNullMaxParticipants() {
    // Given
    createGameRequest.setMaxParticipants(null);

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("Le nombre maximum de participants est requis");
  }

  @Test
  @DisplayName("Ne devrait pas valider un CreateGameRequest avec maxParticipants trop petit")
  void shouldNotValidateCreateGameRequestWithTooSmallMaxParticipants() {
    // Given
    createGameRequest.setMaxParticipants(1);

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("Le nombre maximum de participants doit être entre 2 et 20");
  }

  @Test
  @DisplayName("Ne devrait pas valider un CreateGameRequest avec maxParticipants trop grand")
  void shouldNotValidateCreateGameRequestWithTooLargeMaxParticipants() {
    // Given
    createGameRequest.setMaxParticipants(21);

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("Le nombre maximum de participants doit être entre 2 et 20");
  }

  @Test
  @DisplayName("Devrait valider un CreateGameRequest avec maxParticipants à 2")
  void shouldValidateCreateGameRequestWithMinMaxParticipants() {
    // Given
    createGameRequest.setMaxParticipants(2);

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un CreateGameRequest avec maxParticipants à 20")
  void shouldValidateCreateGameRequestWithMaxMaxParticipants() {
    // Given
    createGameRequest.setMaxParticipants(20);

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Ne devrait pas valider un CreateGameRequest avec une description trop longue")
  void shouldNotValidateCreateGameRequestWithTooLongDescription() {
    // Given
    createGameRequest.setDescription("A".repeat(501));

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("La description ne peut pas dépasser 500 caractères");
  }

  @Test
  @DisplayName("Devrait valider un CreateGameRequest avec une description vide")
  void shouldValidateCreateGameRequestWithEmptyDescription() {
    // Given
    createGameRequest.setDescription("");

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Ne devrait pas valider un CreateGameRequest avec draftTimeLimit trop petit")
  void shouldNotValidateCreateGameRequestWithTooSmallDraftTimeLimit() {
    // Given
    createGameRequest.setDraftTimeLimit(30); // 30 secondes

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("La limite de temps par sélection doit être entre 60 et 3600 secondes");
  }

  @Test
  @DisplayName("Ne devrait pas valider un CreateGameRequest avec draftTimeLimit trop grand")
  void shouldNotValidateCreateGameRequestWithTooLargeDraftTimeLimit() {
    // Given
    createGameRequest.setDraftTimeLimit(7200); // 2 heures

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("La limite de temps par sélection doit être entre 60 et 3600 secondes");
  }

  @Test
  @DisplayName("Ne devrait pas valider un CreateGameRequest avec autoPickDelay trop petit")
  void shouldNotValidateCreateGameRequestWithTooSmallAutoPickDelay() {
    // Given
    createGameRequest.setAutoPickDelay(60); // 1 minute

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("Le délai d'auto-pick doit être entre 300 et 86400 secondes");
  }

  @Test
  @DisplayName("Ne devrait pas valider un CreateGameRequest avec autoPickDelay trop grand")
  void shouldNotValidateCreateGameRequestWithTooLargeAutoPickDelay() {
    // Given
    createGameRequest.setAutoPickDelay(172800); // 48 heures

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("Le délai d'auto-pick doit être entre 300 et 86400 secondes");
  }

  @Test
  @DisplayName("Devrait gérer les règles de région")
  void shouldHandleRegionRules() {
    // Given
    Map<Player.Region, Integer> regionRules = new HashMap<>();
    regionRules.put(Player.Region.EU, 3);
    regionRules.put(Player.Region.NAC, 2);
    regionRules.put(Player.Region.BR, 1);

    // When
    createGameRequest.setRegionRules(regionRules);

    // Then
    assertThat(createGameRequest.getRegionRules()).isEqualTo(regionRules);
    assertThat(createGameRequest.getRegionRules().get(Player.Region.EU)).isEqualTo(3);
    assertThat(createGameRequest.getRegionRules().get(Player.Region.NAC)).isEqualTo(2);
    assertThat(createGameRequest.getRegionRules().get(Player.Region.BR)).isEqualTo(1);
  }

  @Test
  @DisplayName("Devrait valider les règles de région")
  void shouldValidateRegionRules() {
    // Given
    Map<Player.Region, Integer> regionRules = new HashMap<>();
    regionRules.put(Player.Region.EU, 3);
    regionRules.put(Player.Region.NAC, 2);
    createGameRequest.setRegionRules(regionRules);

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Ne devrait pas valider des règles de région avec des valeurs négatives")
  void shouldNotValidateRegionRulesWithNegativeValues() {
    // Given
    Map<Player.Region, Integer> regionRules = new HashMap<>();
    regionRules.put(Player.Region.EU, -1);
    createGameRequest.setRegionRules(regionRules);

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("Le nombre de joueurs par région doit être positif");
  }

  @Test
  @DisplayName("Ne devrait pas valider des règles de région avec des valeurs trop grandes")
  void shouldNotValidateRegionRulesWithTooLargeValues() {
    // Given
    Map<Player.Region, Integer> regionRules = new HashMap<>();
    regionRules.put(Player.Region.EU, 15);
    createGameRequest.setRegionRules(regionRules);

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains("Le nombre de joueurs par région ne peut pas dépasser 10");
  }

  @Test
  @DisplayName("Devrait calculer le nombre total de joueurs des règles de région")
  void shouldCalculateTotalPlayersFromRegionRules() {
    // Given
    Map<Player.Region, Integer> regionRules = new HashMap<>();
    regionRules.put(Player.Region.EU, 3);
    regionRules.put(Player.Region.NAC, 2);
    regionRules.put(Player.Region.BR, 1);
    createGameRequest.setRegionRules(regionRules);

    // When
    int totalPlayers = createGameRequest.getTotalPlayersFromRegionRules();

    // Then
    assertThat(totalPlayers).isEqualTo(6);
  }

  @Test
  @DisplayName("Devrait retourner 0 pour le total de joueurs sans règles de région")
  void shouldReturnZeroForTotalPlayersWithoutRegionRules() {
    // When
    int totalPlayers = createGameRequest.getTotalPlayersFromRegionRules();

    // Then
    assertThat(totalPlayers).isEqualTo(0);
  }

  @Test
  @DisplayName("Devrait vérifier si les règles de région sont cohérentes avec maxParticipants")
  void shouldCheckIfRegionRulesAreConsistentWithMaxParticipants() {
    // Given
    createGameRequest.setMaxParticipants(8);
    Map<Player.Region, Integer> regionRules = new HashMap<>();
    regionRules.put(Player.Region.EU, 3);
    regionRules.put(Player.Region.NAC, 2);
    regionRules.put(Player.Region.BR, 1);
    createGameRequest.setRegionRules(regionRules);

    // When
    boolean isConsistent = createGameRequest.areRegionRulesConsistent();

    // Then
    assertThat(isConsistent).isTrue();
  }

  @Test
  @DisplayName("Ne devrait pas valider des règles de région incohérentes avec maxParticipants")
  void shouldNotValidateInconsistentRegionRulesWithMaxParticipants() {
    // Given
    createGameRequest.setMaxParticipants(5);
    Map<Player.Region, Integer> regionRules = new HashMap<>();
    regionRules.put(Player.Region.EU, 3);
    regionRules.put(Player.Region.NAC, 2);
    regionRules.put(Player.Region.BR, 1);
    createGameRequest.setRegionRules(regionRules);

    // When
    boolean isValid = createGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(createGameRequest.getValidationErrors())
        .contains(
            "Le total des joueurs par région (6) ne peut pas dépasser le nombre maximum de participants (5)");
  }

  @Test
  @DisplayName("Devrait créer un CreateGameRequest avec le constructeur")
  void shouldCreateCreateGameRequestWithConstructor() {
    // When
    CreateGameRequest newRequest =
        new CreateGameRequest("New Game", 6, "Nouvelle game", false, true, 600, 86400, 2025);

    // Then
    assertThat(newRequest.getName()).isEqualTo("New Game");
    assertThat(newRequest.getMaxParticipants()).isEqualTo(6);
    assertThat(newRequest.getDescription()).isEqualTo("Nouvelle game");
    assertThat(newRequest.getIsPrivate()).isFalse();
    assertThat(newRequest.getAutoStartDraft()).isTrue();
    assertThat(newRequest.getDraftTimeLimit()).isEqualTo(600);
    assertThat(newRequest.getAutoPickDelay()).isEqualTo(86400);
    assertThat(newRequest.getCurrentSeason()).isEqualTo(2025);
  }

  @Test
  @DisplayName("Devrait comparer deux CreateGameRequest égaux")
  void shouldCompareEqualCreateGameRequests() {
    // Given
    CreateGameRequest request2 = new CreateGameRequest();
    request2.setName("Test Game");
    request2.setMaxParticipants(8);
    request2.setDescription("Une game de test pour les pronostics Fortnite");
    request2.setIsPrivate(false);
    request2.setAutoStartDraft(true);
    request2.setDraftTimeLimit(300);
    request2.setAutoPickDelay(43200);
    request2.setCurrentSeason(2025);

    // When & Then
    assertThat(createGameRequest).isEqualTo(request2);
    assertThat(createGameRequest.hashCode()).isEqualTo(request2.hashCode());
  }

  @Test
  @DisplayName("Devrait comparer deux CreateGameRequest différents")
  void shouldCompareDifferentCreateGameRequests() {
    // Given
    CreateGameRequest request2 = new CreateGameRequest();
    request2.setName("Different Game");
    request2.setMaxParticipants(10);

    // When & Then
    assertThat(createGameRequest).isNotEqualTo(request2);
    assertThat(createGameRequest.hashCode()).isNotEqualTo(request2.hashCode());
  }

  @Test
  @DisplayName("Devrait générer une représentation string correcte")
  void shouldGenerateCorrectStringRepresentation() {
    // When
    String stringRepresentation = createGameRequest.toString();

    // Then
    assertThat(stringRepresentation).contains("Test Game");
    assertThat(stringRepresentation).contains("8");
    assertThat(stringRepresentation).contains("2025");
  }

  @Test
  @DisplayName("Devrait effacer les erreurs de validation")
  void shouldClearValidationErrors() {
    // Given
    createGameRequest.setName("");
    createGameRequest.isValid(); // Génère des erreurs

    // When
    createGameRequest.clearValidationErrors();

    // Then
    assertThat(createGameRequest.getValidationErrors()).isEmpty();
  }

  @Test
  @DisplayName("Devrait ajouter une erreur de validation")
  void shouldAddValidationError() {
    // When
    createGameRequest.addValidationError("Erreur de test");

    // Then
    assertThat(createGameRequest.getValidationErrors()).contains("Erreur de test");
  }
}
