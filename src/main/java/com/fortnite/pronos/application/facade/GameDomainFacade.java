package com.fortnite.pronos.application.facade;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.GameLifecycle;
import com.fortnite.pronos.domain.InvitationCodeGenerator;
import com.fortnite.pronos.domain.ParticipantRules;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.User;

/**
 * Facade bridging Game entity with pure domain rules. Extracts entity data, applies domain logic,
 * and returns results or applies changes to entities.
 */
@Component
public class GameDomainFacade {

  private final InvitationCodeGenerator codeGenerator;

  public GameDomainFacade() {
    this.codeGenerator = new InvitationCodeGenerator();
  }

  /**
   * Validates and attempts to start a draft for the game.
   *
   * @param game game entity
   * @return result of the operation
   */
  public GameLifecycle.TransitionResult tryStartDraft(Game game) {
    int totalParticipants = game.getTotalParticipantCount();
    GameLifecycle.TransitionResult result =
        GameLifecycle.canStartDraft(game.getStatus(), totalParticipants);

    if (result.allowed()) {
      game.setStatus(result.newStatus());
    }
    return result;
  }

  /**
   * Validates and attempts to complete the draft.
   *
   * @param game game entity
   * @return result of the operation
   */
  public GameLifecycle.TransitionResult tryCompleteDraft(Game game) {
    GameLifecycle.TransitionResult result = GameLifecycle.canCompleteDraft(game.getStatus());

    if (result.allowed()) {
      game.setStatus(result.newStatus());
    }
    return result;
  }

  /**
   * Validates and attempts to finish the game.
   *
   * @param game game entity
   * @return result of the operation
   */
  public GameLifecycle.TransitionResult tryFinishGame(Game game) {
    GameLifecycle.TransitionResult result = GameLifecycle.canFinishGame(game.getStatus());

    if (result.allowed()) {
      game.setStatus(result.newStatus());
    }
    return result;
  }

  /**
   * Validates and attempts to cancel the game.
   *
   * @param game game entity
   * @return result of the operation
   */
  public GameLifecycle.TransitionResult tryCancelGame(Game game) {
    GameLifecycle.TransitionResult result = GameLifecycle.canCancelGame(game.getStatus());

    if (result.allowed()) {
      game.setStatus(result.newStatus());
    }
    return result;
  }

  /**
   * Validates if a user can be added as a participant.
   *
   * @param game game entity
   * @param user user to add
   * @return validation result
   */
  public ParticipantRules.ValidationResult canAddParticipant(Game game, User user) {
    Set<UUID> existingParticipantIds = extractParticipantUserIds(game);

    return ParticipantRules.canAddParticipant(
        game.getStatus(),
        game.getTotalParticipantCount(),
        game.getMaxParticipants(),
        user.getId(),
        game.getCreator().getId(),
        existingParticipantIds);
  }

  /**
   * Validates game creation parameters.
   *
   * @param name proposed game name
   * @param creator creator user
   * @param maxParticipants maximum participants
   * @return validation result
   */
  public ParticipantRules.ValidationResult validateGameCreation(
      String name, User creator, int maxParticipants) {
    return ParticipantRules.validateGameCreation(
        name, creator != null ? creator.getId() : null, maxParticipants);
  }

  /**
   * Gets available spots in the game.
   *
   * @param game game entity
   * @return number of available spots
   */
  public int getAvailableSpots(Game game) {
    return ParticipantRules.calculateAvailableSpots(
        game.getMaxParticipants(), game.getTotalParticipantCount());
  }

  /**
   * Generates an invitation code for the game if not already set.
   *
   * @param game game entity
   * @return the invitation code
   */
  public String ensureInvitationCode(Game game) {
    if (game.getInvitationCode() == null) {
      game.setInvitationCode(codeGenerator.generate());
    }
    return game.getInvitationCode();
  }

  /**
   * Validates an invitation code format.
   *
   * @param code code to validate
   * @return true if valid format
   */
  public boolean isValidInvitationCode(String code) {
    return InvitationCodeGenerator.isValidFormat(code);
  }

  private Set<UUID> extractParticipantUserIds(Game game) {
    if (game.getParticipants() == null) {
      return Set.of();
    }
    return game.getParticipants().stream()
        .filter(p -> p.getUser() != null)
        .map(GameParticipant::getUser)
        .map(User::getId)
        .collect(Collectors.toSet());
  }
}
