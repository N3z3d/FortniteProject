package com.fortnite.pronos.domain.draft.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain aggregate representing the snake draft cursor for a single region.
 *
 * <p>Each region has an independent snake order and pick position. The common timer is shared at
 * the draft level (not per-cursor).
 */
public final class DraftRegionCursor {

  private final UUID draftId;
  private final String region;
  private final int currentRound;
  private final int currentPick;
  private final List<UUID> snakeOrder;

  /** Business constructor for a brand-new cursor (round 1, pick 1). */
  public DraftRegionCursor(UUID draftId, String region, List<UUID> snakeOrder) {
    this(draftId, region, 1, 1, snakeOrder);
  }

  private DraftRegionCursor(
      UUID draftId, String region, int currentRound, int currentPick, List<UUID> snakeOrder) {
    Objects.requireNonNull(draftId, "draftId cannot be null");
    Objects.requireNonNull(region, "region cannot be null");
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
  }

  /** Reconstitution factory — for persistence mapping only. */
  public static DraftRegionCursor restore(
      UUID draftId, String region, int currentRound, int currentPick, List<UUID> snakeOrder) {
    return new DraftRegionCursor(draftId, region, currentRound, currentPick, snakeOrder);
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
    return new DraftRegionCursor(draftId, region, nextRound, nextPick, snakeOrder);
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
