package com.fortnite.pronos.domain.player.identity.model;

import java.time.LocalDateTime;
import java.util.UUID;

public final class PlayerIdentityEntry {

  private UUID id;
  private final UUID playerId;
  private final String playerUsername;
  private final String playerRegion;
  private String epicId;
  private IdentityStatus status;
  private int confidenceScore;
  private String resolvedBy;
  private LocalDateTime resolvedAt;
  private LocalDateTime rejectedAt;
  private String rejectionReason;
  private final LocalDateTime createdAt;

  public PlayerIdentityEntry(
      UUID playerId, String playerUsername, String playerRegion, LocalDateTime createdAt) {
    this.id = UUID.randomUUID();
    this.playerId = playerId;
    this.playerUsername = playerUsername;
    this.playerRegion = playerRegion;
    this.status = IdentityStatus.UNRESOLVED;
    this.confidenceScore = 0;
    this.createdAt = createdAt;
  }

  @SuppressWarnings("java:S107")
  public static PlayerIdentityEntry restore(
      UUID id,
      UUID playerId,
      String playerUsername,
      String playerRegion,
      String epicId,
      IdentityStatus status,
      int confidenceScore,
      String resolvedBy,
      LocalDateTime resolvedAt,
      LocalDateTime rejectedAt,
      String rejectionReason,
      LocalDateTime createdAt) {
    PlayerIdentityEntry entry =
        new PlayerIdentityEntry(playerId, playerUsername, playerRegion, createdAt);
    entry.id = id;
    entry.epicId = epicId;
    entry.status = status;
    entry.confidenceScore = confidenceScore;
    entry.resolvedBy = resolvedBy;
    entry.resolvedAt = resolvedAt;
    entry.rejectedAt = rejectedAt;
    entry.rejectionReason = rejectionReason;
    return entry;
  }

  public void resolve(String epicId, int confidenceScore, String resolvedBy) {
    this.epicId = epicId;
    this.confidenceScore = confidenceScore;
    this.resolvedBy = resolvedBy;
    this.resolvedAt = LocalDateTime.now();
    this.status = IdentityStatus.RESOLVED;
  }

  public void reject(String reason, String rejectedBy) {
    this.rejectionReason = reason;
    this.resolvedBy = rejectedBy;
    this.rejectedAt = LocalDateTime.now();
    this.status = IdentityStatus.REJECTED;
  }

  public UUID getId() {
    return id;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public String getPlayerUsername() {
    return playerUsername;
  }

  public String getPlayerRegion() {
    return playerRegion;
  }

  public String getEpicId() {
    return epicId;
  }

  public IdentityStatus getStatus() {
    return status;
  }

  public int getConfidenceScore() {
    return confidenceScore;
  }

  public String getResolvedBy() {
    return resolvedBy;
  }

  public LocalDateTime getResolvedAt() {
    return resolvedAt;
  }

  public LocalDateTime getRejectedAt() {
    return rejectedAt;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
