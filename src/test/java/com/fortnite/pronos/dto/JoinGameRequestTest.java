package com.fortnite.pronos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests TDD pour JoinGameRequest */
@DisplayName("Tests TDD - JoinGameRequest")
class JoinGameRequestTest {

  private JoinGameRequest joinGameRequest;
  private UUID testGameId;
  private UUID testUserId;

  @BeforeEach
  void setUp() {
    testGameId = UUID.randomUUID();
    testUserId = UUID.randomUUID();

    joinGameRequest = new JoinGameRequest();
    joinGameRequest.setGameId(testGameId);
    joinGameRequest.setUserId(testUserId);
    joinGameRequest.setInvitationCode("TEST123");
    joinGameRequest.setJoinAsSpectator(false);
  }

  @Test
  @DisplayName("Devrait créer un JoinGameRequest avec tous les champs")
  void shouldCreateJoinGameRequestWithAllFields() {
    // Then
    assertThat(joinGameRequest.getGameId()).isEqualTo(testGameId);
    assertThat(joinGameRequest.getUserId()).isEqualTo(testUserId);
    assertThat(joinGameRequest.getInvitationCode()).isEqualTo("TEST123");
    assertThat(joinGameRequest.getJoinAsSpectator()).isFalse();
  }

  @Test
  @DisplayName("Devrait mettre à jour les champs du JoinGameRequest")
  void shouldUpdateJoinGameRequestFields() {
    // Given
    UUID newGameId = UUID.randomUUID();
    UUID newUserId = UUID.randomUUID();
    String newInvitationCode = "NEW456";
    boolean newJoinAsSpectator = true;

    // When
    joinGameRequest.setGameId(newGameId);
    joinGameRequest.setUserId(newUserId);
    joinGameRequest.setInvitationCode(newInvitationCode);
    joinGameRequest.setJoinAsSpectator(newJoinAsSpectator);

    // Then
    assertThat(joinGameRequest.getGameId()).isEqualTo(newGameId);
    assertThat(joinGameRequest.getUserId()).isEqualTo(newUserId);
    assertThat(joinGameRequest.getInvitationCode()).isEqualTo(newInvitationCode);
    assertThat(joinGameRequest.getJoinAsSpectator()).isEqualTo(newJoinAsSpectator);
  }

  @Test
  @DisplayName("Devrait valider un JoinGameRequest avec des valeurs correctes")
  void shouldValidateJoinGameRequestWithCorrectValues() {
    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
    assertThat(joinGameRequest.getValidationErrors()).isEmpty();
  }

  @Test
  @DisplayName("Ne devrait pas valider un JoinGameRequest avec un gameId null")
  void shouldNotValidateJoinGameRequestWithNullGameId() {
    // Given
    joinGameRequest.setGameId(null);

    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(joinGameRequest.getValidationErrors()).contains("L'ID de la game est requis");
  }

  @Test
  @DisplayName("Ne devrait pas valider un JoinGameRequest avec un userId null")
  void shouldNotValidateJoinGameRequestWithNullUserId() {
    // Given
    joinGameRequest.setUserId(null);

    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(joinGameRequest.getValidationErrors()).contains("L'ID de l'utilisateur est requis");
  }

