package com.fortnite.pronos.domain.team.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pure domain model representing the computed PR delta for a team (participant) in a game over a
 * competition period. Immutable — all mutation produces a new instance via restore().
 */
public final class TeamScoreDelta {

  private final UUID id;
  private final UUID gameId;
  private final UUID participantId;
  private final LocalDate periodStart;
  private final LocalDate periodEnd;
  private final int deltaPr;
  private final LocalDateTime computedAt;

  /** Creation constructor — generates a new UUID and sets computedAt to now. */
  public TeamScoreDelta(
      UUID gameId, UUID participantId, LocalDate periodStart, LocalDate periodEnd, int deltaPr) {
    this.id = UUID.randomUUID();
    this.gameId = gameId;
    this.participantId = participantId;
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
    this.deltaPr = deltaPr;
    this.computedAt = LocalDateTime.now();
  }

  /** Reconstitution factory for persistence mapping. */
  public static TeamScoreDelta restore(
      UUID id,
      UUID gameId,
      UUID participantId,
      LocalDate periodStart,
      LocalDate periodEnd,
      int deltaPr,
      LocalDateTime computedAt) {
    return new TeamScoreDelta(
        id, gameId, participantId, periodStart, periodEnd, deltaPr, computedAt);
  }

  private TeamScoreDelta(
      UUID id,
      UUID gameId,
      UUID participantId,
      LocalDate periodStart,
      LocalDate periodEnd,
      int deltaPr,
      LocalDateTime computedAt) {
    this.id = id;
    this.gameId = gameId;
    this.participantId = participantId;
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
    this.deltaPr = deltaPr;
    this.computedAt = computedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getGameId() {
    return gameId;
  }

  public UUID getParticipantId() {
    return participantId;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public int getDeltaPr() {
    return deltaPr;
  }

  public LocalDateTime getComputedAt() {
    return computedAt;
  }
}
