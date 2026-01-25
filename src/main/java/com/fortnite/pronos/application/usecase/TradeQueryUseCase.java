package com.fortnite.pronos.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fortnite.pronos.model.Trade;

/** Application use case for Trade query operations. Defines the public API for querying trades. */
public interface TradeQueryUseCase {

  Trade getTrade(UUID tradeId);

  List<Trade> getTeamTradeHistory(UUID teamId);

  List<Trade> getPendingTradesForTeam(UUID teamId);

  List<Trade> getGameTradesByStatus(UUID gameId, Trade.Status status);

  List<Trade> getAllGameTrades(UUID gameId);

  Map<String, Object> getGameTradeStatistics(UUID gameId);
}
