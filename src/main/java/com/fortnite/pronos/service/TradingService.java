package com.fortnite.pronos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.domain.port.out.TeamRepositoryPort;
import com.fortnite.pronos.domain.port.out.TradeRepositoryPort;
import com.fortnite.pronos.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TradingService {

  private static final String TRADE_NOT_FOUND_MESSAGE = "Trade not found";
  private static final String TRADE_NOT_PENDING_MESSAGE = "Trade is not in pending status";

  private final TradeRepositoryPort tradeRepository;
  private final TeamRepositoryPort teamRepository;
  private final PlayerRepositoryPort playerRepository;
  private final ValidationService validationService;
  private final TradeNotificationService tradeNotificationService;

  /**
   * Propose a trade between two teams (with UUID lists)
   *
   * @param fromTeamId ID of the team making the offer
   * @param toTeamId ID of the team receiving the offer
   * @param offeredPlayerIds IDs of players being offered
   * @param requestedPlayerIds IDs of players being requested
   * @return The created trade proposal
   */
  public com.fortnite.pronos.model.Trade proposeTradeWithPlayerIds(
      UUID fromTeamId, UUID toTeamId, List<UUID> offeredPlayerIds, List<UUID> requestedPlayerIds) {
    log.info("Proposing trade from team {} to team {}", fromTeamId, toTeamId);

    // Convert UUIDs to Player entities
    List<com.fortnite.pronos.model.Player> offeredPlayers =
        offeredPlayerIds.stream()
            .map(
                id ->
                    playerRepository
                        .findById(id)
                        .orElseThrow(
                            () -> new BusinessException("Offered player not found: " + id)))
            .toList();

    List<com.fortnite.pronos.model.Player> requestedPlayers =
        requestedPlayerIds.stream()
            .map(
                id ->
                    playerRepository
                        .findById(id)
                        .orElseThrow(
                            () -> new BusinessException("Requested player not found: " + id)))
            .toList();

    return proposeTrade(fromTeamId, toTeamId, offeredPlayers, requestedPlayers);
  }

  /**
   * Propose a trade between two teams
   *
   * @param fromTeamId ID of the team making the offer
   * @param toTeamId ID of the team receiving the offer
   * @param offeredPlayers Players being offered
   * @param requestedPlayers Players being requested
   * @return The created trade proposal
   */
  public com.fortnite.pronos.model.Trade proposeTrade(
      UUID fromTeamId,
      UUID toTeamId,
      List<com.fortnite.pronos.model.Player> offeredPlayers,
      List<com.fortnite.pronos.model.Player> requestedPlayers) {
    log.info("Proposing trade from team {} to team {}", fromTeamId, toTeamId);

    com.fortnite.pronos.model.Team fromTeam = findTeamOrThrow(fromTeamId, "From team not found");
    com.fortnite.pronos.model.Team toTeam = findTeamOrThrow(toTeamId, "To team not found");

    validateGameTradingRules(fromTeam, toTeam);
    validatePlayerOwnershipAndLocks(fromTeam, offeredPlayers, "Team does not own offered player: ");
    validatePlayerOwnershipAndLocks(
        toTeam, requestedPlayers, "Target team does not own requested player: ");
    validateTradeSize(offeredPlayers, requestedPlayers);

    com.fortnite.pronos.model.Trade trade =
        buildPendingTrade(fromTeam, toTeam, offeredPlayers, requestedPlayers);
    return saveAndNotifyTradeProposed(trade);
  }

  /**
   * Accept a trade proposal
   *
   * @param tradeId ID of the trade to accept
   * @param userId ID of the user accepting the trade
   * @return The accepted trade
   */
  public com.fortnite.pronos.model.Trade acceptTrade(UUID tradeId, UUID userId) {
    log.info("Accepting trade {} by user {}", tradeId, userId);

    com.fortnite.pronos.model.Trade trade = loadTradeOrThrow(tradeId);
    validateTradeAcceptanceRights(trade, userId);

    validatePostTradeComposition(trade);
    executePlayerSwap(trade);
    finalizeAcceptedTrade(trade);
    return saveTeamsAndTradeAndNotifyAccepted(trade);
  }

  /**
   * Reject a trade proposal
   *
   * @param tradeId ID of the trade to reject
   * @param userId ID of the user rejecting the trade
   * @return The rejected trade
   */
  public com.fortnite.pronos.model.Trade rejectTrade(UUID tradeId, UUID userId) {
    log.info("Rejecting trade {} by user {}", tradeId, userId);

    com.fortnite.pronos.model.Trade trade =
        tradeRepository
            .findById(tradeId)
            .orElseThrow(() -> new BusinessException(TRADE_NOT_FOUND_MESSAGE));

    // Validate user can reject this trade
    if (!teamOwnerId(toTeamOf(trade)).equals(userId)) {
      throw new BusinessException("Only the target team owner can reject this trade");
    }

    // Validate trade is pending
    if (trade.getStatus() != com.fortnite.pronos.model.Trade.Status.PENDING) {
      throw new BusinessException(TRADE_NOT_PENDING_MESSAGE);
    }

    trade.setStatus(com.fortnite.pronos.model.Trade.Status.REJECTED);
    trade.setRejectedAt(LocalDateTime.now());

    com.fortnite.pronos.model.Trade savedTrade = tradeRepository.save(trade);
    tradeNotificationService.notifyTradeRejected(savedTrade);
    return savedTrade;
  }

  /**
   * Cancel a trade proposal
   *
   * @param tradeId ID of the trade to cancel
   * @param userId ID of the user canceling the trade
   * @return The canceled trade
   */
  public com.fortnite.pronos.model.Trade cancelTrade(UUID tradeId, UUID userId) {
    log.info("Canceling trade {} by user {}", tradeId, userId);

    com.fortnite.pronos.model.Trade trade =
        tradeRepository
            .findById(tradeId)
            .orElseThrow(() -> new BusinessException(TRADE_NOT_FOUND_MESSAGE));

    // Validate user can cancel this trade
    if (!teamOwnerId(fromTeamOf(trade)).equals(userId)) {
      throw new BusinessException("Only the initiating team owner can cancel this trade");
    }

    // Validate trade is pending
    if (trade.getStatus() != com.fortnite.pronos.model.Trade.Status.PENDING) {
      throw new BusinessException(TRADE_NOT_PENDING_MESSAGE);
    }

    trade.setStatus(com.fortnite.pronos.model.Trade.Status.CANCELLED);
    trade.setCancelledAt(LocalDateTime.now());

    com.fortnite.pronos.model.Trade savedTrade = tradeRepository.save(trade);
    tradeNotificationService.notifyTradeCancelled(savedTrade);
    return savedTrade;
  }

  /**
   * Create a counter-offer for a trade (with UUID lists)
   *
   * @param originalTradeId ID of the original trade
   * @param userId ID of the user making the counter-offer
   * @param offeredPlayerIds IDs of players being offered in the counter
   * @param requestedPlayerIds IDs of players being requested in the counter
   * @return The counter trade proposal
   */
  public com.fortnite.pronos.model.Trade counterTradeWithPlayerIds(
      UUID originalTradeId,
      UUID userId,
      List<UUID> offeredPlayerIds,
      List<UUID> requestedPlayerIds) {
    // Convert UUIDs to Player entities
    List<com.fortnite.pronos.model.Player> offeredPlayers =
        offeredPlayerIds.stream()
            .map(
                id ->
                    playerRepository
                        .findById(id)
                        .orElseThrow(
                            () -> new BusinessException("Offered player not found: " + id)))
            .toList();

    List<com.fortnite.pronos.model.Player> requestedPlayers =
        requestedPlayerIds.stream()
            .map(
                id ->
                    playerRepository
                        .findById(id)
                        .orElseThrow(
                            () -> new BusinessException("Requested player not found: " + id)))
            .toList();

    return counterTrade(originalTradeId, userId, offeredPlayers, requestedPlayers);
  }

  /**
   * Create a counter-offer for a trade
   *
   * @param originalTradeId ID of the original trade
   * @param userId ID of the user making the counter-offer
   * @param offeredPlayers Players being offered in the counter
   * @param requestedPlayers Players being requested in the counter
   * @return The counter trade proposal
   */
  public com.fortnite.pronos.model.Trade counterTrade(
      UUID originalTradeId,
      UUID userId,
      List<com.fortnite.pronos.model.Player> offeredPlayers,
      List<com.fortnite.pronos.model.Player> requestedPlayers) {
    log.info("Creating counter-offer for trade {} by user {}", originalTradeId, userId);

    com.fortnite.pronos.model.Trade originalTrade =
        tradeRepository
            .findById(originalTradeId)
            .orElseThrow(() -> new BusinessException("Original trade not found"));

    // Validate user can counter this trade
    if (!teamOwnerId(toTeamOf(originalTrade)).equals(userId)) {
      throw new BusinessException("Only the target team owner can counter this trade");
    }

    // Validate original trade is pending
    if (originalTrade.getStatus() != com.fortnite.pronos.model.Trade.Status.PENDING) {
      throw new BusinessException("Original trade is not in pending status");
    }

    // Mark original trade as countered
    originalTrade.setStatus(com.fortnite.pronos.model.Trade.Status.COUNTERED);
    com.fortnite.pronos.model.Trade savedOriginal = tradeRepository.save(originalTrade);

    // Create counter-offer (roles are swapped)
    com.fortnite.pronos.model.Trade counterTrade =
        proposeTrade(
            toTeamOf(originalTrade).getId(),
            fromTeamOf(originalTrade).getId(),
            offeredPlayers,
            requestedPlayers);

    counterTrade.setOriginalTradeId(originalTradeId);
    com.fortnite.pronos.model.Trade savedCounter = tradeRepository.save(counterTrade);
    tradeNotificationService.notifyTradeCountered(savedOriginal, savedCounter);
    return savedCounter;
  }

  private void validateGameTradingRules(
      com.fortnite.pronos.model.Team fromTeam, com.fortnite.pronos.model.Team toTeam) {
    if (!gameId(fromTeam).equals(gameId(toTeam))) {
      throw new BusinessException("Teams must be in the same game");
    }
    com.fortnite.pronos.model.Game game = teamGame(fromTeam);
    if (!Boolean.TRUE.equals(game.getTradingEnabled())) {
      throw new BusinessException("Trading is disabled for this game");
    }
    if (game.getTradeDeadline() != null && LocalDateTime.now().isAfter(game.getTradeDeadline())) {
      throw new BusinessException("Trade deadline has passed");
    }
    if (fromTeam.getCompletedTradesCount() >= game.getMaxTradesPerTeam()) {
      throw new BusinessException("Team has reached maximum number of trades");
    }
    if (toTeam.getCompletedTradesCount() >= game.getMaxTradesPerTeam()) {
      throw new BusinessException("Target team has reached maximum number of trades");
    }
  }

  private void validatePlayerOwnershipAndLocks(
      com.fortnite.pronos.model.Team team,
      List<com.fortnite.pronos.model.Player> players,
      String ownershipErrorPrefix) {
    for (com.fortnite.pronos.model.Player player : players) {
      if (!teamOwnsPlayer(team, player)) {
        throw new BusinessException(ownershipErrorPrefix + player.getName());
      }
      if (player.isLocked()) {
        throw new BusinessException("Cannot trade locked player: " + player.getName());
      }
    }
  }

  private void validatePostTradeComposition(com.fortnite.pronos.model.Trade trade) {
    com.fortnite.pronos.model.Team fromTeam = trade.getFromTeam();
    com.fortnite.pronos.model.Team toTeam = trade.getToTeam();

    List<com.fortnite.pronos.model.Player> fromPlayersAfter =
        simulatePlayersAfterTrade(fromTeam, trade.getOfferedPlayers(), trade.getRequestedPlayers());
    List<com.fortnite.pronos.model.Player> toPlayersAfter =
        simulatePlayersAfterTrade(toTeam, trade.getRequestedPlayers(), trade.getOfferedPlayers());

    com.fortnite.pronos.model.Team tempFromTeam = new com.fortnite.pronos.model.Team();
    tempFromTeam.setName(fromTeam.getName());
    tempFromTeam.setPlayers(asTeamPlayers(tempFromTeam, fromPlayersAfter));

    com.fortnite.pronos.model.Team tempToTeam = new com.fortnite.pronos.model.Team();
    tempToTeam.setName(toTeam.getName());
    tempToTeam.setPlayers(asTeamPlayers(tempToTeam, toPlayersAfter));

    validationService.validateTeamComposition(tempFromTeam, regionRules(fromTeam));
    validationService.validateTeamComposition(tempToTeam, regionRules(toTeam));
  }

  private com.fortnite.pronos.model.Team findTeamOrThrow(UUID teamId, String errorMessage) {
    return teamRepository.findById(teamId).orElseThrow(() -> new BusinessException(errorMessage));
  }

  private void validateTradeSize(
      List<com.fortnite.pronos.model.Player> offeredPlayers,
      List<com.fortnite.pronos.model.Player> requestedPlayers) {
    if (offeredPlayers.size() > 5 || requestedPlayers.size() > 5) {
      throw new BusinessException("Too many players in trade (maximum 5 per side)");
    }
  }

  private com.fortnite.pronos.model.Trade buildPendingTrade(
      com.fortnite.pronos.model.Team fromTeam,
      com.fortnite.pronos.model.Team toTeam,
      List<com.fortnite.pronos.model.Player> offeredPlayers,
      List<com.fortnite.pronos.model.Player> requestedPlayers) {
    return com.fortnite.pronos.model.Trade.builder()
        .fromTeam(fromTeam)
        .toTeam(toTeam)
        .offeredPlayers(offeredPlayers)
        .requestedPlayers(requestedPlayers)
        .status(com.fortnite.pronos.model.Trade.Status.PENDING)
        .proposedAt(LocalDateTime.now())
        .build();
  }

  private com.fortnite.pronos.model.Trade saveAndNotifyTradeProposed(
      com.fortnite.pronos.model.Trade trade) {
    com.fortnite.pronos.model.Trade savedTrade = tradeRepository.save(trade);
    tradeNotificationService.notifyTradeProposed(savedTrade);
    return savedTrade;
  }

  private com.fortnite.pronos.model.Trade loadTradeOrThrow(UUID tradeId) {
    return tradeRepository
        .findById(tradeId)
        .orElseThrow(() -> new BusinessException(TRADE_NOT_FOUND_MESSAGE));
  }

  private void validateTradeAcceptanceRights(com.fortnite.pronos.model.Trade trade, UUID userId) {
    if (!teamOwnerId(toTeamOf(trade)).equals(userId)) {
      throw new BusinessException("Only the target team owner can accept this trade");
    }
    if (trade.getStatus() != com.fortnite.pronos.model.Trade.Status.PENDING) {
      throw new BusinessException(TRADE_NOT_PENDING_MESSAGE);
    }
  }

  private com.fortnite.pronos.model.Team fromTeamOf(com.fortnite.pronos.model.Trade trade) {
    return trade.getFromTeam();
  }

  private com.fortnite.pronos.model.Team toTeamOf(com.fortnite.pronos.model.Trade trade) {
    return trade.getToTeam();
  }

  private com.fortnite.pronos.model.Game teamGame(com.fortnite.pronos.model.Team team) {
    return team.getGame();
  }

  private UUID gameId(com.fortnite.pronos.model.Team team) {
    return team.getGameId();
  }

  private UUID teamOwnerId(com.fortnite.pronos.model.Team team) {
    return team.getUserId();
  }

  private List<com.fortnite.pronos.model.GameRegionRule> regionRules(
      com.fortnite.pronos.model.Team team) {
    return team.getGameRegionRules();
  }

  private List<com.fortnite.pronos.model.Player> simulatePlayersAfterTrade(
      com.fortnite.pronos.model.Team team,
      List<com.fortnite.pronos.model.Player> playersToRemove,
      List<com.fortnite.pronos.model.Player> playersToAdd) {
    List<com.fortnite.pronos.model.Player> playersAfterTrade =
        new ArrayList<>(getActivePlayers(team));
    playersAfterTrade.removeAll(playersToRemove);
    playersAfterTrade.addAll(playersToAdd);
    return playersAfterTrade;
  }

  private void executePlayerSwap(com.fortnite.pronos.model.Trade trade) {
    com.fortnite.pronos.model.Team fromTeam = trade.getFromTeam();
    com.fortnite.pronos.model.Team toTeam = trade.getToTeam();
    trade.getOfferedPlayers().forEach(p -> removePlayerFromTeam(fromTeam, p));
    trade.getRequestedPlayers().forEach(p -> removePlayerFromTeam(toTeam, p));
    trade.getRequestedPlayers().forEach(p -> addPlayerToTeam(fromTeam, p));
    trade.getOfferedPlayers().forEach(p -> addPlayerToTeam(toTeam, p));
  }

  private void finalizeAcceptedTrade(com.fortnite.pronos.model.Trade trade) {
    trade.setStatus(com.fortnite.pronos.model.Trade.Status.ACCEPTED);
    trade.setAcceptedAt(LocalDateTime.now());

    com.fortnite.pronos.model.Team fromTeam = trade.getFromTeam();
    com.fortnite.pronos.model.Team toTeam = trade.getToTeam();
    fromTeam.setCompletedTradesCount(fromTeam.getCompletedTradesCount() + 1);
    toTeam.setCompletedTradesCount(toTeam.getCompletedTradesCount() + 1);
  }

  private com.fortnite.pronos.model.Trade saveTeamsAndTradeAndNotifyAccepted(
      com.fortnite.pronos.model.Trade trade) {
    teamRepository.save(trade.getFromTeam());
    teamRepository.save(trade.getToTeam());
    com.fortnite.pronos.model.Trade savedTrade = tradeRepository.save(trade);
    tradeNotificationService.notifyTradeAccepted(savedTrade);
    return savedTrade;
  }

  // Helpers to work with TeamPlayer mapping
  private List<com.fortnite.pronos.model.Player> getActivePlayers(
      com.fortnite.pronos.model.Team team) {
    return team.getPlayers().stream()
        .filter(
            tp -> tp != null && tp.getPlayer() != null && (tp.getUntil() == null || tp.isActive()))
        .map(com.fortnite.pronos.model.TeamPlayer::getPlayer)
        .toList();
  }

  private boolean teamOwnsPlayer(
      com.fortnite.pronos.model.Team team, com.fortnite.pronos.model.Player player) {
    return team.getPlayers().stream()
        .anyMatch(
            tp ->
                tp != null
                    && tp.getPlayer() != null
                    && tp.getPlayer().equals(player)
                    && (tp.getUntil() == null || tp.isActive()));
  }

  private List<com.fortnite.pronos.model.TeamPlayer> asTeamPlayers(
      com.fortnite.pronos.model.Team team, List<com.fortnite.pronos.model.Player> players) {
    List<com.fortnite.pronos.model.TeamPlayer> result = new ArrayList<>();
    int position = 1;
    for (com.fortnite.pronos.model.Player p : players) {
      com.fortnite.pronos.model.TeamPlayer tp = new com.fortnite.pronos.model.TeamPlayer();
      tp.setTeam(team);
      tp.setPlayer(p);
      tp.setPosition(position++);
      result.add(tp);
    }
    return result;
  }

  private void removePlayerFromTeam(
      com.fortnite.pronos.model.Team team, com.fortnite.pronos.model.Player player) {
    team.getPlayers()
        .removeIf(tp -> tp != null && tp.getPlayer() != null && tp.getPlayer().equals(player));
  }

  private void addPlayerToTeam(
      com.fortnite.pronos.model.Team team, com.fortnite.pronos.model.Player player) {
    com.fortnite.pronos.model.TeamPlayer tp = new com.fortnite.pronos.model.TeamPlayer();
    tp.setTeam(team);
    tp.setPlayer(player);
    tp.setPosition(team.getPlayers().size() + 1);
    team.getPlayers().add(tp);
  }
}
