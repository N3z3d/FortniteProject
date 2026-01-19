package com.fortnite.pronos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.GameLifecycle.TransitionResult;
import com.fortnite.pronos.model.GameStatus;

/** Unit tests for GameLifecycle domain rules. Pure domain tests - no Spring context required. */
@DisplayName("GameLifecycle")
class GameLifecycleTest {

  @Nested
  @DisplayName("canStartDraft")
  class CanStartDraft {

    @Test
    @DisplayName("allows starting draft from CREATING with enough participants")
    void allowsStartingFromCreatingWithEnoughParticipants() {
      TransitionResult result = GameLifecycle.canStartDraft(GameStatus.CREATING, 2);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(GameStatus.DRAFTING);
    }

    @Test
    @DisplayName("rejects starting draft with insufficient participants")
    void rejectsInsufficientParticipants() {
      TransitionResult result = GameLifecycle.canStartDraft(GameStatus.CREATING, 1);

      assertThat(result.allowed()).isFalse();
      assertThat(result.errorMessage()).contains("at least 2 participants");
    }

    @Test
    @DisplayName("rejects starting draft from non-CREATING status")
    void rejectsNonCreatingStatus() {
      TransitionResult result = GameLifecycle.canStartDraft(GameStatus.ACTIVE, 3);

      assertThat(result.allowed()).isFalse();
      assertThat(result.errorMessage()).contains("CREATING");
    }
  }

  @Nested
  @DisplayName("canCompleteDraft")
  class CanCompleteDraft {

    @Test
    @DisplayName("allows completing draft from DRAFTING status")
    void allowsCompletingFromDrafting() {
      TransitionResult result = GameLifecycle.canCompleteDraft(GameStatus.DRAFTING);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(GameStatus.ACTIVE);
    }

    @Test
    @DisplayName("rejects completing draft from non-DRAFTING status")
    void rejectsNonDraftingStatus() {
      TransitionResult result = GameLifecycle.canCompleteDraft(GameStatus.CREATING);

      assertThat(result.allowed()).isFalse();
      assertThat(result.errorMessage()).contains("DRAFTING");
    }
  }

  @Nested
  @DisplayName("canFinishGame")
  class CanFinishGame {

    @Test
    @DisplayName("allows finishing game from ACTIVE status")
    void allowsFinishingFromActive() {
      TransitionResult result = GameLifecycle.canFinishGame(GameStatus.ACTIVE);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(GameStatus.FINISHED);
    }

    @Test
    @DisplayName("rejects finishing game from non-ACTIVE status")
    void rejectsNonActiveStatus() {
      TransitionResult result = GameLifecycle.canFinishGame(GameStatus.DRAFTING);

      assertThat(result.allowed()).isFalse();
      assertThat(result.errorMessage()).contains("ACTIVE");
    }
  }

  @Nested
  @DisplayName("canCancelGame")
  class CanCancelGame {

    @Test
    @DisplayName("allows cancelling game from CREATING status")
    void allowsCancellingFromCreating() {
      TransitionResult result = GameLifecycle.canCancelGame(GameStatus.CREATING);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(GameStatus.CANCELLED);
    }

    @Test
    @DisplayName("allows cancelling game from DRAFTING status")
    void allowsCancellingFromDrafting() {
      TransitionResult result = GameLifecycle.canCancelGame(GameStatus.DRAFTING);

      assertThat(result.allowed()).isTrue();
    }

    @Test
    @DisplayName("rejects cancelling already finished game")
    void rejectsCancellingFinishedGame() {
      TransitionResult result = GameLifecycle.canCancelGame(GameStatus.FINISHED);

      assertThat(result.allowed()).isFalse();
      assertThat(result.errorMessage()).contains("finished");
    }

    @Test
    @DisplayName("rejects cancelling already cancelled game")
    void rejectsCancellingCancelledGame() {
      TransitionResult result = GameLifecycle.canCancelGame(GameStatus.CANCELLED);

      assertThat(result.allowed()).isFalse();
      assertThat(result.errorMessage()).contains("cancelled");
    }
  }
}
