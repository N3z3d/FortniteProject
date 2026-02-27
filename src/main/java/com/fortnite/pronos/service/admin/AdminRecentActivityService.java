package com.fortnite.pronos.service.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.admin.RecentActivityDto;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Trade;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.TradeRepository;
import com.fortnite.pronos.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class AdminRecentActivityService {

  private static final int DEFAULT_HOURS = 24;
  private static final int MAX_RECENT_ENTRIES = 10;
  private static final int TRADE_ID_PREFIX_LENGTH = 8;
  private static final String UNKNOWN_TRADE_ID = "unknown";

  private final GameRepository gameRepository;
  private final TradeRepository tradeRepository;
  private final UserRepository userRepository;

  RecentActivityDto getRecentActivity(int hours) {
    int effectiveHours = hours > 0 ? hours : DEFAULT_HOURS;
    LocalDateTime since = LocalDateTime.now().minusHours(effectiveHours);

    List<Game> recentGames = gameRepository.findByCreatedAtAfter(since);
    List<Trade> recentTrades = filterRecentTrades(since);

    return RecentActivityDto.builder()
        .recentGamesCount(recentGames.size())
        .recentTradesCount(recentTrades.size())
        .recentUsersCount(userRepository.count())
        .recentGames(mapGamesToActivity(recentGames))
        .recentTrades(mapTradesToActivity(recentTrades))
        .build();
  }

  private List<Trade> filterRecentTrades(LocalDateTime since) {
    return tradeRepository.findAll().stream()
        .filter(trade -> trade.getProposedAt() != null && trade.getProposedAt().isAfter(since))
        .toList();
  }

  private List<RecentActivityDto.ActivityEntry> mapGamesToActivity(List<Game> games) {
    return games.stream()
        .limit(MAX_RECENT_ENTRIES)
        .map(
            game ->
                RecentActivityDto.ActivityEntry.builder()
                    .id(game.getId().toString())
                    .name(game.getName())
                    .status(game.getStatus() != null ? game.getStatus().name() : "UNKNOWN")
                    .createdAt(game.getCreatedAt() != null ? game.getCreatedAt().toString() : "")
                    .build())
        .toList();
  }

  private List<RecentActivityDto.ActivityEntry> mapTradesToActivity(List<Trade> trades) {
    return trades.stream()
        .limit(MAX_RECENT_ENTRIES)
        .map(
            trade ->
                RecentActivityDto.ActivityEntry.builder()
                    .id(buildTradeActivityId(trade.getId()))
                    .name("Trade #" + buildTradeDisplayId(trade.getId()))
                    .status(trade.getStatus() != null ? trade.getStatus().name() : "UNKNOWN")
                    .createdAt(
                        trade.getProposedAt() != null ? trade.getProposedAt().toString() : "")
                    .build())
        .toList();
  }

  private String buildTradeDisplayId(UUID tradeId) {
    String tradeIdValue = buildTradeActivityId(tradeId);
    int endIndex = Math.min(TRADE_ID_PREFIX_LENGTH, tradeIdValue.length());
    return tradeIdValue.substring(0, endIndex);
  }

  private String buildTradeActivityId(UUID tradeId) {
    if (tradeId == null) {
      return UNKNOWN_TRADE_ID;
    }
    return tradeId.toString();
  }
}
