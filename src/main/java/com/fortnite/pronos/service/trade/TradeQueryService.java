package com.fortnite.pronos.service.trade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class TradeQueryService {

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
    return tradeRepository.findByGameIdAndStatus(gameId, status);
  }

  /**
   * Get all trades for a game
   *
   * @param gameId ID of the game
   * @return List of all trades for the game
   */
  public List<Trade> getAllGameTrades(UUID gameId) {
    log.debug("Getting all trades for game {}", gameId);
    return tradeRepository.findByGameId(gameId);
  }

  /**
   * Get trade statistics for a game
   *
   * @param gameId ID of the game
   * @return Map of trade statistics
   */
  public Map<String, Long> getGameTradeStatistics(UUID gameId) {
    log.debug("Getting trade statistics for game {}", gameId);
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
}
