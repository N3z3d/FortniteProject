package com.fortnite.pronos.service.game;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.dto.DraftDto;
import com.fortnite.pronos.dto.DraftPickDto;
import com.fortnite.pronos.exception.DraftIncompleteException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.NotYourTurnException;
import com.fortnite.pronos.exception.PlayerAlreadySelectedException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.DraftPick;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.repository.DraftPickRepository;
import com.fortnite.pronos.repository.DraftRepository;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.service.draft.DraftService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service responsible for game draft operations Handles draft lifecycle and player selection */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GameDraftService {

  private final GameRepositoryPort gameRepository;
  private final DraftRepository draftRepository;
  private final DraftPickRepository draftPickRepository;
  private final PlayerRepository playerRepository;
  private final GameParticipantRepository gameParticipantRepository;
  private final DraftService draftService;

  /** Starts draft for a game */
  public DraftDto startDraft(UUID gameId, UUID creatorId) {
    log.debug("Starting draft for game {} by user {}", gameId, creatorId);

    Game game = findGameOrThrow(gameId);
    validateUserCanStartDraft(game, creatorId);
    validateGameCanStartDraft(game);

    updateGameStatus(game, GameStatus.DRAFTING);
    Draft draft = draftService.startDraft(game);

    log.info("Draft started for game {}", game.getName());
    return DraftDto.fromDraft(draft);
  }

  /** Pauses draft for a game */
  public DraftDto pauseDraft(UUID gameId, UUID userId) {
    log.debug("Pausing draft for game {} by user {}", gameId, userId);

    Game game = findGameOrThrow(gameId);
    validateUserCanManageDraft(game, userId);

    Draft draft = findActiveDraft(game);
    draftService.pauseDraft(draft);

    log.info("Draft paused for game {}", game.getName());

    // Return updated draft state
    return DraftDto.fromDraft(draft);
  }

  /** Resumes draft for a game */
  public DraftDto resumeDraft(UUID gameId, UUID userId) {
    log.debug("Resuming draft for game {} by user {}", gameId, userId);

    Game game = findGameOrThrow(gameId);
    validateUserCanManageDraft(game, userId);

    Draft draft = findActiveDraft(game);
    draftService.resumeDraft(draft);

    log.info("Draft resumed for game {}", game.getName());

    // Return updated draft state
    return DraftDto.fromDraft(draft);
  }

  /** Finishes draft for a game */
  public DraftDto finishDraft(UUID gameId, UUID userId) {
    log.debug("Finishing draft for game {} by user {}", gameId, userId);

    Game game = findGameOrThrow(gameId);
    validateUserCanManageDraft(game, userId);

    Draft draft = findActiveDraft(game);
    validateDraftCanBeFinished(draft);

    draftService.finishDraft(draft);
    updateGameStatus(game, GameStatus.ACTIVE);

    log.info("Draft finished for game {}", game.getName());

    // Return updated draft state
    return DraftDto.fromDraft(draft);
  }

  /** Selects a player in the draft */
  public DraftPickDto selectPlayer(UUID gameId, UUID userId, UUID playerId) {
    log.debug("User {} selecting player {} in game {}", userId, playerId, gameId);

    Game game = findGameOrThrow(gameId);
    Draft draft = findActiveDraft(game);
    Player player = findPlayerOrThrow(playerId);

    validatePlayerSelection(draft, userId, player);

    // Persistance explicite du pick
    GameParticipant participant =
        gameParticipantRepository
            .findByUserIdAndGameId(userId, gameId)
            .orElseThrow(() -> new IllegalArgumentException("Participant not found for user"));

    DraftPick newPick =
        new DraftPick(draft, participant, player, draft.getCurrentRound(), draft.getCurrentPick());

    DraftPick savedPick = draftPickRepository.save(newPick);

    // Avancer le draft (pas de parallélisme, avance séquentielle)
    draftService.nextPick(draft, game.getMaxParticipants());

    log.info("Player {} selected by user {} in game {}", player.getName(), userId, game.getName());
    return DraftPickDto.fromDraftPick(savedPick);
  }

  /** Gets draft for a game */
  @Transactional(readOnly = true)
  public DraftDto getDraftByGame(UUID gameId) {
    Game game = findGameOrThrow(gameId);
    Draft draft = findActiveDraft(game);
    return DraftDto.fromDraft(draft);
  }

  /** Gets draft picks for a game */
  @Transactional(readOnly = true)
  public List<DraftPickDto> getDraftPicks(UUID gameId) {
    Game game = findGameOrThrow(gameId);
    Draft draft = findActiveDraft(game);

    return draftPickRepository.findByDraftOrderByPickNumber(draft).stream()
        .map(DraftPickDto::fromDraftPick)
        .toList();
  }

  // Private helper methods

  private Game findGameOrThrow(UUID gameId) {
    return gameRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
  }

  private Player findPlayerOrThrow(UUID playerId) {
    return playerRepository
        .findById(playerId)
        .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
  }

  private Draft findActiveDraft(Game game) {
    return draftRepository
        .findByGame(game)
        .orElseThrow(() -> new InvalidDraftStateException("No active draft found for game"));
  }

  private void validateUserCanStartDraft(Game game, UUID userId) {
    if (!game.getCreator().getId().equals(userId)) {
      throw new UnauthorizedAccessException("Only game creator can start draft");
    }
  }

  private void validateUserCanManageDraft(Game game, UUID userId) {
    if (!game.getCreator().getId().equals(userId)) {
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

  private void validateDraftCanBeFinished(Draft draft) {
    if (!draftService.isDraftComplete(draft)) {
      throw new DraftIncompleteException("Draft is not complete yet");
    }
  }

  private void validatePlayerSelection(Draft draft, UUID userId, Player player) {
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

  private boolean isPlayerAlreadySelected(Draft draft, Player player) {
    return draftPickRepository.existsByDraftAndPlayer(draft, player);
  }

  private void validateRegionLimits(UUID userId, Player player) {
    // This would need to check if selecting this player would exceed region limits
    // Implementation depends on your business rules
    log.debug(
        "Validating region limits for player {} selection by user {}", player.getId(), userId);
  }

  private void updateGameStatus(Game game, GameStatus status) {
    game.setStatus(status);
    gameRepository.save(game);
  }
}
