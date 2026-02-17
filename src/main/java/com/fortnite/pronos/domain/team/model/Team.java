package com.fortnite.pronos.domain.team.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain model representing a fantasy league team (aggregate root). Contains all business
 * logic with zero framework dependencies (no JPA, no Spring, no Lombok).
 */
public final class Team {

  private UUID id;
  private String name;
  private UUID ownerId;
  private int season;
  private UUID gameId;
  private int completedTradesCount;
  private final List<TeamMember> members;

  /** Business constructor for creating a new team. */
  public Team(String name, UUID ownerId, int season) {
    validateCreation(name, ownerId, season);
    this.id = UUID.randomUUID();
    this.name = name.trim();
    this.ownerId = ownerId;
    this.season = season;
    this.completedTradesCount = 0;
    this.members = new ArrayList<>();
  }

  /** Reconstitution factory for persistence mapping. */
  public static Team restore(
      UUID id,
      String name,
      UUID ownerId,
      int season,
      UUID gameId,
      int completedTradesCount,
      List<TeamMember> members) {
    Team team = new Team();
    team.id = id;
    team.name = name;
    team.ownerId = ownerId;
    team.season = season;
    team.gameId = gameId;
    team.completedTradesCount = completedTradesCount;
    if (members != null) {
      team.members.addAll(members);
    }
    return team;
  }

  /** Private no-arg constructor for restore(). */
  private Team() {
    this.members = new ArrayList<>();
  }

  // ===============================
  // MEMBER MANAGEMENT
  // ===============================

  /** Adds a player at the given position. Validates position uniqueness among active members. */
  public void addMember(UUID playerId, int position) {
    Objects.requireNonNull(playerId, "Player ID cannot be null");
    if (position < 1) {
      throw new IllegalArgumentException("Position must be at least 1");
    }
    boolean positionTaken =
        members.stream().anyMatch(m -> m.isActive() && m.getPosition() == position);
    if (positionTaken) {
      throw new IllegalStateException("Position " + position + " is already taken");
    }
    boolean alreadyActive =
        members.stream().anyMatch(m -> m.isActive() && m.getPlayerId().equals(playerId));
    if (alreadyActive) {
      throw new IllegalStateException("Player is already an active member");
    }
    members.add(new TeamMember(playerId, position));
  }

  /** Ends the active membership of the given player. */
  public void removeMember(UUID playerId) {
    members.stream()
        .filter(m -> m.isActive() && m.getPlayerId().equals(playerId))
        .findFirst()
        .ifPresent(TeamMember::endMembership);
  }

  /** Returns true if the player is an active member. */
  public boolean hasActiveMember(UUID playerId) {
    return members.stream().anyMatch(m -> m.isActive() && m.getPlayerId().equals(playerId));
  }

  /** Returns the position of the active member, or throws if not found. */
  public int getMemberPosition(UUID playerId) {
    return members.stream()
        .filter(m -> m.isActive() && m.getPlayerId().equals(playerId))
        .findFirst()
        .map(TeamMember::getPosition)
        .orElseThrow(() -> new IllegalArgumentException("Player is not an active member"));
  }

  /** Returns an unmodifiable view of active members. */
  public List<TeamMember> getActiveMembers() {
    return members.stream().filter(TeamMember::isActive).toList();
  }

  /** Returns the count of active members. */
  public int getActiveMemberCount() {
    return (int) members.stream().filter(TeamMember::isActive).count();
  }

  /** Returns the total count of all members (including ended). */
  public int getMemberCount() {
    return members.size();
  }

  // ===============================
  // SETTERS FOR MUTABLE STATE
  // ===============================

  public void rename(String newName) {
    if (newName == null || newName.trim().isEmpty()) {
      throw new IllegalArgumentException("Team name cannot be null or empty");
    }
    this.name = newName.trim();
  }

  public void setGameId(UUID gameId) {
    this.gameId = gameId;
  }

  public void incrementCompletedTrades() {
    this.completedTradesCount++;
  }

  // ===============================
  // GETTERS
  // ===============================

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public int getSeason() {
    return season;
  }

  public UUID getGameId() {
    return gameId;
  }

  public int getCompletedTradesCount() {
    return completedTradesCount;
  }

  public List<TeamMember> getMembers() {
    return Collections.unmodifiableList(members);
  }

  // ===============================
  // EQUALS / HASHCODE
  // ===============================

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Team that)) return false;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  // ===============================
  // PRIVATE HELPERS
  // ===============================

  private void validateCreation(String name, UUID ownerId, int season) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Team name cannot be null or empty");
    }
    if (ownerId == null) {
      throw new IllegalArgumentException("Owner ID cannot be null");
    }
    if (season <= 0) {
      throw new IllegalArgumentException("Season must be positive");
    }
  }
}
