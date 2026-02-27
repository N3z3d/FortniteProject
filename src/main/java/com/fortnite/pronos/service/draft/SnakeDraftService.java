package com.fortnite.pronos.service.draft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.draft.model.Draft;
import com.fortnite.pronos.domain.draft.model.SnakeTurn;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.dto.SnakeTurnResponse;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.NotYourTurnException;
import com.fortnite.pronos.model.GameParticipant;

/**
 * Orchestrates snake-draft turn management.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Initializing per-region cursors with a randomized participant order.
 *   <li>Querying the current turn for a region.
 *   <li>Validating and advancing the cursor when a pick is submitted.
 *   <li>Broadcasting the next turn via WebSocket after each advance.
 * </ul>
 *
 * <p>Pick <em>recording</em> (DraftPick persistence) is delegated to the existing {@code
 * GameDraftService.selectPlayer()} call in the controller layer.
 */
@Service
@Transactional
public class SnakeDraftService {

  static final String TOPIC_PREFIX = "/topic/draft/";
  static final String TOPIC_SUFFIX = "/snake";
  static final String GLOBAL_REGION = "GLOBAL";

  private final DraftPickOrchestratorService orchestratorService;
  private final GameDomainRepositoryPort gameDomainRepository;
  private final DraftDomainRepositoryPort draftDomainRepository;
  private final GameParticipantRepositoryPort gameParticipantRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final Random random;

  public SnakeDraftService(
      DraftPickOrchestratorService orchestratorService,
      GameDomainRepositoryPort gameDomainRepository,
      DraftDomainRepositoryPort draftDomainRepository,
      GameParticipantRepositoryPort gameParticipantRepository,
      SimpMessagingTemplate messagingTemplate,
      Random random) {
    this.orchestratorService = orchestratorService;
    this.gameDomainRepository = gameDomainRepository;
    this.draftDomainRepository = draftDomainRepository;
    this.gameParticipantRepository = gameParticipantRepository;
    this.messagingTemplate = messagingTemplate;
    this.random = random;
  }

  /**
   * Initializes snake-draft cursors for all regions of the given game.
   *
   * <p>Participants are shuffled randomly and one cursor is created per region (or a single {@code
   * GLOBAL} cursor if the game has no region rules). Returns the first turn (round 1, pick 1).
   *
   * @param gameId the game identifier
   * @return the first SnakeTurnResponse
   * @throws GameNotFoundException if the game does not exist
   * @throws InvalidDraftStateException if no active draft exists for the game
   */
  public SnakeTurnResponse initializeCursors(UUID gameId) {
    Game game = findGameOrThrow(gameId);
    Draft draft = findActiveDraftOrThrow(gameId);

    List<UUID> participantIds = buildShuffledParticipantIds(gameId);
    List<String> regions = resolveRegions(game);

    String firstRegion = regions.get(0);
    SnakeTurn firstTurn = null;
    for (String region : regions) {
      SnakeTurn turn = orchestratorService.getOrInitTurn(draft.getId(), region, participantIds);
      if (firstTurn == null) {
        firstTurn = turn;
      }
    }

    return SnakeTurnResponse.from(draft.getId(), firstRegion, firstTurn);
  }

  /**
   * Returns the current snake turn for the given game and region without modifying state.
   *
   * @param gameId the game identifier
   * @param region the region label (e.g. {@code "GLOBAL"}, {@code "EU"})
   * @return the current SnakeTurnResponse, or empty if no cursor exists yet
   */
  @Transactional(readOnly = true)
  public Optional<SnakeTurnResponse> getCurrentTurn(UUID gameId, String region) {
    Optional<Draft> draftOpt = draftDomainRepository.findActiveByGameId(gameId);
    if (draftOpt.isEmpty()) {
      return Optional.empty();
    }
    Draft draft = draftOpt.get();
    return orchestratorService
        .getCurrentTurn(draft.getId(), region)
        .map(turn -> SnakeTurnResponse.from(draft.getId(), region, turn));
  }

  /**
   * Validates that it is the user's turn, advances the cursor, and broadcasts the next turn.
   *
   * @param gameId the game identifier
   * @param userId the user submitting the pick
   * @param region the region for this pick
   * @return the next SnakeTurnResponse after advancing
   * @throws NotYourTurnException if it is not the user's turn
   * @throws InvalidDraftStateException if no active draft or cursor exists
   */
  public SnakeTurnResponse validateAndAdvance(UUID gameId, UUID userId, String region) {
    Draft draft = findActiveDraftOrThrow(gameId);

    SnakeTurn currentTurn =
        orchestratorService
            .getCurrentTurn(draft.getId(), region)
            .orElseThrow(
                () -> new InvalidDraftStateException("No active cursor for region: " + region));

    if (!currentTurn.participantId().equals(userId)) {
      throw new NotYourTurnException("It is not your turn to pick");
    }

    SnakeTurn nextTurn =
        orchestratorService
            .advance(draft.getId(), region)
            .orElseThrow(
                () ->
                    new InvalidDraftStateException(
                        "Failed to advance cursor for region: " + region));

    SnakeTurnResponse nextResponse = SnakeTurnResponse.from(draft.getId(), region, nextTurn);
    messagingTemplate.convertAndSend(TOPIC_PREFIX + draft.getId() + TOPIC_SUFFIX, nextResponse);
    return nextResponse;
  }

  // ===== PRIVATE HELPERS =====

  private Game findGameOrThrow(UUID gameId) {
    return gameDomainRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
  }

  private Draft findActiveDraftOrThrow(UUID gameId) {
    return draftDomainRepository
        .findActiveByGameId(gameId)
        .orElseThrow(() -> new InvalidDraftStateException("No active draft for game: " + gameId));
  }

  private List<UUID> buildShuffledParticipantIds(UUID gameId) {
    List<GameParticipant> participants =
        gameParticipantRepository.findByGameIdWithUserFetch(gameId);
    List<UUID> ids = new ArrayList<>();
    for (GameParticipant p : participants) {
      ids.add(p.getUser().getId());
    }
    Collections.shuffle(ids, random);
    return ids;
  }

  private List<String> resolveRegions(Game game) {
    List<String> regions = game.getRegionRules().stream().map(r -> r.getRegion().name()).toList();
    return regions.isEmpty() ? List.of(GLOBAL_REGION) : regions;
  }
}
