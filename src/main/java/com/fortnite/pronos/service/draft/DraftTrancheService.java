package com.fortnite.pronos.service.draft;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.draft.model.Draft;
import com.fortnite.pronos.domain.draft.model.SnakeTurn;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.PlayerRecommendResponse;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.InvalidTrancheViolationException;
import com.fortnite.pronos.exception.PlayerAlreadySelectedException;

/**
 * Validates tranche floor rules for draft picks and provides player recommendations.
 *
 * <p>Tranche floor formula: {@code requiredFloor = (slot - 1) * trancheSize + 1} where {@code slot
 * = (round - 1) * maxParticipants + pickNumber}.
 *
 * <p>A pick is valid when {@code parseTrancheFloor(player.tranche) >= requiredFloor}. A lower
 * tranche number means a better-ranked player, which is NOT allowed if the floor is higher.
 */
@Service
@Transactional(readOnly = true)
public class DraftTrancheService {

  private final GameDomainRepositoryPort gameDomainRepository;
  private final DraftDomainRepositoryPort draftDomainRepository;
  private final PlayerDomainRepositoryPort playerRepository;
  private final DraftPickRepositoryPort draftPickRepository;
  private final DraftPickOrchestratorService orchestratorService;

  public DraftTrancheService(
      GameDomainRepositoryPort gameDomainRepository,
      DraftDomainRepositoryPort draftDomainRepository,
      PlayerDomainRepositoryPort playerRepository,
      DraftPickRepositoryPort draftPickRepository,
      DraftPickOrchestratorService orchestratorService) {
    this.gameDomainRepository = gameDomainRepository;
    this.draftDomainRepository = draftDomainRepository;
    this.playerRepository = playerRepository;
    this.draftPickRepository = draftPickRepository;
    this.orchestratorService = orchestratorService;
  }

  /**
   * Validates that the given player respects the tranche floor for the current draft slot.
   *
   * <p>No-op when tranches are disabled on the game.
   *
   * @param gameId the game identifier
   * @param region the draft region (e.g. "GLOBAL", "EU")
   * @param playerId the player being picked
   * @throws InvalidTrancheViolationException if the player is better-ranked than the floor allows
   * @throws InvalidDraftStateException if no active draft or cursor exists for the region
   */
  public void validatePick(UUID gameId, String region, UUID playerId) {
    Game game = findGameOrThrow(gameId);
    if (!game.isTranchesEnabled()) {
      return;
    }
    Draft draft = findActiveDraftOrThrow(gameId);
    SnakeTurn turn =
        orchestratorService
            .getCurrentTurn(draft.getId(), region)
            .orElseThrow(
                () -> new InvalidDraftStateException("No active cursor for region: " + region));

    int requiredFloor = computeRequiredFloor(turn, game);
    Player player =
        playerRepository
            .findById(playerId)
            .orElseThrow(() -> new GameNotFoundException("Player not found: " + playerId));

    int playerFloor = parseTrancheFloor(player.getTranche());
    if (playerFloor < requiredFloor) {
      throw new InvalidTrancheViolationException(
          "Tranche violation: player rank "
              + playerFloor
              + " is better than allowed floor "
              + requiredFloor);
    }
  }

  /**
   * Validates tranche rules using a draft ID instead of a game ID.
   *
   * <p>Resolves the game ID from the domain draft and delegates to {@link #validatePick}. Intended
   * for use by the simultaneous draft controller which only has a draft ID available.
   *
   * @param draftId the draft identifier
   * @param region the draft region
   * @param playerId the player being picked
   */
  public void validatePickByDraftId(UUID draftId, String region, UUID playerId) {
    Draft draft =
        draftDomainRepository
            .findById(draftId)
            .orElseThrow(() -> new InvalidDraftStateException("Draft not found: " + draftId));
    requirePlayerNotAlreadyPickedInDraft(draftId, playerId);
    validatePick(draft.getGameId(), region, playerId);
  }

  private void requirePlayerNotAlreadyPickedInDraft(UUID draftId, UUID playerId) {
    Set<UUID> pickedIds = new HashSet<>(draftPickRepository.findPickedPlayerIdsByDraftId(draftId));
    if (pickedIds.contains(playerId)) {
      throw new PlayerAlreadySelectedException("Player is already selected in this draft");
    }
  }

  /**
   * Returns the best available player that conforms to the current slot's tranche floor.
   *
   * <p>Returns empty when tranches are disabled, when there is no active draft/cursor, or when no
   * conforming player is available.
   *
   * @param gameId the game identifier
   * @param region the draft region
   * @return the recommended player wrapped in Optional, or empty
   */
  public Optional<PlayerRecommendResponse> recommendPlayer(UUID gameId, String region) {
    Game game = findGameOrThrow(gameId);
    if (!game.isTranchesEnabled()) {
      return Optional.empty();
    }

    Optional<Draft> draftOpt = draftDomainRepository.findActiveByGameId(gameId);
    if (draftOpt.isEmpty()) {
      return Optional.empty();
    }
    Draft draft = draftOpt.get();

    Optional<SnakeTurn> turnOpt = orchestratorService.getCurrentTurn(draft.getId(), region);
    if (turnOpt.isEmpty()) {
      return Optional.empty();
    }
    SnakeTurn turn = turnOpt.get();

    int requiredFloor = computeRequiredFloor(turn, game);
    Set<UUID> pickedIds =
        new HashSet<>(draftPickRepository.findPickedPlayerIdsByDraftId(draft.getId()));

    List<Player> players = playerRepository.findActivePlayers();
    return players.stream()
        .filter(p -> !pickedIds.contains(p.getId()))
        .filter(p -> parseTrancheFloor(p.getTranche()) >= requiredFloor)
        .min(Comparator.comparingInt(p -> parseTrancheFloor(p.getTranche())))
        .map(PlayerRecommendResponse::from);
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

  private int computeRequiredFloor(SnakeTurn turn, Game game) {
    int slot = (turn.round() - 1) * game.getMaxParticipants() + turn.pickNumber();
    return (slot - 1) * game.getTrancheSize() + 1;
  }

  static int parseTrancheFloor(String tranche) {
    if (tranche == null || tranche.isBlank()) return 1;
    return Integer.parseInt(tranche.split("-")[0]);
  }
}
