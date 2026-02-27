package com.fortnite.pronos.domain;

import java.util.Collection;
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
    String validationError = null;
    if (name == null || name.trim().isEmpty()) {
      validationError = "Game name cannot be null or empty";
    } else if (creatorId == null) {
      validationError = "Game creator cannot be null";
    } else if (maxParticipants < MIN_PARTICIPANTS || maxParticipants > MAX_PARTICIPANTS) {
      validationError =
          "Max participants must be between " + MIN_PARTICIPANTS + " and " + MAX_PARTICIPANTS;
    }
    return toValidationResult(validationError);
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
      Collection<UUID> existingParticipantIds) {
    String validationError = null;
    if (status != GameStatus.CREATING) {
      validationError = "Cannot join games that have already started";
    } else if (userIdToAdd == null) {
      validationError = "User to add cannot be null";
    } else if (userIdToAdd.equals(creatorId)) {
      validationError = "Creator is automatically a participant";
    } else if (existingParticipantIds.contains(userIdToAdd)) {
      validationError = "User is already a participant";
    } else if (currentCount >= maxParticipants) {
      validationError = "Game is full";
    }
    return toValidationResult(validationError);
  }

  /**
   * Calculates total participant count when the creator is already in the participants list.
   *
   * @param explicitParticipantCount count of explicit participants
   * @return total count
   */
  public static int calculateTotalParticipantsWithCreatorIncluded(int explicitParticipantCount) {
    return explicitParticipantCount;
  }

  /**
   * Calculates total participant count when the creator is not in the participants list.
   *
   * @param explicitParticipantCount count of explicit participants
   * @return total count
   */
  public static int calculateTotalParticipantsWithCreatorExcluded(int explicitParticipantCount) {
    return explicitParticipantCount + 1;
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

  private static ValidationResult toValidationResult(String validationError) {
    if (validationError == null) {
      return ValidationResult.success();
    }
    return ValidationResult.failure(validationError);
  }
}
