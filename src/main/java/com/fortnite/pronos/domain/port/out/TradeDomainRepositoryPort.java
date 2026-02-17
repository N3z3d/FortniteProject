package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.trade.model.Trade;
import com.fortnite.pronos.domain.trade.model.TradeStatus;

/** Output port for Trade domain persistence operations. */
public interface TradeDomainRepositoryPort {

  Optional<Trade> findById(UUID id);

  Trade save(Trade trade);

  List<Trade> findByTeamId(UUID teamId);

  List<Trade> findPendingTradesForTeam(UUID teamId);

  long countByGameIdAndStatus(UUID gameId, TradeStatus status);

  List<Trade> findByGameId(UUID gameId);

  List<Trade> findByGameIdAndStatus(UUID gameId, TradeStatus status);

  List<Trade> findTradesBetweenTeams(UUID teamId1, UUID teamId2);
}
