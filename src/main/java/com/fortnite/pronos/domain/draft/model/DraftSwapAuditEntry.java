package com.fortnite.pronos.domain.draft.model;

import java.time.LocalDateTime;
import java.util.UUID;

/** Immutable domain model recording a completed solo swap for audit purposes (FR-36). */
public final class DraftSwapAuditEntry {

  private final UUID id;
  private final UUID draftId;
  private final UUID participantId;
  private final UUID playerOutId;
  private final UUID playerInId;
  private final LocalDateTime occurredAt;

  /** Creation constructor — generates a new id and sets occurredAt to now. */
  public DraftSwapAuditEntry(UUID draftId, UUID participantId, UUID playerOutId, UUID playerInId) {
    this.id = UUID.randomUUID();
    this.draftId = draftId;
    this.participantId = participantId;
    this.playerOutId = playerOutId;
    this.playerInId = playerInId;
    this.occurredAt = LocalDateTime.now();
  }

  /** Restore factory for persistence reconstitution. */
  public static DraftSwapAuditEntry restore(
      UUID id,
      UUID draftId,
      UUID participantId,
      UUID playerOutId,
      UUID playerInId,
      LocalDateTime occurredAt) {
    return new DraftSwapAuditEntry(id, draftId, participantId, playerOutId, playerInId, occurredAt);
  }

  private DraftSwapAuditEntry(
      UUID id,
      UUID draftId,
      UUID participantId,
      UUID playerOutId,
      UUID playerInId,
      LocalDateTime occurredAt) {
    this.id = id;
    this.draftId = draftId;
    this.participantId = participantId;
    this.playerOutId = playerOutId;
    this.playerInId = playerInId;
    this.occurredAt = occurredAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getDraftId() {
    return draftId;
  }

  public UUID getParticipantId() {
    return participantId;
  }

  public UUID getPlayerOutId() {
    return playerOutId;
  }

  public UUID getPlayerInId() {
    return playerInId;
  }

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }
}
