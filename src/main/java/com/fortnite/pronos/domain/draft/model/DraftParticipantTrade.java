package com.fortnite.pronos.domain.draft.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain aggregate for a 1v1 draft trade proposal between two participants (FR-34, FR-35).
 *
 * <p>A trade is PENDING until the target participant explicitly accepts or rejects it. No region or
 * rank restriction applies (unlike solo swap).
 */
public final class DraftParticipantTrade {

  private final UUID id;
  private final UUID draftId;
  private final UUID proposerParticipantId;
  private final UUID targetParticipantId;
  private final UUID playerFromProposerId;
  private final UUID playerFromTargetId;
  private final DraftParticipantTradeStatus status;
  private final LocalDateTime proposedAt;
  private final LocalDateTime resolvedAt;

  /** Creation constructor — generates a new ID, status = PENDING. */
  public DraftParticipantTrade(
      UUID draftId,
      UUID proposerParticipantId,
      UUID targetParticipantId,
      UUID playerFromProposerId,
      UUID playerFromTargetId) {
    this(
        UUID.randomUUID(),
        draftId,
        proposerParticipantId,
        targetParticipantId,
        playerFromProposerId,
        playerFromTargetId,
        DraftParticipantTradeStatus.PENDING,
        LocalDateTime.now(),
        null);
  }

  private DraftParticipantTrade(
      UUID id,
      UUID draftId,
      UUID proposerParticipantId,
      UUID targetParticipantId,
      UUID playerFromProposerId,
      UUID playerFromTargetId,
      DraftParticipantTradeStatus status,
      LocalDateTime proposedAt,
      LocalDateTime resolvedAt) {
    this.id = Objects.requireNonNull(id, "id cannot be null");
    this.draftId = Objects.requireNonNull(draftId, "draftId cannot be null");
    this.proposerParticipantId =
        Objects.requireNonNull(proposerParticipantId, "proposerParticipantId cannot be null");
    this.targetParticipantId =
        Objects.requireNonNull(targetParticipantId, "targetParticipantId cannot be null");
    this.playerFromProposerId =
        Objects.requireNonNull(playerFromProposerId, "playerFromProposerId cannot be null");
    this.playerFromTargetId =
        Objects.requireNonNull(playerFromTargetId, "playerFromTargetId cannot be null");
    this.status = Objects.requireNonNull(status, "status cannot be null");
    this.proposedAt = Objects.requireNonNull(proposedAt, "proposedAt cannot be null");
    this.resolvedAt = resolvedAt;
  }

  /** Reconstitution factory — for persistence mapping only. */
  public static DraftParticipantTrade restore(
      UUID id,
      UUID draftId,
      UUID proposerParticipantId,
      UUID targetParticipantId,
      UUID playerFromProposerId,
      UUID playerFromTargetId,
      DraftParticipantTradeStatus status,
      LocalDateTime proposedAt,
      LocalDateTime resolvedAt) {
    return new DraftParticipantTrade(
        id,
        draftId,
        proposerParticipantId,
        targetParticipantId,
        playerFromProposerId,
        playerFromTargetId,
        status,
        proposedAt,
        resolvedAt);
  }

  /** Returns a new instance with status ACCEPTED and resolvedAt = now. */
  public DraftParticipantTrade accept() {
    requirePending();
    return new DraftParticipantTrade(
        id,
        draftId,
        proposerParticipantId,
        targetParticipantId,
        playerFromProposerId,
        playerFromTargetId,
        DraftParticipantTradeStatus.ACCEPTED,
        proposedAt,
        LocalDateTime.now());
  }

  /** Returns a new instance with status REJECTED and resolvedAt = now. */
  public DraftParticipantTrade reject() {
    requirePending();
    return new DraftParticipantTrade(
        id,
        draftId,
        proposerParticipantId,
        targetParticipantId,
        playerFromProposerId,
        playerFromTargetId,
        DraftParticipantTradeStatus.REJECTED,
        proposedAt,
        LocalDateTime.now());
  }

  private void requirePending() {
    if (status != DraftParticipantTradeStatus.PENDING) {
      throw new IllegalStateException("Trade must be PENDING to transition; current: " + status);
    }
  }

  // ===== GETTERS =====

  public UUID getId() {
    return id;
  }

  public UUID getDraftId() {
    return draftId;
  }

  public UUID getProposerParticipantId() {
    return proposerParticipantId;
  }

  public UUID getTargetParticipantId() {
    return targetParticipantId;
  }

  public UUID getPlayerFromProposerId() {
    return playerFromProposerId;
  }

  public UUID getPlayerFromTargetId() {
    return playerFromTargetId;
  }

  public DraftParticipantTradeStatus getStatus() {
    return status;
  }

  public LocalDateTime getProposedAt() {
    return proposedAt;
  }

  public LocalDateTime getResolvedAt() {
    return resolvedAt;
  }

  // ===== EQUALS / HASHCODE =====

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DraftParticipantTrade that)) return false;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
