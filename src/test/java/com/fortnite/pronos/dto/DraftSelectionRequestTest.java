package com.fortnite.pronos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    draftSelectionRequest.setSelectionTime(30000L);
  }

  @Test
  @DisplayName("Devrait créer un DraftSelectionRequest avec tous les champs")
  void shouldCreateDraftSelectionRequestWithAllFields() {
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
    UUID newPlayerId = UUID.randomUUID();
    int newRound = 2;
    int newPick = 5;
    boolean newIsAutoPick = true;
    Long newSelectionTime = 45000L;

    draftSelectionRequest.setPlayerId(newPlayerId);
    draftSelectionRequest.setRound(newRound);
    draftSelectionRequest.setPick(newPick);
    draftSelectionRequest.setIsAutoPick(newIsAutoPick);
    draftSelectionRequest.setSelectionTime(newSelectionTime);

    assertThat(draftSelectionRequest.getPlayerId()).isEqualTo(newPlayerId);
    assertThat(draftSelectionRequest.getRound()).isEqualTo(newRound);
    assertThat(draftSelectionRequest.getPick()).isEqualTo(newPick);
    assertThat(draftSelectionRequest.getIsAutoPick()).isEqualTo(newIsAutoPick);
    assertThat(draftSelectionRequest.getSelectionTime()).isEqualTo(newSelectionTime);
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec des valeurs correctes")
  void shouldValidateDraftSelectionRequestWithCorrectValues() {
    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isTrue();
    assertThat(draftSelectionRequest.getValidationErrors()).isEmpty();
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un gameId null")
  void shouldNotValidateDraftSelectionRequestWithNullGameId() {
    draftSelectionRequest.setGameId(null);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors()).contains("L'ID de la game est requis");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un userId null")
  void shouldNotValidateDraftSelectionRequestWithNullUserId() {
    draftSelectionRequest.setUserId(null);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("L'ID de l'utilisateur est requis");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un playerId null")
  void shouldNotValidateDraftSelectionRequestWithNullPlayerId() {
    draftSelectionRequest.setPlayerId(null);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors()).contains("L'ID du joueur est requis");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un round négatif")
  void shouldNotValidateDraftSelectionRequestWithNegativeRound() {
    draftSelectionRequest.setRound(-1);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le numéro de round doit être positif");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un round null")
  void shouldNotValidateDraftSelectionRequestWithNullRound() {
    draftSelectionRequest.setRound(null);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le numéro de round est requis");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un pick négatif")
  void shouldNotValidateDraftSelectionRequestWithNegativePick() {
    draftSelectionRequest.setPick(-1);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le numéro de pick doit être positif");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un pick null")
  void shouldNotValidateDraftSelectionRequestWithNullPick() {
    draftSelectionRequest.setPick(null);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le numéro de pick est requis");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un round trop grand")
  void shouldNotValidateDraftSelectionRequestWithTooLargeRound() {
    draftSelectionRequest.setRound(101);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le numéro de round ne peut pas dépasser 100");
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un pick trop grand")
  void shouldNotValidateDraftSelectionRequestWithTooLargePick() {
    draftSelectionRequest.setPick(201);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le numéro de pick ne peut pas dépasser 200");
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec un round à 1")
  void shouldValidateDraftSelectionRequestWithRoundOne() {
    draftSelectionRequest.setRound(1);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec un round à 100")
  void shouldValidateDraftSelectionRequestWithRoundHundred() {
    draftSelectionRequest.setRound(100);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec un pick à 1")
  void shouldValidateDraftSelectionRequestWithPickOne() {
    draftSelectionRequest.setPick(1);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec un pick à 200")
  void shouldValidateDraftSelectionRequestWithPickTwoHundred() {
    draftSelectionRequest.setPick(200);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec auto-pick")
  void shouldValidateDraftSelectionRequestWithAutoPick() {
    draftSelectionRequest.setIsAutoPick(true);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest avec un temps de sélection")
  void shouldValidateDraftSelectionRequestWithSelectionTime() {
    draftSelectionRequest.setSelectionTime(60000L);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un DraftSelectionRequest sans temps de sélection")
  void shouldValidateDraftSelectionRequestWithoutSelectionTime() {
    draftSelectionRequest.setSelectionTime(null);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Ne devrait pas valider un DraftSelectionRequest avec un temps de sélection négatif")
  void shouldNotValidateDraftSelectionRequestWithNegativeSelectionTime() {
    draftSelectionRequest.setSelectionTime(-1000L);

    boolean isValid = draftSelectionRequest.isValid();

    assertThat(isValid).isFalse();
    assertThat(draftSelectionRequest.getValidationErrors())
        .contains("Le temps de sélection ne peut pas être négatif");
  }

  @Test
  @DisplayName("Devrait vérifier si la sélection est un auto-pick")
  void shouldCheckIfSelectionIsAutoPick() {
    draftSelectionRequest.setIsAutoPick(true);

    boolean isAutoPick = draftSelectionRequest.isAutoPickSelection();

    assertThat(isAutoPick).isTrue();
  }

  @Test
  @DisplayName("Devrait vérifier si la sélection n'est pas un auto-pick")
  void shouldCheckIfSelectionIsNotAutoPick() {
    draftSelectionRequest.setIsAutoPick(false);

    boolean isAutoPick = draftSelectionRequest.isAutoPickSelection();

    assertThat(isAutoPick).isFalse();
  }

  @Test
  @DisplayName("Devrait vérifier si la sélection a un temps de sélection")
  void shouldCheckIfSelectionHasSelectionTime() {
    draftSelectionRequest.setSelectionTime(30000L);

    boolean hasSelectionTime = draftSelectionRequest.hasSelectionTime();

    assertThat(hasSelectionTime).isTrue();
  }

  @Test
  @DisplayName("Devrait vérifier si la sélection n'a pas de temps de sélection")
  void shouldCheckIfSelectionHasNoSelectionTime() {
    draftSelectionRequest.setSelectionTime(null);

    boolean hasSelectionTime = draftSelectionRequest.hasSelectionTime();

    assertThat(hasSelectionTime).isFalse();
  }

  @Test
  @DisplayName("Devrait obtenir le temps de sélection en secondes")
  void shouldGetSelectionTimeInSeconds() {
    draftSelectionRequest.setSelectionTime(30000L);

    int selectionTimeSeconds = draftSelectionRequest.getSelectionTimeSeconds();

    assertThat(selectionTimeSeconds).isEqualTo(30);
  }

  @Test
  @DisplayName("Devrait retourner 0 pour le temps de sélection en secondes si null")
  void shouldReturnZeroForSelectionTimeSecondsIfNull() {
    draftSelectionRequest.setSelectionTime(null);

    int selectionTimeSeconds = draftSelectionRequest.getSelectionTimeSeconds();

    assertThat(selectionTimeSeconds).isEqualTo(0);
  }

  @Test
  @DisplayName("Devrait obtenir le temps de sélection en minutes")
  void shouldGetSelectionTimeInMinutes() {
    draftSelectionRequest.setSelectionTime(120000L);

    int selectionTimeMinutes = draftSelectionRequest.getSelectionTimeMinutes();

    assertThat(selectionTimeMinutes).isEqualTo(2);
  }

  @Test
  @DisplayName("Devrait créer un DraftSelectionRequest avec le constructeur")
  void shouldCreateDraftSelectionRequestWithConstructor() {
    DraftSelectionRequest newRequest =
        new DraftSelectionRequest(testGameId, testUserId, testPlayerId, 2, 4, true, 45000L);

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
    DraftSelectionRequest newRequest =
        new DraftSelectionRequest(testGameId, testUserId, testPlayerId);

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
    DraftSelectionRequest request2 = new DraftSelectionRequest();
    request2.setGameId(testGameId);
    request2.setUserId(testUserId);
    request2.setPlayerId(testPlayerId);
    request2.setRound(1);
    request2.setPick(3);
    request2.setIsAutoPick(false);
    request2.setSelectionTime(30000L);

    assertThat(draftSelectionRequest).isEqualTo(request2);
    assertThat(draftSelectionRequest.hashCode()).isEqualTo(request2.hashCode());
  }

  @Test
  @DisplayName("Devrait comparer deux DraftSelectionRequest différents")
  void shouldCompareDifferentDraftSelectionRequests() {
    DraftSelectionRequest request2 = new DraftSelectionRequest();
    request2.setGameId(UUID.randomUUID());
    request2.setUserId(testUserId);
    request2.setPlayerId(testPlayerId);

    assertThat(draftSelectionRequest).isNotEqualTo(request2);
    assertThat(draftSelectionRequest.hashCode()).isNotEqualTo(request2.hashCode());
  }

  @Test
  @DisplayName("Devrait générer une représentation string correcte")
  void shouldGenerateCorrectStringRepresentation() {
    String stringRepresentation = draftSelectionRequest.toString();

    assertThat(stringRepresentation).contains(testGameId.toString());
    assertThat(stringRepresentation).contains(testUserId.toString());
    assertThat(stringRepresentation).contains(testPlayerId.toString());
    assertThat(stringRepresentation).contains("1");
    assertThat(stringRepresentation).contains("3");
  }

  @Test
  @DisplayName("Devrait effacer les erreurs de validation")
  void shouldClearValidationErrors() {
    draftSelectionRequest.setGameId(null);
    draftSelectionRequest.isValid();

    draftSelectionRequest.clearValidationErrors();

    assertThat(draftSelectionRequest.getValidationErrors()).isEmpty();
  }

  @Test
  @DisplayName("Devrait ajouter une erreur de validation")
  void shouldAddValidationError() {
    draftSelectionRequest.addValidationError("Erreur de test");

    assertThat(draftSelectionRequest.getValidationErrors()).contains("Erreur de test");
  }

  @Test
  @DisplayName("Devrait obtenir le numéro de sélection complet")
  void shouldGetFullSelectionNumber() {
    String fullSelectionNumber = draftSelectionRequest.getFullSelectionNumber();

    assertThat(fullSelectionNumber).isEqualTo("R1P3");
  }

  @Test
  @DisplayName("Devrait obtenir le type de sélection")
  void shouldGetSelectionType() {
    draftSelectionRequest.setIsAutoPick(true);

    String selectionType = draftSelectionRequest.getSelectionType();

    assertThat(selectionType).isEqualTo("AUTO_PICK");
  }

  @Test
  @DisplayName("Devrait obtenir le type de sélection pour une sélection manuelle")
  void shouldGetSelectionTypeForManualSelection() {
    draftSelectionRequest.setIsAutoPick(false);

    String selectionType = draftSelectionRequest.getSelectionType();

    assertThat(selectionType).isEqualTo("MANUAL");
  }

  @Test
  @DisplayName("Devrait obtenir un résumé de la sélection")
  void shouldGetSelectionSummary() {
    String selectionSummary = draftSelectionRequest.getSelectionSummary();

    assertThat(selectionSummary).contains("R1P3");
    assertThat(selectionSummary).contains("MANUAL");
    assertThat(selectionSummary).contains("30s");
  }

  @Test
  @DisplayName("Devrait obtenir un résumé de la sélection avec auto-pick")
  void shouldGetSelectionSummaryWithAutoPick() {
    draftSelectionRequest.setIsAutoPick(true);

    String selectionSummary = draftSelectionRequest.getSelectionSummary();

    assertThat(selectionSummary).contains("AUTO_PICK");
  }
}