  @Test
  @DisplayName("Devrait valider un JoinGameRequest sans code d'invitation")
  void shouldValidateJoinGameRequestWithoutInvitationCode() {
    // Given
    joinGameRequest.setInvitationCode(null);

    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un JoinGameRequest avec un code d'invitation vide")
  void shouldValidateJoinGameRequestWithEmptyInvitationCode() {
    // Given
    joinGameRequest.setInvitationCode("");

    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Ne devrait pas valider un JoinGameRequest avec un code d'invitation trop court")
  void shouldNotValidateJoinGameRequestWithTooShortInvitationCode() {
    // Given
    joinGameRequest.setInvitationCode("AB");

    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(joinGameRequest.getValidationErrors())
        .contains("Le code d'invitation doit contenir entre 3 et 20 caractères");
  }

  @Test
  @DisplayName("Ne devrait pas valider un JoinGameRequest avec un code d'invitation trop long")
  void shouldNotValidateJoinGameRequestWithTooLongInvitationCode() {
    // Given
    joinGameRequest.setInvitationCode("A".repeat(21));

    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(joinGameRequest.getValidationErrors())
        .contains("Le code d'invitation doit contenir entre 3 et 20 caractères");
  }

  @Test
  @DisplayName("Devrait valider un JoinGameRequest avec un code d'invitation de longueur minimale")
  void shouldValidateJoinGameRequestWithMinLengthInvitationCode() {
    // Given
    joinGameRequest.setInvitationCode("ABC");

    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un JoinGameRequest avec un code d'invitation de longueur maximale")
  void shouldValidateJoinGameRequestWithMaxLengthInvitationCode() {
    // Given
    joinGameRequest.setInvitationCode("A".repeat(20));

    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName(
      "Ne devrait pas valider un JoinGameRequest avec un code d'invitation contenant des caractères invalides")
  void shouldNotValidateJoinGameRequestWithInvalidInvitationCode() {
    // Given
    joinGameRequest.setInvitationCode("TEST@123");

    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isFalse();
    assertThat(joinGameRequest.getValidationErrors())
        .contains("Le code d'invitation ne peut contenir que des lettres, chiffres et tirets");
  }

  @Test
  @DisplayName("Devrait valider un JoinGameRequest avec un code d'invitation valide")
  void shouldValidateJoinGameRequestWithValidInvitationCode() {
    // Given
    joinGameRequest.setInvitationCode("TEST-123");

    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un JoinGameRequest pour rejoindre comme spectateur")
  void shouldValidateJoinGameRequestAsSpectator() {
    // Given
    joinGameRequest.setJoinAsSpectator(true);

    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait valider un JoinGameRequest pour rejoindre comme participant")
  void shouldValidateJoinGameRequestAsParticipant() {
    // Given
    joinGameRequest.setJoinAsSpectator(false);

    // When
    boolean isValid = joinGameRequest.isValid();

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait vérifier si la requête a un code d'invitation")
  void shouldCheckIfRequestHasInvitationCode() {
    // Given
    joinGameRequest.setInvitationCode("TEST123");

    // When
    boolean hasInvitationCode = joinGameRequest.hasInvitationCode();

    // Then
    assertThat(hasInvitationCode).isTrue();
  }

  @Test
  @DisplayName("Devrait vérifier si la requête n'a pas de code d'invitation")
  void shouldCheckIfRequestHasNoInvitationCode() {
    // Given
    joinGameRequest.setInvitationCode(null);

    // When
    boolean hasInvitationCode = joinGameRequest.hasInvitationCode();

    // Then
    assertThat(hasInvitationCode).isFalse();
  }

  @Test
  @DisplayName("Devrait vérifier si la requête n'a pas de code d'invitation (vide)")
  void shouldCheckIfRequestHasNoInvitationCodeEmpty() {
    // Given
    joinGameRequest.setInvitationCode("");

    // When
    boolean hasInvitationCode = joinGameRequest.hasInvitationCode();

    // Then
    assertThat(hasInvitationCode).isFalse();
  }

  @Test
  @DisplayName("Devrait vérifier si la requête est pour rejoindre comme spectateur")
  void shouldCheckIfRequestIsForSpectator() {
    // Given
    joinGameRequest.setJoinAsSpectator(true);

    // When
    boolean isSpectator = joinGameRequest.isSpectator();

    // Then
    assertThat(isSpectator).isTrue();
  }

  @Test
  @DisplayName("Devrait vérifier si la requête est pour rejoindre comme participant")
  void shouldCheckIfRequestIsForParticipant() {
    // Given
    joinGameRequest.setJoinAsSpectator(false);

    // When
    boolean isSpectator = joinGameRequest.isSpectator();

    // Then
    assertThat(isSpectator).isFalse();
  }

  @Test
  @DisplayName("Devrait créer un JoinGameRequest avec le constructeur")
  void shouldCreateJoinGameRequestWithConstructor() {
    // When
    JoinGameRequest newRequest = new JoinGameRequest(testGameId, testUserId, "NEW456", true);

    // Then
    assertThat(newRequest.getGameId()).isEqualTo(testGameId);
    assertThat(newRequest.getUserId()).isEqualTo(testUserId);
    assertThat(newRequest.getInvitationCode()).isEqualTo("NEW456");
    assertThat(newRequest.getJoinAsSpectator()).isTrue();
  }

  @Test
  @DisplayName("Devrait créer un JoinGameRequest avec le constructeur minimal")
  void shouldCreateJoinGameRequestWithMinimalConstructor() {
    // When
    JoinGameRequest newRequest = new JoinGameRequest(testGameId, testUserId);

    // Then
    assertThat(newRequest.getGameId()).isEqualTo(testGameId);
    assertThat(newRequest.getUserId()).isEqualTo(testUserId);
    assertThat(newRequest.getInvitationCode()).isNull();
    assertThat(newRequest.getJoinAsSpectator()).isFalse();
  }

  @Test
  @DisplayName("Devrait comparer deux JoinGameRequest égaux")
  void shouldCompareEqualJoinGameRequests() {
    // Given
    JoinGameRequest request2 = new JoinGameRequest();
    request2.setGameId(testGameId);
    request2.setUserId(testUserId);
    request2.setInvitationCode("TEST123");
    request2.setJoinAsSpectator(false);

    // When & Then
    assertThat(joinGameRequest).isEqualTo(request2);
    assertThat(joinGameRequest.hashCode()).isEqualTo(request2.hashCode());
  }

  @Test
  @DisplayName("Devrait comparer deux JoinGameRequest différents")
  void shouldCompareDifferentJoinGameRequests() {
    // Given
    JoinGameRequest request2 = new JoinGameRequest();
    request2.setGameId(UUID.randomUUID());
    request2.setUserId(testUserId);

    // When & Then
    assertThat(joinGameRequest).isNotEqualTo(request2);
    assertThat(joinGameRequest.hashCode()).isNotEqualTo(request2.hashCode());
  }

  @Test
  @DisplayName("Devrait générer une représentation string correcte")
  void shouldGenerateCorrectStringRepresentation() {
    // When
    String stringRepresentation = joinGameRequest.toString();

    // Then
    assertThat(stringRepresentation).contains(testGameId.toString());
    assertThat(stringRepresentation).contains(testUserId.toString());
    assertThat(stringRepresentation).contains("TEST123");
  }

  @Test
  @DisplayName("Devrait effacer les erreurs de validation")
  void shouldClearValidationErrors() {
    // Given
    joinGameRequest.setGameId(null);
    joinGameRequest.isValid(); // Génère des erreurs

    // When
    joinGameRequest.clearValidationErrors();

    // Then
    assertThat(joinGameRequest.getValidationErrors()).isEmpty();
  }

  @Test
  @DisplayName("Devrait ajouter une erreur de validation")
  void shouldAddValidationError() {
    // When
    joinGameRequest.addValidationError("Erreur de test");

    // Then
    assertThat(joinGameRequest.getValidationErrors()).contains("Erreur de test");
  }

  @Test
  @DisplayName("Devrait obtenir le code d'invitation en majuscules")
  void shouldGetInvitationCodeInUpperCase() {
    // Given
    joinGameRequest.setInvitationCode("test123");

    // When
    String upperCaseCode = joinGameRequest.getInvitationCodeUpperCase();

    // Then
    assertThat(upperCaseCode).isEqualTo("TEST123");
  }

  @Test
  @DisplayName("Devrait retourner null pour le code d'invitation en majuscules si null")
  void shouldReturnNullForUpperCaseInvitationCodeIfNull() {
    // Given
    joinGameRequest.setInvitationCode(null);

    // When
    String upperCaseCode = joinGameRequest.getInvitationCodeUpperCase();

    // Then
    assertThat(upperCaseCode).isNull();
  }

  @Test
  @DisplayName("Devrait obtenir le type de participation")
  void shouldGetParticipationType() {
    // Given
    joinGameRequest.setJoinAsSpectator(true);

    // When
    String participationType = joinGameRequest.getParticipationType();

    // Then
    assertThat(participationType).isEqualTo("SPECTATOR");
  }

  @Test
  @DisplayName("Devrait obtenir le type de participation pour un participant")
  void shouldGetParticipationTypeForParticipant() {
    // Given
    joinGameRequest.setJoinAsSpectator(false);

    // When
    String participationType = joinGameRequest.getParticipationType();

    // Then
    assertThat(participationType).isEqualTo("PARTICIPANT");
  }
}
