package com.fortnite.pronos.domain;

import com.fortnite.pronos.model.GameStatus;

/**
 * Domain service encapsulating Game state transition rules. Pure domain logic without JPA
 * dependencies.
 */
public final class GameLifecycle {

  private GameLifecycle() {}

  /** Minimum participants required to start a draft. */
  public static final int MIN_PARTICIPANTS_TO_START = 2;

  /**
   * Validates if starting a draft is allowed.
   *
   * @param currentStatus current game status
   * @param totalParticipants total participant count including creator
   * @return validation result
   */
  public static TransitionResult canStartDraft(GameStatus currentStatus, int totalParticipants) {
    if (currentStatus != GameStatus.CREATING) {
      return TransitionResult.failure("Can only start draft from CREATING status");
    }
    if (totalParticipants < MIN_PARTICIPANTS_TO_START) {
      return TransitionResult.failure(
          "Need at least " + MIN_PARTICIPANTS_TO_START + " participants to start draft");
    }
    return TransitionResult.success(GameStatus.DRAFTING);
  }

  /**
   * Validates if completing a draft is allowed.
   *
   * @param currentStatus current game status
   * @return validation result
   */
  public static TransitionResult canCompleteDraft(GameStatus currentStatus) {
    if (currentStatus != GameStatus.DRAFTING) {
      return TransitionResult.failure("Can only complete draft from DRAFTING status");
    }
    return TransitionResult.success(GameStatus.ACTIVE);
  }

  /**
   * Validates if finishing a game is allowed.
   *
   * @param currentStatus current game status
   * @return validation result
   */
  public static TransitionResult canFinishGame(GameStatus currentStatus) {
    if (currentStatus != GameStatus.ACTIVE) {
      return TransitionResult.failure("Can only finish game from ACTIVE status");
    }
    return TransitionResult.success(GameStatus.FINISHED);
  }

  /**
   * Validates if cancelling a game is allowed.
   *
   * @param currentStatus current game status
   * @return validation result
   */
  public static TransitionResult canCancelGame(GameStatus currentStatus) {
    if (currentStatus == GameStatus.FINISHED || currentStatus == GameStatus.CANCELLED) {
      return TransitionResult.failure("Cannot cancel a finished or already cancelled game");
    }
    return TransitionResult.success(GameStatus.CANCELLED);
  }

  /** Result of a state transition validation. */
  public record TransitionResult(boolean allowed, GameStatus newStatus, String errorMessage) {

    public static TransitionResult success(GameStatus newStatus) {
      return new TransitionResult(true, newStatus, null);
    }

    public static TransitionResult failure(String errorMessage) {
      return new TransitionResult(false, null, errorMessage);
    }
  }
}
