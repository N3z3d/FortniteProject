package com.fortnite.pronos.service.trade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.application.usecase.TradeQueryUseCase;
import com.fortnite.pronos.domain.port.out.TradeDomainRepositoryPort;
import com.fortnite.pronos.domain.trade.model.Trade;
import com.fortnite.pronos.domain.trade.model.TradeStatus;
import com.fortnite.pronos.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for read-only trade queries. Returns pure domain Trade models. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TradeQueryService implements TradeQueryUseCase {

  private final TradeDomainRepositoryPort tradeDomainRepository;

  @Override
  public Trade getTrade(UUID tradeId) {
    log.debug("Getting trade by ID: {}", tradeId);
    return tradeDomainRepository
        .findById(tradeId)
        .orElseThrow(() -> new BusinessException("Trade not found with ID: " + tradeId));
  }

  @Override
  public List<Trade> getTeamTradeHistory(UUID teamId) {
    log.debug("Getting trade history for team {}", teamId);
    return tradeDomainRepository.findByTeamId(teamId);
  }

  @Override
  public List<Trade> getPendingTradesForTeam(UUID teamId) {
    log.debug("Getting pending trades for team {}", teamId);
    return tradeDomainRepository.findPendingTradesForTeam(teamId);
  }

  @Override
  public List<Trade> getGameTradesByStatus(UUID gameId, TradeStatus status) {
    log.debug("Getting trades for game {} with status {}", gameId, status);
    return tradeDomainRepository.findByGameIdAndStatus(gameId, status);
  }

  @Override
  public List<Trade> getAllGameTrades(UUID gameId) {
    log.debug("Getting all trades for game {}", gameId);
    return tradeDomainRepository.findByGameId(gameId);
  }

  @Override
  public Map<String, Object> getGameTradeStatistics(UUID gameId) {
    log.debug("Getting trade statistics for game {}", gameId);
    Map<String, Object> stats = new HashMap<>();

    long accepted = tradeDomainRepository.countByGameIdAndStatus(gameId, TradeStatus.ACCEPTED);
    long pending = tradeDomainRepository.countByGameIdAndStatus(gameId, TradeStatus.PENDING);
    long rejected = tradeDomainRepository.countByGameIdAndStatus(gameId, TradeStatus.REJECTED);
    long total = accepted + pending + rejected;

    stats.put("totalTrades", total);
    stats.put("successfulTrades", accepted);
    stats.put("pendingOffers", pending);
    stats.put("receivedOffers", pending);

    return stats;
  }
}
