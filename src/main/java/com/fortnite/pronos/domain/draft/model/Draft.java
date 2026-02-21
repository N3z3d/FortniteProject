package com.fortnite.pronos.domain.draft.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain model representing a draft aggregate.
 *
 * <p>Contains business rules without JPA/Spring dependencies.
 */
@SuppressWarnings({"java:S107"})
public final class Draft {

  private UUID id;
  private UUID gameId;
  private DraftStatus status;
  private int currentRound;
  private int currentPick;
  private int totalRounds;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;

  /** Business constructor for creating a new draft. */
  public Draft(UUID gameId, int totalRounds) {
    validateCreation(gameId, totalRounds);
    this.id = UUID.randomUUID();
    this.gameId = gameId;
    this.status = DraftStatus.PENDING;
    this.currentRound = 1;
    this.currentPick = 1;
    this.totalRounds = totalRounds;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
  }

  /** Reconstitution factory for persistence mapping. */
  public static Draft restore(
      UUID id,
      UUID gameId,
      DraftStatus status,
      int currentRound,
      int currentPick,
      int totalRounds,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime startedAt,
      LocalDateTime finishedAt) {
    Draft draft = new Draft();
    draft.id = id;
    draft.gameId = gameId;
    draft.status = status != null ? status : DraftStatus.PENDING;
    draft.currentRound = currentRound > 0 ? currentRound : 1;
    draft.currentPick = currentPick > 0 ? currentPick : 1;
    draft.totalRounds = totalRounds > 0 ? totalRounds : 1;
    draft.createdAt = createdAt;
    draft.updatedAt = updatedAt;
    draft.startedAt = startedAt;
    draft.finishedAt = finishedAt;
    return draft;
  }

  private Draft() {}

  // ===============================
  // BUSINESS METHODS
  // ===============================

  public boolean start() {
    if (status != DraftStatus.PENDING) {
      return false;
    }
    status = DraftStatus.ACTIVE;
    startedAt = LocalDateTime.now();
    touch();
    return true;
  }

  public boolean pause() {
    if (status != DraftStatus.ACTIVE && status != DraftStatus.IN_PROGRESS) {
      return false;
    }
    status = DraftStatus.PAUSED;
    touch();
    return true;
  }

  public boolean resume() {
    if (status != DraftStatus.PAUSED) {
      return false;
    }
    status = DraftStatus.ACTIVE;
    touch();
    return true;
  }

  public boolean finish() {
    if (!isComplete()) {
      return false;
    }
    status = DraftStatus.FINISHED;
    finishedAt = LocalDateTime.now();
    touch();
    return true;
  }

  public void cancel() {
    status = DraftStatus.CANCELLED;
    touch();
  }

  public void nextPick(int participantCount) {
    if (participantCount <= 0) {
      throw new IllegalArgumentException("Participant count must be positive");
    }

    currentPick++;
    if (currentPick > participantCount) {
      currentRound++;
      currentPick = 1;
      if (currentRound > totalRounds) {
        status = DraftStatus.FINISHED;
        finishedAt = LocalDateTime.now();
      }
    }
    touch();
  }

  public boolean isComplete() {
    return status == DraftStatus.FINISHED || currentRound > totalRounds;
  }

  public boolean isActive() {
    return status == DraftStatus.ACTIVE || status == DraftStatus.IN_PROGRESS;
  }

  public boolean isPending() {
    return status == DraftStatus.PENDING;
  }

  // ===============================
  // GETTERS
  // ===============================

  public UUID getId() {
    return id;
  }

  public UUID getGameId() {
    return gameId;
  }

  public DraftStatus getStatus() {
    return status;
  }

  public int getCurrentRound() {
    return currentRound;
  }

  public int getCurrentPick() {
    return currentPick;
  }

  public int getTotalRounds() {
    return totalRounds;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  public LocalDateTime getFinishedAt() {
    return finishedAt;
  }

  // ===============================
  // EQUALS / HASHCODE
  // ===============================

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Draft draft)) {
      return false;
    }
    return Objects.equals(id, draft.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  // ===============================
  // PRIVATE HELPERS
  // ===============================

  private void touch() {
    updatedAt = LocalDateTime.now();
  }

  private void validateCreation(UUID gameId, int totalRounds) {
    if (gameId == null) {
      throw new IllegalArgumentException("Game ID cannot be null");
    }
    if (totalRounds <= 0) {
      throw new IllegalArgumentException("Total rounds must be positive");
    }
  }
}
