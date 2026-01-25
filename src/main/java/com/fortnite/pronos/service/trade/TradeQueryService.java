package com.fortnite.pronos.service.trade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.application.usecase.TradeQueryUseCase;
import com.fortnite.pronos.exception.BusinessException;
import com.fortnite.pronos.model.Trade;
import com.fortnite.pronos.repository.TradeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for read-only trade queries. Extracted from TradingService to respect SRP. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TradeQueryService implements TradeQueryUseCase {

  private final TradeRepository tradeRepository;

  /**
   * Get a specific trade by ID
   *
   * @param tradeId ID of the trade
   * @return The trade
   * @throws BusinessException if trade not found
   */
  public Trade getTrade(UUID tradeId) {
    log.debug("Getting trade by ID: {}", tradeId);
    return tradeRepository
        .findById(tradeId)
        .orElseThrow(() -> new BusinessException("Trade not found with ID: " + tradeId));
  }

  /**
   * Get trade history for a team
   *
   * @param teamId ID of the team
   * @return List of trades involving the team
   */
  public List<Trade> getTeamTradeHistory(UUID teamId) {
    log.debug("Getting trade history for team {}", teamId);
    return tradeRepository.findByTeamId(teamId);
  }

  /**
   * Get pending trades for a team
   *
   * @param teamId ID of the team
   * @return List of pending trades for the team
   */
  public List<Trade> getPendingTradesForTeam(UUID teamId) {
    log.debug("Getting pending trades for team {}", teamId);
    return tradeRepository.findPendingTradesForTeam(teamId);
  }

  /**
   * Get all trades for a game filtered by status
   *
   * @param gameId ID of the game
   * @param status Status to filter by
   * @return List of trades with the specified status
   */
  public List<Trade> getGameTradesByStatus(UUID gameId, Trade.Status status) {
    log.debug("Getting trades for game {} with status {}", gameId, status);
    List<Trade> trades = tradeRepository.findByGameIdAndStatus(gameId, status);
    // Initialize lazy collections within transaction
    trades.forEach(this::initializeTradeCollections);
    return trades;
  }

  /**
   * Get all trades for a game
   *
   * @param gameId ID of the game
   * @return List of all trades for the game
   */
  public List<Trade> getAllGameTrades(UUID gameId) {
    log.debug("Getting all trades for game {}", gameId);
    List<Trade> trades = tradeRepository.findByGameId(gameId);
    // Initialize lazy collections within transaction to avoid LazyInitializationException
    trades.forEach(this::initializeTradeCollections);
    return trades;
  }

  /** Initialize lazy collections on a trade to avoid LazyInitializationException */
  private void initializeTradeCollections(Trade trade) {
    if (trade.getOfferedPlayers() != null) {
      trade.getOfferedPlayers().size(); // Force initialization
    }
    if (trade.getRequestedPlayers() != null) {
      trade.getRequestedPlayers().size(); // Force initialization
    }
    // Initialize team owners for DTO mapping
    if (trade.getFromTeam() != null && trade.getFromTeam().getOwner() != null) {
      trade.getFromTeam().getOwner().getUsername();
    }
    if (trade.getToTeam() != null && trade.getToTeam().getOwner() != null) {
      trade.getToTeam().getOwner().getUsername();
    }
  }

  /**
   * Get trade statistics for a game
   *
   * @param gameId ID of the game
   * @return Map of trade statistics matching frontend TradeStats interface
   */
  public Map<String, Object> getGameTradeStatistics(UUID gameId) {
    log.debug("Getting trade statistics for game {}", gameId);
    Map<String, Object> stats = new HashMap<>();

    Long accepted = tradeRepository.countByGameIdAndStatus(gameId, Trade.Status.ACCEPTED);
    Long pending = tradeRepository.countByGameIdAndStatus(gameId, Trade.Status.PENDING);
    Long rejected = tradeRepository.countByGameIdAndStatus(gameId, Trade.Status.REJECTED);
    Long total = accepted + pending + rejected;

    // Match frontend TradeStats interface
    stats.put("totalTrades", total);
    stats.put("successfulTrades", accepted);
    stats.put("pendingOffers", pending);
    stats.put("receivedOffers", pending); // Same as pending for now

    return stats;
  }
}
