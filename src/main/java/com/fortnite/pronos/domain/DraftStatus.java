package com.fortnite.pronos.domain;

import com.fortnite.pronos.model.Draft;

/** Domain logic for draft status transitions. Pure domain rules without JPA dependencies. */
public final class DraftStatus {

  private DraftStatus() {}

  /**
   * Validates if a draft can be started.
   *
   * @param currentStatus current draft status
   * @return validation result
   */
  public static TransitionResult canStart(Draft.Status currentStatus) {
    if (currentStatus != Draft.Status.PENDING) {
      return TransitionResult.failure("Draft can only be started from PENDING status");
    }
    return TransitionResult.success(Draft.Status.ACTIVE);
  }

  /**
   * Validates if a draft can be paused.
   *
   * @param currentStatus current draft status
   * @return validation result
   */
  public static TransitionResult canPause(Draft.Status currentStatus) {
    if (currentStatus != Draft.Status.ACTIVE && currentStatus != Draft.Status.IN_PROGRESS) {
      return TransitionResult.failure("Draft can only be paused when active or in progress");
    }
    return TransitionResult.success(Draft.Status.PAUSED);
  }

  /**
   * Validates if a draft can be resumed.
   *
   * @param currentStatus current draft status
   * @return validation result
   */
  public static TransitionResult canResume(Draft.Status currentStatus) {
    if (currentStatus != Draft.Status.PAUSED) {
      return TransitionResult.failure("Draft can only be resumed from PAUSED status");
    }
    return TransitionResult.success(Draft.Status.ACTIVE);
  }

  /**
   * Validates if a draft can be finished.
   *
   * @param currentStatus current draft status
   * @param isDraftComplete whether all picks have been made
   * @return validation result
   */
  public static TransitionResult canFinish(Draft.Status currentStatus, boolean isDraftComplete) {
    if (currentStatus != Draft.Status.ACTIVE && currentStatus != Draft.Status.IN_PROGRESS) {
      return TransitionResult.failure("Draft can only be finished when active or in progress");
    }
    if (!isDraftComplete) {
      return TransitionResult.failure("Draft cannot be finished - picks remaining");
    }
    return TransitionResult.success(Draft.Status.FINISHED);
  }

  /**
   * Validates if a draft can be cancelled.
   *
   * @param currentStatus current draft status
   * @return validation result
   */
  public static TransitionResult canCancel(Draft.Status currentStatus) {
    if (currentStatus == Draft.Status.FINISHED || currentStatus == Draft.Status.CANCELLED) {
      return TransitionResult.failure("Cannot cancel a finished or already cancelled draft");
    }
    return TransitionResult.success(Draft.Status.CANCELLED);
  }

  /**
   * Checks if a draft status is terminal (no more transitions allowed).
   *
   * @param status status to check
   * @return true if terminal
   */
  public static boolean isTerminal(Draft.Status status) {
    return status == Draft.Status.FINISHED || status == Draft.Status.CANCELLED;
  }

  /**
   * Checks if a draft is in an active state (picks can be made).
   *
   * @param status status to check
   * @return true if picks can be made
   */
  public static boolean allowsPicks(Draft.Status status) {
    return status == Draft.Status.ACTIVE || status == Draft.Status.IN_PROGRESS;
  }

  /** Result of a status transition validation. */
  public record TransitionResult(boolean allowed, Draft.Status newStatus, String errorMessage) {

    public static TransitionResult success(Draft.Status newStatus) {
      return new TransitionResult(true, newStatus, null);
    }

    public static TransitionResult failure(String errorMessage) {
      return new TransitionResult(false, null, errorMessage);
    }
  }
}
