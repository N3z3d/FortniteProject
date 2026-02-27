package com.fortnite.pronos.domain;

import java.util.Collection;
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
      Collection<UUID> existingPlayerIds,
      UUID playerIdToAdd,
      Collection<Integer> existingPositions,
      int positionToAssign,
      int maxPlayers) {
    String validationError = null;
    if (playerIdToAdd == null) {
      validationError = "Player ID cannot be null";
    } else if (existingPlayerIds.contains(playerIdToAdd)) {
      validationError = "Player is already on the team";
    } else if (existingPlayerIds.size() >= maxPlayers) {
      validationError = "Team roster is full (max " + maxPlayers + ")";
    } else if (positionToAssign < 1) {
      validationError = "Position must be at least 1";
    } else if (existingPositions.contains(positionToAssign)) {
      validationError = "Position " + positionToAssign + " is already taken";
    }
    return toValidationResult(validationError);
  }

  /**
   * Validates that all positions in a roster are unique.
   *
   * @param positions list of positions to validate
   * @return validation result
   */
  public static ValidationResult validateUniquePositions(List<Integer> positions) {
    String validationError = null;
    if (positions != null && !positions.isEmpty()) {
      Set<Integer> seen = new HashSet<>();
      for (Integer pos : positions) {
        if (pos == null || pos < 1) {
          validationError = "All positions must be positive integers";
          break;
        }
        if (!seen.add(pos)) {
          validationError = "Duplicate position found: " + pos;
          break;
        }
      }
    }
    return toValidationResult(validationError);
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
      Collection<UUID> existingPlayerIds, UUID playerIdToRemove, int minPlayers) {
    String validationError = null;
    if (playerIdToRemove == null) {
      validationError = "Player ID cannot be null";
    } else if (!existingPlayerIds.contains(playerIdToRemove)) {
      validationError = "Player is not on the team";
    } else if (existingPlayerIds.size() <= minPlayers) {
      validationError = "Cannot remove player - minimum roster size is " + minPlayers;
    }
    return toValidationResult(validationError);
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

  private static ValidationResult toValidationResult(String validationError) {
    if (validationError == null) {
      return ValidationResult.success();
    }
    return ValidationResult.failure(validationError);
  }
}
