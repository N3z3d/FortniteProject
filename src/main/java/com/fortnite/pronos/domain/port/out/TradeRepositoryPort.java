package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.model.Trade;

/**
 * Output port for Trade persistence operations. Implemented by the persistence adapter
 * (TradeRepository).
 */
public interface TradeRepositoryPort {

  Optional<Trade> findById(UUID id);

  Trade save(Trade trade);

  List<Trade> findByTeamId(UUID teamId);

  List<Trade> findPendingTradesForTeam(UUID teamId);

  Long countByGameIdAndStatus(UUID gameId, Trade.Status status);

  List<Trade> findByGameId(UUID gameId);

  List<Trade> findByGameIdAndStatus(UUID gameId, Trade.Status status);

  List<Trade> findTradesBetweenTeams(UUID teamId1, UUID teamId2);
}
