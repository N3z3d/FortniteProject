package com.fortnite.pronos.service.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.exception.BusinessException;
import com.fortnite.pronos.model.Trade;
import com.fortnite.pronos.repository.TradeRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeQueryService - TDD Tests")
class TradeQueryServiceTddTest {

  @Mock private TradeRepository tradeRepository;

  @InjectMocks private TradeQueryService tradeQueryService;

  @Test
  @DisplayName("getTrade returns trade when found")
  void getTradeReturnsTradeWhenFound() {
    UUID tradeId = UUID.randomUUID();
    Trade trade = new Trade();
    trade.setId(tradeId);

    when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(trade));

    Trade result = tradeQueryService.getTrade(tradeId);

    assertThat(result).isEqualTo(trade);
  }

  @Test
  @DisplayName("getTrade throws exception when not found")
  void getTradeThrowsExceptionWhenNotFound() {
    UUID tradeId = UUID.randomUUID();

    when(tradeRepository.findById(tradeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tradeQueryService.getTrade(tradeId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Trade not found");
  }

  @Test
  @DisplayName("getTeamTradeHistory returns trades for team")
  void getTeamTradeHistoryReturnsTradesForTeam() {
    UUID teamId = UUID.randomUUID();
    Trade trade1 = new Trade();
    Trade trade2 = new Trade();
    List<Trade> trades = List.of(trade1, trade2);

    when(tradeRepository.findByTeamId(teamId)).thenReturn(trades);

    List<Trade> result = tradeQueryService.getTeamTradeHistory(teamId);

    assertThat(result).hasSize(2).containsExactly(trade1, trade2);
  }

  @Test
  @DisplayName("getPendingTradesForTeam returns pending trades")
  void getPendingTradesForTeamReturnsPendingTrades() {
    UUID teamId = UUID.randomUUID();
    Trade trade = new Trade();
    trade.setStatus(Trade.Status.PENDING);
    List<Trade> trades = List.of(trade);

    when(tradeRepository.findPendingTradesForTeam(teamId)).thenReturn(trades);

    List<Trade> result = tradeQueryService.getPendingTradesForTeam(teamId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(Trade.Status.PENDING);
  }

  @Test
  @DisplayName("getGameTradesByStatus returns filtered trades")
  void getGameTradesByStatusReturnsFilteredTrades() {
    UUID gameId = UUID.randomUUID();
    Trade trade = new Trade();
    trade.setStatus(Trade.Status.ACCEPTED);
    List<Trade> trades = List.of(trade);

    when(tradeRepository.findByGameIdAndStatus(gameId, Trade.Status.ACCEPTED)).thenReturn(trades);

    List<Trade> result = tradeQueryService.getGameTradesByStatus(gameId, Trade.Status.ACCEPTED);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(Trade.Status.ACCEPTED);
  }

  @Test
  @DisplayName("getAllGameTrades returns all trades for game")
  void getAllGameTradesReturnsAllTradesForGame() {
    UUID gameId = UUID.randomUUID();
    Trade trade1 = new Trade();
    Trade trade2 = new Trade();
    List<Trade> trades = List.of(trade1, trade2);

    when(tradeRepository.findByGameId(gameId)).thenReturn(trades);

    List<Trade> result = tradeQueryService.getAllGameTrades(gameId);

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("getGameTradeStatistics returns correct counts")
  void getGameTradeStatisticsReturnsCorrectCounts() {
    UUID gameId = UUID.randomUUID();

    when(tradeRepository.countByGameIdAndStatus(gameId, Trade.Status.ACCEPTED)).thenReturn(5L);
    when(tradeRepository.countByGameIdAndStatus(gameId, Trade.Status.PENDING)).thenReturn(3L);
    when(tradeRepository.countByGameIdAndStatus(gameId, Trade.Status.REJECTED)).thenReturn(2L);

    Map<String, Long> stats = tradeQueryService.getGameTradeStatistics(gameId);

    assertThat(stats)
        .containsEntry("accepted", 5L)
        .containsEntry("pending", 3L)
        .containsEntry("rejected", 2L)
        .containsEntry("total", 10L);
  }

  @Test
  @DisplayName("getTeamTradeHistory returns empty list when no trades")
  void getTeamTradeHistoryReturnsEmptyListWhenNoTrades() {
    UUID teamId = UUID.randomUUID();

    when(tradeRepository.findByTeamId(teamId)).thenReturn(List.of());

    List<Trade> result = tradeQueryService.getTeamTradeHistory(teamId);

    assertThat(result).isEmpty();
  }
}
