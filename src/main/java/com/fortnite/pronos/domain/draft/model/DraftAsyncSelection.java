package com.fortnite.pronos.domain.draft.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain value object representing one participant's anonymous pick within a simultaneous window.
 *
 * <p>The chosen player is hidden from other participants until the window resolves.
 */
public final class DraftAsyncSelection {

  private final UUID id;
  private final UUID windowId;
  private final UUID participantId;
  private final UUID playerId;
  private final Instant submittedAt;

  public DraftAsyncSelection(UUID windowId, UUID participantId, UUID playerId) {
    this(UUID.randomUUID(), windowId, participantId, playerId, Instant.now());
  }

  private DraftAsyncSelection(
      UUID id, UUID windowId, UUID participantId, UUID playerId, Instant submittedAt) {
    this.id = Objects.requireNonNull(id, "id cannot be null");
    this.windowId = Objects.requireNonNull(windowId, "windowId cannot be null");
    this.participantId = Objects.requireNonNull(participantId, "participantId cannot be null");
    this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
    this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt cannot be null");
  }

  /** Reconstitution factory — for persistence mapping only. */
  public static DraftAsyncSelection restore(
      UUID id, UUID windowId, UUID participantId, UUID playerId, Instant submittedAt) {
    return new DraftAsyncSelection(id, windowId, participantId, playerId, submittedAt);
  }

  // ===== GETTERS =====

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

  // ===== EQUALS / HASHCODE =====

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DraftAsyncSelection that)) return false;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
