package com.fortnite.pronos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.User;

/** Tests TDD pour DraftStatus */
@DisplayName("Tests TDD - DraftStatus")
class DraftStatusTest {

  private Draft testDraft;
  private Game testGame;
  private User testUser;
  private UUID testUserId;
  private String testUsername;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();
    testUsername = "testuser";

    testUser = new User();
    testUser.setId(testUserId);
    testUser.setUsername(testUsername);

    testGame = new Game();
    testGame.setId(UUID.randomUUID());
    testGame.setName("Test Game");
    testGame.setCreator(testUser);
    testGame.setParticipants(new ArrayList<>());

    testDraft = new Draft();
    testDraft.setId(UUID.randomUUID());
    testDraft.setGame(testGame);
    testDraft.setStatus(Draft.Status.ACTIVE);
    testDraft.setCurrentRound(1);
    testDraft.setCurrentPick(3);
    testDraft.setTotalRounds(5);
    testDraft.setUpdatedAt(LocalDateTime.now().minusMinutes(30));
  }

  @Test
  @DisplayName("Devrait créer un DraftStatus avec tous les champs")
  void shouldCreateDraftStatusWithAllFields() {
    // When
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, true);

    // Then
    assertThat(status.getDraftId()).isEqualTo(testDraft.getId());
    assertThat(status.getGameId()).isEqualTo(testGame.getId());
    assertThat(status.getStatus()).isEqualTo(Draft.Status.ACTIVE);
    assertThat(status.getCurrentRound()).isEqualTo(1);
    assertThat(status.getCurrentPick()).isEqualTo(3);
    assertThat(status.getTotalRounds()).isEqualTo(5);
    assertThat(status.getCurrentParticipantUsername()).isEqualTo(testUsername);
    assertThat(status.getCurrentParticipantId()).isEqualTo(testUserId);
    assertThat(status.getIsCurrentUserTurn()).isTrue();
    assertThat(status.getAutoPickEnabled()).isTrue();
    assertThat(status.getAutoPickDelaySeconds()).isEqualTo(43200);
  }

  @Test
  @DisplayName("Devrait vérifier si le draft est actif")
  void shouldCheckIfDraftIsActive() {
    // Given
    testDraft.setStatus(Draft.Status.ACTIVE);
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, false);

    // When & Then
    assertThat(status.isActive()).isTrue();
    assertThat(status.isPaused()).isFalse();
    assertThat(status.isFinished()).isFalse();
    assertThat(status.isCancelled()).isFalse();
  }

  @Test
  @DisplayName("Devrait vérifier si le draft est en pause")
  void shouldCheckIfDraftIsPaused() {
    // Given
    testDraft.setStatus(Draft.Status.PAUSED);
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, false);

    // When & Then
    assertThat(status.isActive()).isFalse();
    assertThat(status.isPaused()).isTrue();
    assertThat(status.isFinished()).isFalse();
    assertThat(status.isCancelled()).isFalse();
  }

  @Test
  @DisplayName("Devrait vérifier si le draft est terminé")
  void shouldCheckIfDraftIsFinished() {
    // Given
    testDraft.setStatus(Draft.Status.FINISHED);
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, false);

    // When & Then
    assertThat(status.isActive()).isFalse();
    assertThat(status.isPaused()).isFalse();
    assertThat(status.isFinished()).isTrue();
    assertThat(status.isCancelled()).isFalse();
  }

  @Test
  @DisplayName("Devrait vérifier si c'est le tour de l'utilisateur")
  void shouldCheckIfUserTurn() {
    // Given
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, true);

    // When & Then
    assertThat(status.isUserTurn()).isTrue();
  }

  @Test
  @DisplayName("Devrait vérifier si ce n'est pas le tour de l'utilisateur")
  void shouldCheckIfNotUserTurn() {
    // Given
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, false);

    // When & Then
    assertThat(status.isUserTurn()).isFalse();
  }

  @Test
  @DisplayName("Devrait calculer le temps restant correctement")
  void shouldCalculateTimeRemainingCorrectly() {
    // Given
    testDraft.setUpdatedAt(LocalDateTime.now().minusHours(6)); // 6 heures passées
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, false);

    // When & Then
    assertThat(status.getTimeRemainingSeconds()).isGreaterThan(0);
    assertThat(status.getTimeRemainingSeconds()).isLessThanOrEqualTo(21600); // 6 heures max
  }

  @Test
  @DisplayName("Devrait détecter le temps écoulé")
  void shouldDetectTimeExpired() {
    // Given
    testDraft.setUpdatedAt(LocalDateTime.now().minusHours(13)); // Plus de 12 heures
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, false);

    // When & Then
    assertThat(status.isTimeExpired()).isTrue();
    assertThat(status.getTimeRemainingSeconds()).isEqualTo(0);
  }

  @Test
  @DisplayName("Devrait formater le temps restant correctement")
  void shouldFormatTimeRemainingCorrectly() {
    // Given
    testDraft.setUpdatedAt(LocalDateTime.now().minusHours(2).minusMinutes(30)); // 2h30 passées
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, false);

    // When
    String formatted = status.getTimeRemainingFormatted();

    // Then
    assertThat(formatted).matches("\\d{2}:\\d{2}:\\d{2}"); // Format HH:MM:SS
  }

  @Test
  @DisplayName("Devrait retourner le statut lisible")
  void shouldReturnReadableStatus() {
    // Given
    testDraft.setStatus(Draft.Status.ACTIVE);
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, false);

    // When
    String statusLabel = status.getStatusLabel();

    // Then
    assertThat(statusLabel).isEqualTo("En cours");
  }

  @Test
  @DisplayName("Devrait calculer la progression en pourcentage")
  void shouldCalculateProgressPercentage() {
    // Given
    testDraft.setCurrentRound(2);
    testDraft.setCurrentPick(1);
    testDraft.setTotalRounds(5);
    testGame.setParticipants(new ArrayList<>()); // 0 participants pour simplifier
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, false);

    // When
    int progress = status.getProgressPercentage();

    // Then
    assertThat(progress).isGreaterThanOrEqualTo(0);
    assertThat(progress).isLessThanOrEqualTo(100);
  }

  @Test
  @DisplayName("Devrait formater le pick actuel")
  void shouldFormatCurrentPick() {
    // Given
    testDraft.setCurrentRound(2);
    testDraft.setCurrentPick(3);
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, false);

    // When
    String formatted = status.getCurrentPickFormatted();

    // Then
    assertThat(formatted).isEqualTo("R2P3");
  }

  @Test
  @DisplayName("Devrait calculer le nombre de picks restants")
  void shouldCalculateRemainingPicks() {
    // Given
    testDraft.setCurrentRound(2);
    testDraft.setCurrentPick(1);
    testDraft.setTotalRounds(5);
    testGame.setParticipants(new ArrayList<>()); // 0 participants pour simplifier
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, false);

    // When
    int remaining = status.getRemainingPicks();

    // Then
    assertThat(remaining).isGreaterThanOrEqualTo(0);
  }

  @Test
  @DisplayName("Devrait gérer les valeurs nulles")
  void shouldHandleNullValues() {
    // Given
    testDraft.setUpdatedAt(null);
    DraftStatus status = DraftStatus.fromDraft(testDraft, testUsername, testUserId, false);

    // When & Then
    assertThat(status.getTimeRemainingSeconds()).isEqualTo(43200); // Valeur par défaut
    assertThat(status.getNextPickDeadline()).isNotNull();
  }

  @Test
  @DisplayName("Devrait utiliser le builder pattern correctement")
  void shouldUseBuilderPatternCorrectly() {
    // When
    DraftStatus status =
        DraftStatus.builder()
            .draftId(testDraft.getId())
            .gameId(testGame.getId())
            .status(Draft.Status.ACTIVE)
            .currentRound(1)
            .currentPick(1)
            .totalRounds(5)
            .totalParticipants(4)
            .currentParticipantUsername(testUsername)
            .currentParticipantId(testUserId)
            .isCurrentUserTurn(true)
            .autoPickEnabled(true)
            .autoPickDelaySeconds(43200)
            .build();

    // Then
    assertThat(status.getDraftId()).isEqualTo(testDraft.getId());
    assertThat(status.getGameId()).isEqualTo(testGame.getId());
    assertThat(status.getStatus()).isEqualTo(Draft.Status.ACTIVE);
    assertThat(status.getCurrentRound()).isEqualTo(1);
    assertThat(status.getCurrentPick()).isEqualTo(1);
    assertThat(status.getTotalRounds()).isEqualTo(5);
    assertThat(status.getTotalParticipants()).isEqualTo(4);
    assertThat(status.getCurrentParticipantUsername()).isEqualTo(testUsername);
    assertThat(status.getCurrentParticipantId()).isEqualTo(testUserId);
    assertThat(status.getIsCurrentUserTurn()).isTrue();
    assertThat(status.getAutoPickEnabled()).isTrue();
    assertThat(status.getAutoPickDelaySeconds()).isEqualTo(43200);
  }
}
