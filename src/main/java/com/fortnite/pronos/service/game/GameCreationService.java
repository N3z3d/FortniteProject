package com.fortnite.pronos.service.game;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.application.usecase.GameCreationUseCase;
import com.fortnite.pronos.domain.game.model.DraftMode;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameRegionRule;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.mapper.GameDtoMapper;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameRequestException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.service.InvitationCodeService;
import com.fortnite.pronos.service.ValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for game creation operations. Uses domain models via
 * GameDomainRepositoryPort.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GameCreationService implements GameCreationUseCase {

  private final GameDomainRepositoryPort gameDomainRepository;
  private final UserRepositoryPort userRepository;
  private final ValidationService validationService;
  private final InvitationCodeService invitationCodeService;

  /** Creates a new game with all required setup */
  @Override
  public GameDto createGame(UUID creatorId, CreateGameRequest request) {
    log.debug("Creating game by user {}", creatorId);

    com.fortnite.pronos.model.User creator = findUserOrThrow(creatorId);
    validateGameRequest(request);

    Game game = buildDomainGame(creator, request);
    addRegionRules(game, request.getRegionRules());
    addCreatorAsParticipant(game, creator);

    Game savedGame = saveDomainGame(game);

    log.info("Game created successfully: {} by {}", savedGame.getName(), creator.getUsername());
    GameDto dto = GameDtoMapper.fromDomainGame(savedGame);
    dto.setCreatorUsername(creator.getUsername());
    dto.setCreatorName(creator.getUsername());
    return dto;
  }

  /** Soft deletes a game. Only allowed if the game has not started yet */
  @Override
  @Transactional
  public void deleteGame(UUID gameId) {
    log.debug("Soft deleting game {}", gameId);

    Game game = findDomainGameOrThrow(gameId);

    if (game.getStatus() != GameStatus.CREATING) {
      throw new IllegalStateException("Cannot delete game that has already started");
    }

    game.softDelete();
    saveDomainGame(game);
    log.info("Game {} soft deleted successfully", gameId);
  }

  /** Regenerates the invitation code for a game (permanent by default) */
  @Override
  @Transactional
  public GameDto regenerateInvitationCode(UUID gameId) {
    return regenerateInvitationCode(gameId, null);
  }

  /** Regenerates the invitation code for a game with configurable duration */
  @Override
  @Transactional
  public GameDto regenerateInvitationCode(UUID gameId, String duration) {
    log.debug("Regenerating invitation code for game {} with duration {}", gameId, duration);

    Game game = findDomainGameOrThrow(gameId);

    String newCode = invitationCodeService.generateUniqueCode();
    game.setInvitationCode(newCode);

    InvitationCodeService.CodeDuration codeDuration =
        InvitationCodeService.CodeDuration.fromString(duration);
    game.setInvitationCodeExpiresAt(invitationCodeService.calculateExpirationDate(codeDuration));

    Game savedGame = saveDomainGame(game);

    log.info(
        "Invitation code regenerated for game {}: {} (expires: {})",
        gameId,
        newCode,
        savedGame.getInvitationCodeExpiresAt());
    return GameDtoMapper.fromDomainGame(savedGame);
  }

  /** Deletes the current invitation code for a game. */
  @Override
  @Transactional
  public GameDto deleteInvitationCode(UUID gameId) {
    log.debug("Deleting invitation code for game {}", gameId);

    Game game = findDomainGameOrThrow(gameId);
    game.clearInvitationCode();

    Game savedGame = saveDomainGame(game);

    log.info("Invitation code deleted for game {}", gameId);
    return GameDtoMapper.fromDomainGame(savedGame);
  }

  /** Configures the competition period (start and end dates) for a game. */
  @Override
  @Transactional
  public GameDto configureCompetitionPeriod(UUID gameId, LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      throw new InvalidGameRequestException("Competition start and end dates are required");
    }
    if (startDate.isAfter(endDate)) {
      throw new InvalidGameRequestException("Competition start must be before or equal to end");
    }
    Game game = findDomainGameOrThrow(gameId);
    game.setCompetitionStart(startDate);
    game.setCompetitionEnd(endDate);
    Game savedGame = saveDomainGame(game);
    log.info("Competition period configured for game {}: {} to {}", gameId, startDate, endDate);
    return GameDtoMapper.fromDomainGame(savedGame);
  }

  /** Renames a game */
  @Override
  @Transactional
  public GameDto renameGame(UUID gameId, String newName) {
    log.debug("Renaming game {} to {}", gameId, newName);

    Game game = findDomainGameOrThrow(gameId);
    game.rename(newName);

    Game savedGame = saveDomainGame(game);

    log.info("Game {} renamed to {}", gameId, newName);
    return GameDtoMapper.fromDomainGame(savedGame);
  }

  private com.fortnite.pronos.model.User findUserOrThrow(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
  }

  private Game findDomainGameOrThrow(UUID gameId) {
    return gameDomainRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
  }

  private Game saveDomainGame(Game game) {
    return gameDomainRepository.save(game);
  }

  private void validateGameRequest(CreateGameRequest request) {
    validationService.validateCreateGameRequest(request);

    if (request.getRegionRules() != null && !request.getRegionRules().isEmpty()) {
      try {
        validationService.validateRegionRules(request.getRegionRules());
      } catch (IllegalArgumentException e) {
        throw new InvalidGameRequestException("Invalid region rules: " + e.getMessage());
      }
    }
  }

  private Game buildDomainGame(com.fortnite.pronos.model.User creator, CreateGameRequest request) {
    DraftMode draftMode = request.getDraftMode() != null ? request.getDraftMode() : DraftMode.SNAKE;
    int teamSize = request.getTeamSize() != null ? request.getTeamSize() : 5;
    int trancheSize = request.getTrancheSize() != null ? request.getTrancheSize() : 10;
    boolean tranchesEnabled = !Boolean.FALSE.equals(request.getTranchesEnabled());
    Game game =
        new Game(
            request.getName(),
            creator.getId(),
            request.getMaxParticipants(),
            draftMode,
            teamSize,
            trancheSize,
            tranchesEnabled);
    if (request.getDescription() != null) {
      game.setDescription(request.getDescription());
    }
    if (request.getCompetitionStart() != null) {
      game.setCompetitionStart(request.getCompetitionStart());
    }
    if (request.getCompetitionEnd() != null) {
      game.setCompetitionEnd(request.getCompetitionEnd());
    }
    return game;
  }

  private void addRegionRules(
      Game game, Map<com.fortnite.pronos.model.Player.Region, Integer> regionRules) {
    if (regionRules == null) {
      return;
    }
    regionRules.forEach(
        (region, maxPlayers) -> {
          PlayerRegion domainRegion = PlayerRegion.valueOf(region.name());
          game.addRegionRule(new GameRegionRule(domainRegion, maxPlayers));
        });
  }

  private void addCreatorAsParticipant(Game game, com.fortnite.pronos.model.User creator) {
    GameParticipant participant = new GameParticipant(creator.getId(), creator.getUsername(), true);
    game.addParticipant(participant);
  }
}
