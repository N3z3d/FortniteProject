package com.fortnite.pronos.service.game;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameRequestException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRegionRuleRepository;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;
import com.fortnite.pronos.service.InvitationCodeService;
import com.fortnite.pronos.service.ValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for game creation operations Follows Single Responsibility Principle - only
 * handles game creation
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GameCreationService {

  private final GameRepository gameRepository;
  private final GameRegionRuleRepository gameRegionRuleRepository;
  private final GameParticipantRepository gameParticipantRepository;
  private final UserRepository userRepository;
  private final ValidationService validationService;
  private final InvitationCodeService invitationCodeService;

  /** Creates a new game with all required setup */
  public GameDto createGame(UUID creatorId, CreateGameRequest request) {
    log.debug("Creating game by user {}", creatorId);

    User creator = findUserOrThrow(creatorId);
    validateGameRequest(request);

    Game game = createAndSaveGame(creator, request);
    createRegionRules(game, request.getRegionRules());
    addCreatorAsParticipant(game, creator);

    log.info("Game created successfully: {} by {}", game.getName(), creator.getUsername());
    return GameDto.fromGame(game);
  }

  /** Finds user or throws exception */
  private User findUserOrThrow(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
  }

  /** Validates game creation request */
  private void validateGameRequest(CreateGameRequest request) {
    validationService.validateCreateGameRequest(request);

    if (hasRegionRules(request)) {
      validateRegionRules(request.getRegionRules());
    }
  }

  /** Checks if request has region rules */
  private boolean hasRegionRules(CreateGameRequest request) {
    return request.getRegionRules() != null && !request.getRegionRules().isEmpty();
  }

  /** Validates region rules */
  private void validateRegionRules(Map<Player.Region, Integer> regionRules) {
    try {
      validationService.validateRegionRules(regionRules);
    } catch (IllegalArgumentException e) {
      throw new InvalidGameRequestException("Invalid region rules: " + e.getMessage());
    }
  }

  /** Creates and saves new game */
  private Game createAndSaveGame(User creator, CreateGameRequest request) {
    Game game = buildGame(creator, request);
    return gameRepository.save(game);
  }

  /** Builds new Game instance */
  private Game buildGame(User creator, CreateGameRequest request) {
    return Game.builder()
        .id(UUID.randomUUID())
        .name(request.getName())
        .description(request.getDescription())
        .creator(creator)
        .maxParticipants(request.getMaxParticipants())
        .status(GameStatus.CREATING)
        .createdAt(LocalDateTime.now())
        .invitationCode(invitationCodeService.generateUniqueCode())
        .build();
  }

  /** Creates region rules for the game */
  private void createRegionRules(Game game, Map<Player.Region, Integer> regionRules) {
    if (regionRules == null) {
      return;
    }

    regionRules
        .entrySet()
        .forEach(entry -> createAndSaveRegionRule(game, entry.getKey(), entry.getValue()));
  }

  /** Creates and saves a region rule */
  private void createAndSaveRegionRule(Game game, Player.Region region, Integer maxPlayers) {
    GameRegionRule rule =
        GameRegionRule.builder()
            .id(UUID.randomUUID())
            .game(game)
            .region(region)
            .maxPlayers(maxPlayers)
            .build();

    gameRegionRuleRepository.save(rule);
  }

  /** Adds creator as first participant */
  private void addCreatorAsParticipant(Game game, User creator) {
    GameParticipant participant = buildCreatorParticipant(game, creator);
    gameParticipantRepository.save(participant);
  }

  /** Builds creator participant */
  private GameParticipant buildCreatorParticipant(Game game, User creator) {
    return GameParticipant.builder()
        .id(UUID.randomUUID())
        .game(game)
        .user(creator)
        .joinedAt(LocalDateTime.now())
        .creator(true)
        .build();
  }

  /** Soft deletes a game. Only allowed if the game has not started yet */
  @Transactional
  public void deleteGame(UUID gameId) {
    log.debug("Soft deleting game {}", gameId);

    // Find the game
    Game game =
        gameRepository
            .findById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));

    // Check if game can be deleted (only CREATING status)
    if (game.getStatus() != GameStatus.CREATING) {
      throw new IllegalStateException("Cannot delete game that has already started");
    }

    // Soft delete the game (mark as deleted, don't remove from DB)
    game.softDelete();
    gameRepository.save(game);
    log.info("Game {} soft deleted successfully", gameId);
  }

  /** Regenerates the invitation code for a game (permanent by default) */
  @Transactional
  public GameDto regenerateInvitationCode(UUID gameId) {
    return regenerateInvitationCode(gameId, null);
  }

  /** Regenerates the invitation code for a game with configurable duration */
  @Transactional
  public GameDto regenerateInvitationCode(UUID gameId, String duration) {
    log.debug("Regenerating invitation code for game {} with duration {}", gameId, duration);

    Game game =
        gameRepository
            .findById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));

    String newCode = invitationCodeService.generateUniqueCode();
    game.setInvitationCode(newCode);

    // Calculate and set expiration date based on duration
    InvitationCodeService.CodeDuration codeDuration =
        InvitationCodeService.CodeDuration.fromString(duration);
    game.setInvitationCodeExpiresAt(invitationCodeService.calculateExpirationDate(codeDuration));

    game = gameRepository.save(game);

    log.info(
        "Invitation code regenerated for game {}: {} (expires: {})",
        gameId,
        newCode,
        game.getInvitationCodeExpiresAt());
    return GameDto.fromGame(game);
  }

  /** Renames a game */
  @Transactional
  public GameDto renameGame(UUID gameId, String newName) {
    log.debug("Renaming game {} to {}", gameId, newName);

    Game game =
        gameRepository
            .findById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));

    game.setName(newName);
    game = gameRepository.save(game);

    log.info("Game {} renamed to {}", gameId, newName);
    return GameDto.fromGame(game);
  }
}
