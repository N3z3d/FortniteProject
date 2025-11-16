package com.fortnite.pronos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.exception.BusinessException;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TradingService {

  private final TradeRepository tradeRepository;
  private final TeamRepository teamRepository;
  private final PlayerRepository playerRepository;
  private final GameRepository gameRepository;
  private final ValidationService validationService;

  /**
   * Propose a trade between two teams (with UUID lists)
   *
   * @param fromTeamId ID of the team making the offer
   * @param toTeamId ID of the team receiving the offer
   * @param offeredPlayerIds IDs of players being offered
   * @param requestedPlayerIds IDs of players being requested
   * @return The created trade proposal
   */
  public Trade proposeTradeWithPlayerIds(
      UUID fromTeamId, UUID toTeamId, List<UUID> offeredPlayerIds, List<UUID> requestedPlayerIds) {
    log.info("Proposing trade from team {} to team {}", fromTeamId, toTeamId);

    // Convert UUIDs to Player entities
    List<Player> offeredPlayers =
        offeredPlayerIds.stream()
            .map(
                id ->
                    playerRepository
                        .findById(id)
                        .orElseThrow(
                            () -> new BusinessException("Offered player not found: " + id)))
            .toList();

    List<Player> requestedPlayers =
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
  public Trade proposeTrade(
      UUID fromTeamId, UUID toTeamId, List<Player> offeredPlayers, List<Player> requestedPlayers) {
    log.info("Proposing trade from team {} to team {}", fromTeamId, toTeamId);

    // Validate basic requirements
    Team fromTeam =
        teamRepository
            .findById(fromTeamId)
            .orElseThrow(() -> new BusinessException("From team not found"));
    Team toTeam =
        teamRepository
            .findById(toTeamId)
            .orElseThrow(() -> new BusinessException("To team not found"));

    // Validate same game
    if (!fromTeam.getGame().getId().equals(toTeam.getGame().getId())) {
      throw new BusinessException("Teams must be in the same game");
    }

    Game game = fromTeam.getGame();

    // Validate trading is enabled
    if (!Boolean.TRUE.equals(game.getTradingEnabled())) {
      throw new BusinessException("Trading is disabled for this game");
    }

    // Validate trade deadline
    if (game.getTradeDeadline() != null && LocalDateTime.now().isAfter(game.getTradeDeadline())) {
      throw new BusinessException("Trade deadline has passed");
    }

    // Validate max trades per team
    if (fromTeam.getCompletedTradesCount() >= game.getMaxTradesPerTeam()) {
      throw new BusinessException("Team has reached maximum number of trades");
    }
    if (toTeam.getCompletedTradesCount() >= game.getMaxTradesPerTeam()) {
      throw new BusinessException("Target team has reached maximum number of trades");
    }

    // Validate player ownership
    for (Player player : offeredPlayers) {
      if (!fromTeam.getPlayers().contains(player)) {
        throw new BusinessException("Team does not own offered player: " + player.getName());
      }
      if (player.isLocked()) {
        throw new BusinessException("Cannot trade locked player: " + player.getName());
      }
    }

    for (Player player : requestedPlayers) {
      if (!toTeam.getPlayers().contains(player)) {
        throw new BusinessException(
            "Target team does not own requested player: " + player.getName());
      }
      if (player.isLocked()) {
        throw new BusinessException("Cannot trade locked player: " + player.getName());
      }
    }

    // Validate max players per trade (5 is reasonable limit)
    if (offeredPlayers.size() > 5 || requestedPlayers.size() > 5) {
      throw new BusinessException("Too many players in trade (maximum 5 per side)");
    }

    // Create the trade
    Trade trade =
        Trade.builder()
            .fromTeam(fromTeam)
            .toTeam(toTeam)
            .offeredPlayers(offeredPlayers)
            .requestedPlayers(requestedPlayers)
            .status(Trade.Status.PENDING)
            .proposedAt(LocalDateTime.now())
            .build();

    return tradeRepository.save(trade);
  }

  /**
   * Accept a trade proposal
   *
   * @param tradeId ID of the trade to accept
   * @param userId ID of the user accepting the trade
   * @return The accepted trade
   */
  public Trade acceptTrade(UUID tradeId, UUID userId) {
    log.info("Accepting trade {} by user {}", tradeId, userId);

    Trade trade =
        tradeRepository
            .findById(tradeId)
            .orElseThrow(() -> new BusinessException("Trade not found"));

    // Validate user can accept this trade
    if (!trade.getToTeam().getUser().getId().equals(userId)) {
      throw new BusinessException("Only the target team owner can accept this trade");
    }

    // Validate trade is pending
    if (trade.getStatus() != Trade.Status.PENDING) {
      throw new BusinessException("Trade is not in pending status");
    }

    // Validate regional rules after trade
    Team fromTeam = trade.getFromTeam();
    Team toTeam = trade.getToTeam();

    // Create copies of player lists to simulate the trade
    List<Player> fromTeamPlayersAfterTrade = new ArrayList<>(fromTeam.getPlayers());
    fromTeamPlayersAfterTrade.removeAll(trade.getOfferedPlayers());
    fromTeamPlayersAfterTrade.addAll(trade.getRequestedPlayers());

    List<Player> toTeamPlayersAfterTrade = new ArrayList<>(toTeam.getPlayers());
    toTeamPlayersAfterTrade.removeAll(trade.getRequestedPlayers());
    toTeamPlayersAfterTrade.addAll(trade.getOfferedPlayers());

    // Create temporary team objects for validation
    Team tempFromTeam = new Team();
    tempFromTeam.setName(fromTeam.getName());
    tempFromTeam.setPlayers(fromTeamPlayersAfterTrade);

    Team tempToTeam = new Team();
    tempToTeam.setName(toTeam.getName());
    tempToTeam.setPlayers(toTeamPlayersAfterTrade);

    // Validate team compositions with simulated state
    validationService.validateTeamComposition(tempFromTeam, fromTeam.getGame().getRegionRules());
    validationService.validateTeamComposition(tempToTeam, toTeam.getGame().getRegionRules());

    // Execute the actual trade
    // Remove offered players from fromTeam and add requested players
    fromTeam.getPlayers().removeAll(trade.getOfferedPlayers());
    fromTeam.getPlayers().addAll(trade.getRequestedPlayers());

    // Remove requested players from toTeam and add offered players
    toTeam.getPlayers().removeAll(trade.getRequestedPlayers());
    toTeam.getPlayers().addAll(trade.getOfferedPlayers());

    trade.setStatus(Trade.Status.ACCEPTED);
    trade.setAcceptedAt(LocalDateTime.now());

    // Update trade counters
    fromTeam.setCompletedTradesCount(fromTeam.getCompletedTradesCount() + 1);
    toTeam.setCompletedTradesCount(toTeam.getCompletedTradesCount() + 1);

    // Save everything
    teamRepository.save(fromTeam);
    teamRepository.save(toTeam);
    return tradeRepository.save(trade);
  }

  /**
   * Reject a trade proposal
   *
   * @param tradeId ID of the trade to reject
   * @param userId ID of the user rejecting the trade
   * @return The rejected trade
   */
  public Trade rejectTrade(UUID tradeId, UUID userId) {
    log.info("Rejecting trade {} by user {}", tradeId, userId);

    Trade trade =
        tradeRepository
            .findById(tradeId)
            .orElseThrow(() -> new BusinessException("Trade not found"));

    // Validate user can reject this trade
    if (!trade.getToTeam().getUser().getId().equals(userId)) {
      throw new BusinessException("Only the target team owner can reject this trade");
    }

    // Validate trade is pending
    if (trade.getStatus() != Trade.Status.PENDING) {
      throw new BusinessException("Trade is not in pending status");
    }

    trade.setStatus(Trade.Status.REJECTED);
    trade.setRejectedAt(LocalDateTime.now());

    return tradeRepository.save(trade);
  }

  /**
   * Cancel a trade proposal
   *
   * @param tradeId ID of the trade to cancel
   * @param userId ID of the user canceling the trade
   * @return The canceled trade
   */
  public Trade cancelTrade(UUID tradeId, UUID userId) {
    log.info("Canceling trade {} by user {}", tradeId, userId);

    Trade trade =
        tradeRepository
            .findById(tradeId)
            .orElseThrow(() -> new BusinessException("Trade not found"));

    // Validate user can cancel this trade
    if (!trade.getFromTeam().getUser().getId().equals(userId)) {
      throw new BusinessException("Only the initiating team owner can cancel this trade");
    }

    // Validate trade is pending
    if (trade.getStatus() != Trade.Status.PENDING) {
      throw new BusinessException("Trade is not in pending status");
    }

    trade.setStatus(Trade.Status.CANCELLED);
    trade.setCancelledAt(LocalDateTime.now());

    return tradeRepository.save(trade);
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
  public Trade counterTradeWithPlayerIds(
      UUID originalTradeId,
      UUID userId,
      List<UUID> offeredPlayerIds,
      List<UUID> requestedPlayerIds) {
    // Convert UUIDs to Player entities
    List<Player> offeredPlayers =
        offeredPlayerIds.stream()
            .map(
                id ->
                    playerRepository
                        .findById(id)
                        .orElseThrow(
                            () -> new BusinessException("Offered player not found: " + id)))
            .toList();

    List<Player> requestedPlayers =
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
  public Trade counterTrade(
      UUID originalTradeId,
      UUID userId,
      List<Player> offeredPlayers,
      List<Player> requestedPlayers) {
    log.info("Creating counter-offer for trade {} by user {}", originalTradeId, userId);

    Trade originalTrade =
        tradeRepository
            .findById(originalTradeId)
            .orElseThrow(() -> new BusinessException("Original trade not found"));

    // Validate user can counter this trade
    if (!originalTrade.getToTeam().getUser().getId().equals(userId)) {
      throw new BusinessException("Only the target team owner can counter this trade");
    }

    // Validate original trade is pending
    if (originalTrade.getStatus() != Trade.Status.PENDING) {
      throw new BusinessException("Original trade is not in pending status");
    }

    // Mark original trade as countered
    originalTrade.setStatus(Trade.Status.COUNTERED);
    tradeRepository.save(originalTrade);

    // Create counter-offer (roles are swapped)
    Trade counterTrade =
        proposeTrade(
            originalTrade.getToTeam().getId(),
            originalTrade.getFromTeam().getId(),
            offeredPlayers,
            requestedPlayers);

    counterTrade.setOriginalTradeId(originalTradeId);
    return tradeRepository.save(counterTrade);
  }

  /**
   * Get trade history for a team
   *
   * @param teamId ID of the team
   * @return List of trades involving the team
   */
  @Transactional(readOnly = true)
  public List<Trade> getTeamTradeHistory(UUID teamId) {
    return tradeRepository.findByTeamId(teamId);
  }

  /**
   * Get pending trades for a team
   *
   * @param teamId ID of the team
   * @return List of pending trades for the team
   */
  @Transactional(readOnly = true)
  public List<Trade> getPendingTradesForTeam(UUID teamId) {
    return tradeRepository.findPendingTradesForTeam(teamId);
  }

  /**
   * Get trade statistics for a game
   *
   * @param gameId ID of the game
   * @return Map of trade statistics
   */
  @Transactional(readOnly = true)
  public Map<String, Long> getGameTradeStatistics(UUID gameId) {
    Map<String, Long> stats = new HashMap<>();

    Long accepted = tradeRepository.countByGameIdAndStatus(gameId, Trade.Status.ACCEPTED);
    Long pending = tradeRepository.countByGameIdAndStatus(gameId, Trade.Status.PENDING);
    Long rejected = tradeRepository.countByGameIdAndStatus(gameId, Trade.Status.REJECTED);

    stats.put("accepted", accepted);
    stats.put("pending", pending);
    stats.put("rejected", rejected);
    stats.put("total", accepted + pending + rejected);

    return stats;
  }

  /**
   * Get a specific trade by ID
   *
   * @param tradeId ID of the trade
   * @return The trade
   * @throws BusinessException if trade not found
   */
  @Transactional(readOnly = true)
  public Trade getTrade(UUID tradeId) {
    log.debug("Getting trade by ID: {}", tradeId);
    return tradeRepository
        .findById(tradeId)
        .orElseThrow(() -> new BusinessException("Trade not found with ID: " + tradeId));
  }

  /**
   * Get all trades for a game filtered by status
   *
   * @param gameId ID of the game
   * @param status Status to filter by
   * @return List of trades with the specified status
   */
  @Transactional(readOnly = true)
  public List<Trade> getGameTradesByStatus(UUID gameId, Trade.Status status) {
    log.debug("Getting trades for game {} with status {}", gameId, status);
    return tradeRepository.findByGameIdAndStatus(gameId, status);
  }

  /**
   * Get all trades for a game
   *
   * @param gameId ID of the game
   * @return List of all trades for the game
   */
  @Transactional(readOnly = true)
  public List<Trade> getAllGameTrades(UUID gameId) {
    log.debug("Getting all trades for game {}", gameId);
    return tradeRepository.findByGameId(gameId);
  }
}
