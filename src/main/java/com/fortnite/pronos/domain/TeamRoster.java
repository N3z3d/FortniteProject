package com.fortnite.pronos.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Domain logic for team roster management. Pure domain rules without JPA dependencies. */
public final class TeamRoster {

  private TeamRoster() {}

  /** Default maximum players per team. */
  public static final int DEFAULT_MAX_PLAYERS = 10;

  /**
   * Validates if a player can be added to a team roster.
   *
   * @param existingPlayerIds IDs of players already on roster
   * @param playerIdToAdd ID of player being added
   * @param existingPositions positions already assigned
   * @param positionToAssign position for new player
   * @param maxPlayers maximum roster size
   * @return validation result
   */
  public static ValidationResult canAddPlayer(
      Set<UUID> existingPlayerIds,
      UUID playerIdToAdd,
      Set<Integer> existingPositions,
      int positionToAssign,
      int maxPlayers) {

    if (playerIdToAdd == null) {
      return ValidationResult.failure("Player ID cannot be null");
    }
    if (existingPlayerIds.contains(playerIdToAdd)) {
      return ValidationResult.failure("Player is already on the team");
    }
    if (existingPlayerIds.size() >= maxPlayers) {
      return ValidationResult.failure("Team roster is full (max " + maxPlayers + ")");
    }
    if (positionToAssign < 1) {
      return ValidationResult.failure("Position must be at least 1");
    }
    if (existingPositions.contains(positionToAssign)) {
      return ValidationResult.failure("Position " + positionToAssign + " is already taken");
    }
    return ValidationResult.success();
  }

  /**
   * Validates that all positions in a roster are unique.
   *
   * @param positions list of positions to validate
   * @return validation result
   */
  public static ValidationResult validateUniquePositions(List<Integer> positions) {
    if (positions == null || positions.isEmpty()) {
      return ValidationResult.success();
    }
    Set<Integer> seen = new HashSet<>();
    for (Integer pos : positions) {
      if (pos == null || pos < 1) {
        return ValidationResult.failure("All positions must be positive integers");
      }
      if (!seen.add(pos)) {
        return ValidationResult.failure("Duplicate position found: " + pos);
      }
    }
    return ValidationResult.success();
  }

  /**
   * Calculates the next available position for a roster.
   *
   * @param existingPositions positions already assigned
   * @return next available position number
   */
  public static int nextAvailablePosition(Set<Integer> existingPositions) {
    if (existingPositions == null || existingPositions.isEmpty()) {
      return 1;
    }
    int maxPosition = existingPositions.stream().mapToInt(Integer::intValue).max().orElse(0);
    return maxPosition + 1;
  }

  /**
   * Validates if a player can be removed from a team.
   *
   * @param existingPlayerIds IDs of players on roster
   * @param playerIdToRemove ID of player being removed
   * @param minPlayers minimum roster size to maintain
   * @return validation result
   */
  public static ValidationResult canRemovePlayer(
      Set<UUID> existingPlayerIds, UUID playerIdToRemove, int minPlayers) {

    if (playerIdToRemove == null) {
      return ValidationResult.failure("Player ID cannot be null");
    }
    if (!existingPlayerIds.contains(playerIdToRemove)) {
      return ValidationResult.failure("Player is not on the team");
    }
    if (existingPlayerIds.size() <= minPlayers) {
      return ValidationResult.failure(
          "Cannot remove player - minimum roster size is " + minPlayers);
    }
    return ValidationResult.success();
  }

  /**
   * Checks if a specific position is valid for a given roster size context.
   *
   * @param position position to validate
   * @param rosterSize current roster size
   * @return true if position is reasonable
   */
  public static boolean isValidPosition(int position, int rosterSize) {
    // Position should be between 1 and (roster size + 1) for new additions
    return position >= 1 && position <= rosterSize + 1;
  }

  /** Result of a roster validation. */
  public record ValidationResult(boolean valid, String errorMessage) {

    public static ValidationResult success() {
      return new ValidationResult(true, null);
    }

    public static ValidationResult failure(String errorMessage) {
      return new ValidationResult(false, errorMessage);
    }
  }
}
