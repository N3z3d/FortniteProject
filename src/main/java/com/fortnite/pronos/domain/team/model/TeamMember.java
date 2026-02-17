package com.fortnite.pronos.domain.team.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain value object representing a player's membership in a team. Framework-free (no JPA, no
 * Spring, no Lombok).
 */
public final class TeamMember {

  private final UUID playerId;
  private final int position;
  private OffsetDateTime until;

  /** Business constructor for adding a new active member. */
  public TeamMember(UUID playerId, int position) {
    Objects.requireNonNull(playerId, "Player ID cannot be null");
    if (position < 1) {
      throw new IllegalArgumentException("Position must be at least 1");
    }
    this.playerId = playerId;
    this.position = position;
    this.until = null;
  }

  /** Reconstitution factory for persistence mapping. */
  public static TeamMember restore(UUID playerId, int position, OffsetDateTime until) {
    TeamMember member = new TeamMember(playerId, position);
    member.until = until;
    return member;
  }

  // ===============================
  // BUSINESS METHODS
  // ===============================

  /** Ends this membership by setting the until timestamp. */
  public void endMembership() {
    this.until = OffsetDateTime.now();
  }

  /** Returns true if this membership is still active (not ended). */
  public boolean isActive() {
    return until == null;
  }

  // ===============================
  // GETTERS
  // ===============================

  public UUID getPlayerId() {
    return playerId;
  }

  public int getPosition() {
    return position;
  }

  public OffsetDateTime getUntil() {
    return until;
  }

  // ===============================
  // EQUALS / HASHCODE
  // ===============================

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TeamMember that)) return false;
    return Objects.equals(playerId, that.playerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(playerId);
  }
}
