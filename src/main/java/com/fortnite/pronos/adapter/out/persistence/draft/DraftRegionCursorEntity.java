package com.fortnite.pronos.adapter.out.persistence.draft;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** JPA entity backing the {@code draft_region_cursors} table. */
@Entity
@Table(name = "draft_region_cursors")
public class DraftRegionCursorEntity {

  @EmbeddedId private DraftRegionCursorId id;

  @Column(name = "current_round", nullable = false)
  private int currentRound;

  @Column(name = "current_pick", nullable = false)
  private int currentPick;

  /** Comma-separated ordered list of participant UUIDs. */
  @Column(name = "snake_order", nullable = false, columnDefinition = "TEXT")
  private String snakeOrder;

  protected DraftRegionCursorEntity() {}

  public DraftRegionCursorEntity(
      DraftRegionCursorId id, int currentRound, int currentPick, String snakeOrder) {
    this.id = id;
    this.currentRound = currentRound;
    this.currentPick = currentPick;
    this.snakeOrder = snakeOrder;
  }

  public DraftRegionCursorId getId() {
    return id;
  }

  public int getCurrentRound() {
    return currentRound;
  }

  public int getCurrentPick() {
    return currentPick;
  }

  public String getSnakeOrder() {
    return snakeOrder;
  }
}
