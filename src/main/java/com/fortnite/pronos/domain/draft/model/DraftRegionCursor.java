package com.fortnite.pronos.domain.draft.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain aggregate representing the snake draft cursor for a single region.
 *
 * <p>Each region has an independent snake order, pick position, and turn start timestamp.
 */
public final class DraftRegionCursor {

  private final UUID draftId;
  private final String region;
  private final int currentRound;
  private final int currentPick;
  private final List<UUID> snakeOrder;
  private final Instant turnStartedAt;

  /** Business constructor for a brand-new cursor (round 1, pick 1). */
  public DraftRegionCursor(UUID draftId, String region, List<UUID> snakeOrder) {
    this(draftId, region, 1, 1, snakeOrder, Instant.now());
  }

  private DraftRegionCursor(
      UUID draftId,
      String region,
      int currentRound,
      int currentPick,
      List<UUID> snakeOrder,
      Instant turnStartedAt) {
    Objects.requireNonNull(draftId, "draftId cannot be null");
    Objects.requireNonNull(region, "region cannot be null");
    Objects.requireNonNull(turnStartedAt, "turnStartedAt cannot be null");
    if (snakeOrder == null || snakeOrder.isEmpty()) {
      throw new IllegalArgumentException("snakeOrder cannot be null or empty");
    }
    if (currentRound < 1) throw new IllegalArgumentException("currentRound must be >= 1");
    if (currentPick < 1 || currentPick > snakeOrder.size()) {
      throw new IllegalArgumentException("currentPick out of range");
    }
    this.draftId = draftId;
    this.region = region;
    this.currentRound = currentRound;
    this.currentPick = currentPick;
    this.snakeOrder = Collections.unmodifiableList(new ArrayList<>(snakeOrder));
    this.turnStartedAt = turnStartedAt;
  }

  /** Reconstitution factory for persistence mapping only. */
  public static DraftRegionCursor restore(
      UUID draftId, String region, int currentRound, int currentPick, List<UUID> snakeOrder) {
    return restore(draftId, region, currentRound, currentPick, snakeOrder, Instant.now());
  }

  /** Reconstitution factory for persistence mapping only. */
  public static DraftRegionCursor restore(
      UUID draftId,
      String region,
      int currentRound,
      int currentPick,
      List<UUID> snakeOrder,
      Instant turnStartedAt) {
    return new DraftRegionCursor(
        draftId, region, currentRound, currentPick, snakeOrder, turnStartedAt);
  }

  // ===== BUSINESS METHODS =====

  /**
   * Advances the cursor to the next pick. When all participants in a round have picked, moves to
   * the next round (with reversed direction).
   */
  public DraftRegionCursor advance() {
    int nextPick = currentPick + 1;
    int nextRound = currentRound;
    if (nextPick > snakeOrder.size()) {
      nextPick = 1;
      nextRound = currentRound + 1;
    }
    return new DraftRegionCursor(draftId, region, nextRound, nextPick, snakeOrder, Instant.now());
  }

  // ===== GETTERS =====

  public UUID getDraftId() {
    return draftId;
  }

  public String getRegion() {
    return region;
  }

  public int getCurrentRound() {
    return currentRound;
  }

  public int getCurrentPick() {
    return currentPick;
  }

  public List<UUID> getSnakeOrder() {
    return snakeOrder;
  }

  public Instant getTurnStartedAt() {
    return turnStartedAt;
  }

  // ===== EQUALS / HASHCODE =====

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DraftRegionCursor that)) return false;
    return Objects.equals(draftId, that.draftId) && Objects.equals(region, that.region);
  }

  @Override
  public int hashCode() {
    return Objects.hash(draftId, region);
  }
}
