package com.fortnite.pronos.core.domain;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;

import lombok.extern.slf4j.Slf4j;

/**
 * PHASE 2A: CLEAN ARCHITECTURE - Domain Service
 *
 * <p>Contains domain logic that doesn't naturally fit within a single entity but operates on domain
 * concepts. This helps eliminate circular dependencies by centralizing domain rules and
 * calculations.
 *
 * <p>Domain Services are different from Application Services: - Domain Services: Contain domain
 * logic operating on domain objects - Application Services: Orchestrate use cases and coordinate
 * between layers
 */
@Slf4j
@Component
@SuppressWarnings({"java:S3923"})
public class GameDomainService {

  private static final int MIN_PARTICIPANTS = 2;
  private static final int MIN_PARTICIPANTS_DIVISOR = 2;
  private static final int PARTICIPANT_PRIORITY_WEIGHT = 10;
  private static final int MAX_AGE_PRIORITY_HOURS = 168;
  private static final int CAPACITY_PRIORITY_WEIGHT = 50;
  private static final int MAX_ALLOWED_PARTICIPANTS = 50;

  /** Calculate if a game can be started based on domain rules */
  public boolean canStartGame(Game game, int currentParticipants) {
    if (game.getStatus() != GameStatus.CREATING && game.getStatus() != GameStatus.DRAFTING) {
      log.debug("Game {} cannot start - wrong status: {}", game.getId(), game.getStatus());
      return false;
    }

    int minParticipants = calculateMinimumParticipants(game);
    boolean hasEnoughParticipants = currentParticipants >= minParticipants;

    log.debug(
        "Game {} start check - participants: {}, minimum: {}, can start: {}",
        game.getId(),
        currentParticipants,
        minParticipants,
        hasEnoughParticipants);

    return hasEnoughParticipants;
  }

  /** Calculate minimum participants based on game rules */
  public int calculateMinimumParticipants(Game game) {
    // Domain rule: minimum 2 participants, or half of max capacity, whichever is higher
    return Math.max(MIN_PARTICIPANTS, game.getMaxParticipants() / MIN_PARTICIPANTS_DIVISOR);
  }

  /** Determine if a user can participate in a game */
  public boolean canUserParticipate(User user, Game game) {
    if (user == null || game == null) {
      return false;
    }

    // Domain rule: Creator cannot participate as a regular participant
    if (game.getCreator() != null
        && game.getCreator().getId() != null
        && game.getCreator().getId().equals(user.getId())) {
      return false;
    }

    // Domain rule: Only participants and admins can join games
    User.UserRole role = user.getRole();
    return role == User.UserRole.USER || role == User.UserRole.ADMIN;
  }

  /** Calculate game priority score for scheduling */
  public int calculateGamePriority(Game game, int participantCount) {
    int priority = 0;

    // Higher priority for games with more participants
    priority += participantCount * PARTICIPANT_PRIORITY_WEIGHT;

    // Higher priority for older games
    if (game.getCreatedAt() != null) {
      long hoursOld =
          java.time.Duration.between(game.getCreatedAt(), LocalDateTime.now()).toHours();
      priority += (int) Math.min(hoursOld, MAX_AGE_PRIORITY_HOURS);
    }

    // Higher priority for games close to capacity
    double capacityRatio = (double) participantCount / game.getMaxParticipants();
    priority += (int) (capacityRatio * CAPACITY_PRIORITY_WEIGHT);

    return priority;
  }

  /** Determine next logical game status */
  public GameStatus determineNextStatus(Game game, int participantCount) {
    if (game.getStatus() == GameStatus.CREATING) {
      int minParticipants = calculateMinimumParticipants(game);
      log.debug(
          "Game {} next status check - participants: {}, minimum: {}",
          game.getId(),
          participantCount,
          minParticipants);
    }
    return game.getStatus();
  }

  /** Validate game configuration */
  public boolean isValidGameConfiguration(Game game) {
    boolean hasValidMinimumParticipants = game.getMaxParticipants() >= MIN_PARTICIPANTS;
    if (!hasValidMinimumParticipants) {
      log.warn("Invalid game configuration: maxParticipants must be at least 2");
    }

    boolean hasValidMaximumParticipants = game.getMaxParticipants() <= MAX_ALLOWED_PARTICIPANTS;
    if (!hasValidMaximumParticipants) {
      log.warn("Invalid game configuration: maxParticipants cannot exceed 50");
    }

    boolean hasValidName = game.getName() != null && !game.getName().trim().isEmpty();
    if (!hasValidName) {
      log.warn("Invalid game configuration: name is required");
    }

    return hasValidMinimumParticipants && hasValidMaximumParticipants && hasValidName;
  }
}
