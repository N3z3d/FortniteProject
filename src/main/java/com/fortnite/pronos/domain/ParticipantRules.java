package com.fortnite.pronos.domain;

import java.util.UUID;

import com.fortnite.pronos.model.GameStatus;

/** Domain rules for participant management. Pure domain logic without JPA dependencies. */
public final class ParticipantRules {

  private ParticipantRules() {}

  /** Minimum number of participants for a valid game. */
  public static final int MIN_PARTICIPANTS = 2;

  /** Maximum number of participants allowed. */
  public static final int MAX_PARTICIPANTS = 50;

  /**
   * Validates game creation parameters.
   *
   * @param name game name
   * @param creatorId creator user ID
   * @param maxParticipants maximum participants
   * @return validation result
   */
  public static ValidationResult validateGameCreation(
      String name, UUID creatorId, int maxParticipants) {

    if (name == null || name.trim().isEmpty()) {
      return ValidationResult.failure("Game name cannot be null or empty");
    }
    if (creatorId == null) {
      return ValidationResult.failure("Game creator cannot be null");
    }
    if (maxParticipants < MIN_PARTICIPANTS || maxParticipants > MAX_PARTICIPANTS) {
      return ValidationResult.failure(
          "Max participants must be between " + MIN_PARTICIPANTS + " and " + MAX_PARTICIPANTS);
    }
    return ValidationResult.success();
  }

  /**
   * Checks if a participant can be added to a game.
   *
   * @param status current game status
   * @param currentCount current participant count
   * @param maxParticipants maximum allowed
   * @param userIdToAdd user ID being added
   * @param creatorId game creator ID
   * @param existingParticipantIds IDs of existing participants
   * @return validation result
   */
  public static ValidationResult canAddParticipant(
      GameStatus status,
      int currentCount,
      int maxParticipants,
      UUID userIdToAdd,
      UUID creatorId,
      java.util.Set<UUID> existingParticipantIds) {

    if (status != GameStatus.CREATING) {
      return ValidationResult.failure("Cannot join games that have already started");
    }
    if (userIdToAdd.equals(creatorId)) {
      return ValidationResult.failure("Creator is automatically a participant");
    }
    if (existingParticipantIds.contains(userIdToAdd)) {
      return ValidationResult.failure("User is already a participant");
    }
    if (currentCount >= maxParticipants) {
      return ValidationResult.failure("Game is full");
    }
    return ValidationResult.success();
  }

  /**
   * Calculates total participant count including creator.
   *
   * @param explicitParticipantCount count of explicit participants
   * @param creatorInParticipants whether creator is in participants list
   * @return total count
   */
  public static int calculateTotalParticipants(
      int explicitParticipantCount, boolean creatorInParticipants) {
    return creatorInParticipants ? explicitParticipantCount : explicitParticipantCount + 1;
  }

  /**
   * Calculates available spots in a game.
   *
   * @param maxParticipants maximum allowed
   * @param currentTotal current total count
   * @return available spots
   */
  public static int calculateAvailableSpots(int maxParticipants, int currentTotal) {
    return Math.max(0, maxParticipants - currentTotal);
  }

  /** Result of a validation check. */
  public record ValidationResult(boolean valid, String errorMessage) {

    public static ValidationResult success() {
      return new ValidationResult(true, null);
    }

    public static ValidationResult failure(String errorMessage) {
      return new ValidationResult(false, errorMessage);
    }
  }
}
