package com.fortnite.pronos.adapter.out.persistence.draft;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA entity backing the {@code draft_async_selections} table. */
@Entity
@Table(name = "draft_async_selections")
public class DraftAsyncSelectionEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "window_id", nullable = false)
  private UUID windowId;

  @Column(name = "participant_id", nullable = false)
  private UUID participantId;

  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  protected DraftAsyncSelectionEntity() {}

  public DraftAsyncSelectionEntity(
      UUID id, UUID windowId, UUID participantId, UUID playerId, Instant submittedAt) {
    this.id = id;
    this.windowId = windowId;
    this.participantId = participantId;
    this.playerId = playerId;
    this.submittedAt = submittedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getWindowId() {
    return windowId;
  }

  public UUID getParticipantId() {
    return participantId;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }
}
