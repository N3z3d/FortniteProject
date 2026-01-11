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
public class GameDomainService {

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
    return Math.max(2, game.getMaxParticipants() / 2);
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
    priority += participantCount * 10;

    // Higher priority for older games
    if (game.getCreatedAt() != null) {
      long hoursOld =
          java.time.Duration.between(game.getCreatedAt(), LocalDateTime.now()).toHours();
      priority += (int) Math.min(hoursOld, 168); // Cap at 1 week
    }

    // Higher priority for games close to capacity
    double capacityRatio = (double) participantCount / game.getMaxParticipants();
    priority += (int) (capacityRatio * 50);

    return priority;
  }

  /** Determine next logical game status */
  public GameStatus determineNextStatus(Game game, int participantCount) {
    switch (game.getStatus()) {
      case CREATING:
        int minParticipants = calculateMinimumParticipants(game);
        return participantCount >= minParticipants ? GameStatus.CREATING : GameStatus.CREATING;

      case DRAFTING:
        // This would be determined by draft completion logic
        return GameStatus.DRAFTING;

      case ACTIVE:
        // Game continues until end conditions are met
        return GameStatus.ACTIVE;

      default:
        return game.getStatus();
    }
  }

  /** Validate game configuration */
  public boolean isValidGameConfiguration(Game game) {
    if (game.getMaxParticipants() < 2) {
      log.warn("Invalid game configuration: maxParticipants must be at least 2");
      return false;
    }

    if (game.getMaxParticipants() > 50) { // Business rule: reasonable upper limit
      log.warn("Invalid game configuration: maxParticipants cannot exceed 50");
      return false;
    }

    if (game.getName() == null || game.getName().trim().isEmpty()) {
      log.warn("Invalid game configuration: name is required");
      return false;
    }

    return true;
  }
}
