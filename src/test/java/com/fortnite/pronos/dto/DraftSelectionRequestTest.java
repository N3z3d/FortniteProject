package com.fortnite.pronos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests TDD pour DraftSelectionRequest */
@DisplayName("Tests TDD - DraftSelectionRequest")
class DraftSelectionRequestTest {

  private DraftSelectionRequest draftSelectionRequest;
  private UUID testGameId;
  private UUID testUserId;
  private UUID testPlayerId;

  @BeforeEach
  void setUp() {
    testGameId = UUID.randomUUID();
    testUserId = UUID.randomUUID();
    testPlayerId = UUID.randomUUID();

    draftSelectionRequest = new DraftSelectionRequest();
    draftSelectionRequest.setGameId(testGameId);
    draftSelectionRequest.setUserId(testUserId);
    draftSelectionRequest.setPlayerId(testPlayerId);
    draftSelectionRequest.setRound(1);
    draftSelectionRequest.setPick(3);
    draftSelectionRequest.setIsAutoPick(false);
    draftSelectionRequest.setSelectionTime(30000L); // 30 secondes
  }

  @Test
  @DisplayName("Devrait créer un DraftSelectionRequest avec tous les champs")
  void shouldCreateDraftSelectionRequestWithAllFields() {
    // Then
    assertThat(draftSelectionRequest.getGameId()).isEqualTo(testGameId);
    assertThat(draftSelectionRequest.getUserId()).isEqualTo(testUserId);
    assertThat(draftSelectionRequest.getPlayerId()).isEqualTo(testPlayerId);
    assertThat(draftSelectionRequest.getRound()).isEqualTo(1);
    assertThat(draftSelectionRequest.getPick()).isEqualTo(3);
    assertThat(draftSelectionRequest.getIsAutoPick()).isFalse();
    assertThat(draftSelectionRequest.getSelectionTime()).isEqualTo(30000L);
  }

