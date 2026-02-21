package com.fortnite.pronos.service.game;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.DraftDto;
import com.fortnite.pronos.dto.DraftPickDto;
import com.fortnite.pronos.exception.DraftIncompleteException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.NotYourTurnException;
import com.fortnite.pronos.exception.PlayerAlreadySelectedException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.service.draft.DraftService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service responsible for game draft operations Handles draft lifecycle and player selection */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GameDraftService {

  private final GameDomainRepositoryPort gameDomainRepository;
  private final DraftDomainRepositoryPort draftDomainRepository;
  private final DraftRepositoryPort draftRepository;
  private final DraftPickRepositoryPort draftPickRepository;
  private final PlayerDomainRepositoryPort playerRepository;
  private final GameParticipantRepositoryPort gameParticipantRepository;
  private final DraftService draftService;

  /** Starts draft for a game */
  public DraftDto startDraft(UUID gameId, UUID creatorId) {
    log.debug("Starting draft for game {} by user {}", gameId, creatorId);

    Game domainGame = findDomainGameOrThrow(gameId);
    validateUserCanStartDraft(domainGame, creatorId);
    validateGameCanStartDraft(domainGame);

    if (!domainGame.startDraft()) {
      throw new InvalidGameStateException("Game must be ready for draft (CREATING)");
    }
    gameDomainRepository.save(domainGame);

    com.fortnite.pronos.model.Draft draft = draftService.startDraftForGame(gameId);

    log.info("Draft started for game {}", domainGame.getName());
    return DraftDto.fromDraft(draft);
  }

  /** Pauses draft for a game */
  public DraftDto pauseDraft(UUID gameId, UUID userId) {
    log.debug("Pausing draft for game {} by user {}", gameId, userId);

    Game domainGame = findDomainGameOrThrow(gameId);
    validateUserCanManageDraft(domainGame, userId);

    com.fortnite.pronos.model.Draft draft = findActiveDraftEntity(gameId);
    draftService.pauseDraft(draft);

    log.info("Draft paused for game {}", domainGame.getName());

    // Return updated draft state
    return DraftDto.fromDraft(draft);
  }

  /** Resumes draft for a game */
  public DraftDto resumeDraft(UUID gameId, UUID userId) {
    log.debug("Resuming draft for game {} by user {}", gameId, userId);

    Game domainGame = findDomainGameOrThrow(gameId);
    validateUserCanManageDraft(domainGame, userId);

    com.fortnite.pronos.model.Draft draft = findActiveDraftEntity(gameId);
    draftService.resumeDraft(draft);

    log.info("Draft resumed for game {}", domainGame.getName());

    // Return updated draft state
    return DraftDto.fromDraft(draft);
  }

  /** Finishes draft for a game */
  public DraftDto finishDraft(UUID gameId, UUID userId) {
    log.debug("Finishing draft for game {} by user {}", gameId, userId);

    Game domainGame = findDomainGameOrThrow(gameId);
    validateUserCanManageDraft(domainGame, userId);

    com.fortnite.pronos.model.Draft draft = findActiveDraftEntity(gameId);
    validateDraftCanBeFinished(draft);

    draftService.finishDraft(draft);
    if (!domainGame.completeDraft()) {
      throw new InvalidGameStateException("Game must be in drafting status");
    }
    gameDomainRepository.save(domainGame);

    log.info("Draft finished for game {}", domainGame.getName());

    // Return updated draft state
    return DraftDto.fromDraft(draft);
  }

  /** Selects a player in the draft */
  public DraftPickDto selectPlayer(UUID gameId, UUID userId, UUID playerId) {
    log.debug("User {} selecting player {} in game {}", userId, playerId, gameId);

    Game domainGame = findDomainGameOrThrow(gameId);
    com.fortnite.pronos.model.Draft draft = findActiveDraftEntity(gameId);
    com.fortnite.pronos.model.Player player = findPlayerOrThrow(playerId);

    validatePlayerSelection(draft, userId, player);

    // Persistance explicite du pick
    com.fortnite.pronos.model.GameParticipant participant =
        gameParticipantRepository
            .findByUserIdAndGameId(userId, gameId)
            .orElseThrow(() -> new IllegalArgumentException("Participant not found for user"));

    com.fortnite.pronos.model.DraftPick newPick =
        new com.fortnite.pronos.model.DraftPick(
            draft, participant, player, draft.getCurrentRound(), draft.getCurrentPick());

    com.fortnite.pronos.model.DraftPick savedPick = draftPickRepository.save(newPick);

    // Avancer le draft (pas de parallélisme, avance séquentielle)
    draftService.nextPick(draft, domainGame.getMaxParticipants());

    log.info(
        "Player {} selected by user {} in game {}", player.getName(), userId, domainGame.getName());
    return DraftPickDto.fromDraftPick(savedPick);
  }

  /** Gets draft for a game */
  @Transactional(readOnly = true)
  public DraftDto getDraftByGame(UUID gameId) {
    com.fortnite.pronos.model.Draft draft = findActiveDraftEntity(gameId);
    return DraftDto.fromDraft(draft);
  }

  /** Gets draft picks for a game */
  @Transactional(readOnly = true)
  public List<DraftPickDto> getDraftPicks(UUID gameId) {
    com.fortnite.pronos.model.Draft draft = findActiveDraftEntity(gameId);

    return draftPickRepository.findByDraftOrderByPickNumber(draft).stream()
        .map(DraftPickDto::fromDraftPick)
        .toList();
  }

  // Private helper methods

  private Game findDomainGameOrThrow(UUID gameId) {
    return gameDomainRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
  }

  private com.fortnite.pronos.model.Player findPlayerOrThrow(UUID playerId) {
    return playerRepository
        .findById(playerId)
        .map(this::toLegacyPlayer)
        .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
  }

  private com.fortnite.pronos.model.Draft findActiveDraftEntity(UUID gameId) {
    com.fortnite.pronos.domain.draft.model.Draft domainDraft =
        draftDomainRepository
            .findActiveByGameId(gameId)
            .orElseThrow(() -> new InvalidDraftStateException("No active draft found for game"));

    return draftRepository
        .findById(domainDraft.getId())
        .orElseThrow(() -> new InvalidDraftStateException("No active draft found for game"));
  }

  private void validateUserCanStartDraft(Game game, UUID userId) {
    if (game.getCreatorId() == null) {
      throw new InvalidGameStateException("Game creator is missing");
    }
    if (!game.getCreatorId().equals(userId)) {
      throw new UnauthorizedAccessException("Only game creator can start draft");
    }
  }

  private void validateUserCanManageDraft(Game game, UUID userId) {
    if (game.getCreatorId() == null) {
      throw new InvalidGameStateException("Game creator is missing");
    }
    if (!game.getCreatorId().equals(userId)) {
      throw new UnauthorizedAccessException("Only game creator can manage draft");
    }
  }

  private void validateGameCanStartDraft(Game game) {
    if (game.getTotalParticipantCount() < 2) {
      throw new InvalidGameStateException("Not enough participants to start draft");
    }

    if (game.getStatus() != GameStatus.CREATING) {
      throw new InvalidGameStateException("Game must be ready for draft (CREATING)");
    }
  }

  private void validateDraftCanBeFinished(com.fortnite.pronos.model.Draft draft) {
    if (!draftService.isDraftComplete(draft)) {
      throw new DraftIncompleteException("Draft is not complete yet");
    }
  }

  private void validatePlayerSelection(
      com.fortnite.pronos.model.Draft draft, UUID userId, com.fortnite.pronos.model.Player player) {
    // Check if it's user's turn
    if (!draftService.isUserTurn(draft, userId)) {
      throw new NotYourTurnException("It's not your turn to pick");
    }

    // Check if player is already selected
    if (isPlayerAlreadySelected(draft, player)) {
      throw new PlayerAlreadySelectedException("Player is already selected");
    }

    // Check region limits if applicable
    validateRegionLimits(userId, player);
  }

  private boolean isPlayerAlreadySelected(
      com.fortnite.pronos.model.Draft draft, com.fortnite.pronos.model.Player player) {
    return draftPickRepository.existsByDraftAndPlayer(draft, player);
  }

  private void validateRegionLimits(UUID userId, com.fortnite.pronos.model.Player player) {
    // This would need to check if selecting this player would exceed region limits
    // Implementation depends on your business rules
    log.debug(
        "Validating region limits for player {} selection by user {}", player.getId(), userId);
  }

  private com.fortnite.pronos.model.Player toLegacyPlayer(
      com.fortnite.pronos.domain.player.model.Player domainPlayer) {
    com.fortnite.pronos.model.Player legacyPlayer = new com.fortnite.pronos.model.Player();
    legacyPlayer.setId(domainPlayer.getId());
    legacyPlayer.setFortniteId(domainPlayer.getFortniteId());
    legacyPlayer.setUsername(domainPlayer.getUsername());
    legacyPlayer.setNickname(domainPlayer.getNickname());
    legacyPlayer.setRegion(toLegacyRegion(domainPlayer.getRegion()));
    legacyPlayer.setTranche(domainPlayer.getTranche());
    legacyPlayer.setCurrentSeason(domainPlayer.getCurrentSeason());
    legacyPlayer.setLocked(domainPlayer.isLocked());
    return legacyPlayer;
  }

  private com.fortnite.pronos.model.Player.Region toLegacyRegion(
      com.fortnite.pronos.domain.game.model.PlayerRegion region) {
    if (region == null) {
      return com.fortnite.pronos.model.Player.Region.UNKNOWN;
    }
    try {
      return com.fortnite.pronos.model.Player.Region.valueOf(region.name());
    } catch (IllegalArgumentException ex) {
      return com.fortnite.pronos.model.Player.Region.UNKNOWN;
    }
  }
}