  @Test
  @DisplayName("Devrait mettre à jour les champs du DraftSelectionRequest")
  void shouldUpdateDraftSelectionRequestFields() {
    // Given
    UUID newPlayerId = UUID.randomUUID();
    int newRound = 2;
    int newPick = 5;
    boolean newIsAutoPick = true;
    Long newSelectionTime = 45000L;

    // When
    draftSelectionRequest.setPlayerId(newPlayerId);
    draftSelectionRequest.setRound(newRound);
    draftSelectionRequest.setPick(newPick);
    draftSelectionRequest.setIsAutoPick(newIsAutoPick);
    draftSelectionRequest.setSelectionTime(newSelectionTime);

    // Then
    assertThat(draftSelectionRequest.getPlayerId()).isEqualTo(newPlayerId);
    assertThat(draftSelectionRequest.getRound()).isEqualTo(newRound);
    assertThat(draftSelectionRequest.getPick()).isEqualTo(newPick);
    assertThat(draftSelectionRequest.getIsAutoPick()).isEqualTo(newIsAutoPick);
    assertThat(draftSelectionRequest.getSelectionTime()).isEqualTo(newSelectionTime);
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec des valeurs correctes")
  void shouldValidateDraftSelectionRequestWithCorrectValues() {
    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
    assertThat(draftSelectionRequest.getValidationErrors()).isEmpty();
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un gameId null")
  void shouldNotValidateDraftSelectionRequestWithNullGameId() {
    // Given
    draftSelectionRequest.setGameId(null);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors()).contains("L'ID de la game est requis");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un userId null")
  void shouldNotValidateDraftSelectionRequestWithNullUserId() {
    // Given
    draftSelectionRequest.setUserId(null);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("L'ID de l'utilisateur est requis");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un playerId null")
  void shouldNotValidateDraftSelectionRequestWithNullPlayerId() {
    // Given
    draftSelectionRequest.setPlayerId(null);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors()).contains("L'ID du joueur est requis");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un round négatif")
  void shouldNotValidateDraftSelectionRequestWithNegativeRound() {
    // Given
    draftSelectionRequest.setRound(-1);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le numéro de round doit être positif");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un round null")
  void shouldNotValidateDraftSelectionRequestWithNullRound() {
    // Given
    draftSelectionRequest.setRound(null);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le numéro de round est requis");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un pick négatif")
  void shouldNotValidateDraftSelectionRequestWithNegativePick() {
    // Given
    draftSelectionRequest.setPick(-1);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le numéro de pick doit être positif");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un pick null")
  void shouldNotValidateDraftSelectionRequestWithNullPick() {
    // Given
    draftSelectionRequest.setPick(null);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le numéro de pick est requis");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un round trop grand")
  void shouldNotValidateDraftSelectionRequestWithTooLargeRound() {
    // Given
    draftSelectionRequest.setRound(101);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le numéro de round ne peut pas dépasser 100");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un pick trop grand")
  void shouldNotValidateDraftSelectionRequestWithTooLargePick() {
    // Given
    draftSelectionRequest.setPick(201);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le numéro de pick ne peut pas dépasser 200");
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec un round à 1")
  void shouldValidateDraftSelectionRequestWithRoundOne() {
    // Given
    draftSelectionRequest.setRound(1);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec un round à 100")
  void shouldValidateDraftSelectionRequestWithRoundHundred() {
    // Given
    draftSelectionRequest.setRound(100);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec un pick à 1")
  void shouldValidateDraftSelectionRequestWithPickOne() {
    // Given
    draftSelectionRequest.setPick(1);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec un pick à 200")
  void shouldValidateDraftSelectionRequestWithPickTwoHundred() {
    // Given
    draftSelectionRequest.setPick(200);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec auto-pick")
  void shouldValidateDraftSelectionRequestWithAutoPick() {
    // Given
    draftSelectionRequest.setIsAutoPick(true);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec un temps de sélection")
  void shouldValidateDraftSelectionRequestWithSelectionTime() {
    // Given
    draftSelectionRequest.setSelectionTime(60000L); // 1 minute

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest sans temps de sélection")
  void shouldValidateDraftSelectionRequestWithoutSelectionTime() {
    // Given
    draftSelectionRequest.setSelectionTime(null);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un temps de sélection négatif")
  void shouldNotValidateDraftSelectionRequestWithNegativeSelectionTime() {
    // Given
    draftSelectionRequest.setSelectionTime(-1000L);

    // When
    boolean isValid = draftSelectionRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le temps de sélection ne peut pas être négatif");
  }

  @Test
  @DisplayName("Devrait vérifier si la sélection est un auto-pick")
  void shouldCheckIfSelectionIsAutoPick() {
    // Given
    draftSelectionRequest.setIsAutoPick(true);

    // When
    boolean isAutoPick = draftSelectionRequest.isAutoPickSelection();

    // Then
    assertThat(isAutoPick).isTrue();
  }

  @Test
  @DisplayName("Devrait vérifier si la sélection n'est pas un auto-pick")
  void shouldCheckIfSelectionIsNotAutoPick() {
    // Given
    draftSelectionRequest.setIsAutoPick(false);

    // When
    boolean isAutoPick = draftSelectionRequest.isAutoPickSelection();

    // Then
    assertThat(isAutoPick).isFalse();
  }

  @Test
  @DisplayName("Devrait vérifier si la sélection a un temps de sélection")
  void shouldCheckIfSelectionHasSelectionTime() {
    // Given
    draftSelectionRequest.setSelectionTime(30000L);

    // When
    boolean hasSelectionTime = draftSelectionRequest.hasSelectionTime();

    // Then
    assertThat(hasSelectionTime).isTrue();
  }

  @Test
  @DisplayName("Devrait vérifier si la sélection n'a pas de temps de sélection")
  void shouldCheckIfSelectionHasNoSelectionTime() {
    // Given
    draftSelectionRequest.setSelectionTime(null);

    // When
    boolean hasSelectionTime = draftSelectionRequest.hasSelectionTime();

    // Then
    assertThat(hasSelectionTime).isFalse();
  }

  @Test
  @DisplayName("Devrait obtenir le temps de sélection en secondes")
  void shouldGetSelectionTimeInSeconds() {
    // Given
    draftSelectionRequest.setSelectionTime(30000L); // 30 secondes

    // When
    int selectionTimeSeconds = draftSelectionRequest.getSelectionTimeSeconds();

    // Then
    assertThat(selectionTimeSeconds).isEqualTo(30);
  }

  @Test
  @DisplayName("Devrait retourner 0 pour le temps de sélection en secondes si null")
  void shouldReturnZeroForSelectionTimeSecondsIfNull() {
    // Given
    draftSelectionRequest.setSelectionTime(null);

    // When
    int selectionTimeSeconds = draftSelectionRequest.getSelectionTimeSeconds();

    // Then
    assertThat(selectionTimeSeconds).isEqualTo(0);
  }

  @Test
  @DisplayName("Devrait obtenir le temps de sélection en minutes")
  void shouldGetSelectionTimeInMinutes() {
    // Given
    draftSelectionRequest.setSelectionTime(120000L); // 2 minutes

    // When
    int selectionTimeMinutes = draftSelectionRequest.getSelectionTimeMinutes();

    // Then
    assertThat(selectionTimeMinutes).isEqualTo(2);
  }

  @Test
  @DisplayName("Devrait créer un DraftSelectionRequest avec le constructeur")
  void shouldCreateDraftSelectionRequestWithConstructor() {
    // When
    DraftSelectionRequest newRequest =
        new DraftSelectionRequest(testGameId, testUserId, testPlayerId, 2, 4, true, 45000L);

    // Then
    assertThat(newRequest.getGameId()).isEqualTo(testGameId);
    assertThat(newRequest.getUserId()).isEqualTo(testUserId);
    assertThat(newRequest.getPlayerId()).isEqualTo(testPlayerId);
    assertThat(newRequest.getRound()).isEqualTo(2);
    assertThat(newRequest.getPick()).isEqualTo(4);
    assertThat(newRequest.getIsAutoPick()).isTrue();
    assertThat(newRequest.getSelectionTime()).isEqualTo(45000L);
  }

  @Test
  @DisplayName("Devrait créer un DraftSelectionRequest avec le constructeur minimal")
  void shouldCreateDraftSelectionRequestWithMinimalConstructor() {
    // When
    DraftSelectionRequest newRequest =
        new DraftSelectionRequest(testGameId, testUserId, testPlayerId);

    // Then
    assertThat(newRequest.getGameId()).isEqualTo(testGameId);
    assertThat(newRequest.getUserId()).isEqualTo(testUserId);
    assertThat(newRequest.getPlayerId()).isEqualTo(testPlayerId);
    assertThat(newRequest.getRound()).isEqualTo(1);
    assertThat(newRequest.getPick()).isEqualTo(1);
    assertThat(newRequest.getIsAutoPick()).isFalse();
    assertThat(newRequest.getSelectionTime()).isNull();
  }

  @Test
  @DisplayName("Devrait comparer deux DraftSelectionRequest égaux")
  void shouldCompareEqualDraftSelectionRequests() {
    // Given
    DraftSelectionRequest request2 = new DraftSelectionRequest();
    request2.setGameId(testGameId);
    request2.setUserId(testUserId);
    request2.setPlayerId(testPlayerId);
    request2.setRound(1);
    request2.setPick(3);
    request2.setIsAutoPick(false);
    request2.setSelectionTime(30000L);

    // When & Then
    assertThat(draftSelectionRequest).isEqualTo(request2);
    assertThat(draftSelectionRequest.hashCode()).isEqualTo(request2.hashCode());
  }

  @Test
  @DisplayName("Devrait comparer deux DraftSelectionRequest différents")
  void shouldCompareDifferentDraftSelectionRequests() {
    // Given
    DraftSelectionRequest request2 = new DraftSelectionRequest();
    request2.setGameId(UUID.randomUUID());
    request2.setUserId(testUserId);
    request2.setPlayerId(testPlayerId);

    // When & Then
    assertThat(draftSelectionRequest).isNotEqualTo(request2);
    assertThat(draftSelectionRequest.hashCode()).isNotEqualTo(request2.hashCode());
  }

  @Test
  @DisplayName("Devrait générer une représentation string correcte")
  void shouldGenerateCorrectStringRepresentation() {
    // When
    String stringRepresentation = draftSelectionRequest.toString();

    // Then
    assertThat(stringRepresentation).contains(testGameId.toString());
    assertThat(stringRepresentation).contains(testUserId.toString());
    assertThat(stringRepresentation).contains(testPlayerId.toString());
    assertThat(stringRepresentation).contains("1");
    assertThat(stringRepresentation).contains("3");
  }

  @Test
  @DisplayName("Devrait effacer les erreurs de validation")
  void shouldClearValidationErrors() {
    // Given
    draftSelectionRequest.setGameId(null);
    draftSelectionRequest.isValid(); // Génère des erreurs

    // When
    draftSelectionRequest.clearValidationErrors();

    // Then
    assertThat(draftSelectionRequest.getValidationErrors()).isEmpty();
  }

  @Test
  @DisplayName("Devrait ajouter une erreur de validation")
  void shouldAddValidationError() {
    // When
    draftSelectionRequest.addValidationError("Erreur de test");

    // Then
    assertThat(draftSelectionRequest.getValidationErrors()).contains("Erreur de test");
  }

  @Test
  @DisplayName("Devrait obtenir le numéro de sélection complet")
  void shouldGetFullSelectionNumber() {
    // When
    String fullSelectionNumber = draftSelectionRequest.getFullSelectionNumber();

    // Then
    assertThat(fullSelectionNumber).isEqualTo("R1P3");
  }

  @Test
  @DisplayName("Devrait obtenir le type de sélection")
  void shouldGetSelectionType() {
    // Given
    draftSelectionRequest.setIsAutoPick(true);

    // When
    String selectionType = draftSelectionRequest.getSelectionType();

    // Then
    assertThat(selectionType).isEqualTo("AUTO_PICK");
  }

  @Test
  @DisplayName("Devrait obtenir le type de sélection pour une sélection manuelle")
  void shouldGetSelectionTypeForManualSelection() {
    // Given
    draftSelectionRequest.setIsAutoPick(false);

    // When
    String selectionType = draftSelectionRequest.getSelectionType();

    // Then
    assertThat(selectionType).isEqualTo("MANUAL");
  }

  @Test
  @DisplayName("Devrait obtenir un résumé de la sélection")
  void shouldGetSelectionSummary() {
    // When
    String selectionSummary = draftSelectionRequest.getSelectionSummary();

    // Then
    assertThat(selectionSummary).contains("R1P3");
    assertThat(selectionSummary).contains("MANUAL");
    assertThat(selectionSummary).contains("30s");
  }

  @Test
  @DisplayName("Devrait obtenir un résumé de la sélection avec auto-pick")
  void shouldGetSelectionSummaryWithAutoPick() {
    // Given
    draftSelectionRequest.setIsAutoPick(true);

    // When
    String selectionSummary = draftSelectionRequest.getSelectionSummary();

    // Then
    assertThat(selectionSummary).contains("AUTO_PICK");
  }
}
