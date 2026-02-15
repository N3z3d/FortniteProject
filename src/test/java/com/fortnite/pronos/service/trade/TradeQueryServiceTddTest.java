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

import com.fortnite.pronos.domain.port.out.TradeDomainRepositoryPort;
import com.fortnite.pronos.domain.trade.model.Trade;
import com.fortnite.pronos.domain.trade.model.TradeStatus;
import com.fortnite.pronos.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeQueryService - TDD Tests")
class TradeQueryServiceTddTest {

  @Mock private TradeDomainRepositoryPort tradeDomainRepository;

  @InjectMocks private TradeQueryService tradeQueryService;

  @Test
  @DisplayName("getTrade returns domain trade when found")
  void getTradeReturnsDomainTradeWhenFound() {
    UUID tradeId = UUID.randomUUID();
    Trade domainTrade = domainTrade(tradeId, TradeStatus.PENDING);

    when(tradeDomainRepository.findById(tradeId)).thenReturn(Optional.of(domainTrade));

    Trade result = tradeQueryService.getTrade(tradeId);

    assertThat(result).isEqualTo(domainTrade);
    assertThat(result.getStatus()).isEqualTo(TradeStatus.PENDING);
  }

  @Test
  @DisplayName("getTrade throws exception when not found")
  void getTradeThrowsExceptionWhenNotFound() {
    UUID tradeId = UUID.randomUUID();

    when(tradeDomainRepository.findById(tradeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tradeQueryService.getTrade(tradeId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Trade not found");
  }

  @Test
  @DisplayName("getTeamTradeHistory returns domain trades for team")
  void getTeamTradeHistoryReturnsDomainTradesForTeam() {
    UUID teamId = UUID.randomUUID();
    UUID tradeId1 = UUID.randomUUID();
    UUID tradeId2 = UUID.randomUUID();
    List<Trade> domainTrades =
        List.of(
            domainTrade(tradeId1, TradeStatus.PENDING),
            domainTrade(tradeId2, TradeStatus.ACCEPTED));

    when(tradeDomainRepository.findByTeamId(teamId)).thenReturn(domainTrades);

    List<Trade> result = tradeQueryService.getTeamTradeHistory(teamId);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getId()).isEqualTo(tradeId1);
    assertThat(result.get(1).getId()).isEqualTo(tradeId2);
  }

  @Test
  @DisplayName("getPendingTradesForTeam returns pending domain trades")
  void getPendingTradesForTeamReturnsPendingDomainTrades() {
    UUID teamId = UUID.randomUUID();
    UUID tradeId = UUID.randomUUID();
    List<Trade> domainTrades = List.of(domainTrade(tradeId, TradeStatus.PENDING));

    when(tradeDomainRepository.findPendingTradesForTeam(teamId)).thenReturn(domainTrades);

    List<Trade> result = tradeQueryService.getPendingTradesForTeam(teamId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(TradeStatus.PENDING);
  }

  @Test
  @DisplayName("getGameTradesByStatus returns filtered domain trades")
  void getGameTradesByStatusReturnsFilteredDomainTrades() {
    UUID gameId = UUID.randomUUID();
    UUID tradeId = UUID.randomUUID();
    List<Trade> domainTrades = List.of(domainTrade(tradeId, TradeStatus.ACCEPTED));

    when(tradeDomainRepository.findByGameIdAndStatus(gameId, TradeStatus.ACCEPTED))
        .thenReturn(domainTrades);

    List<Trade> result = tradeQueryService.getGameTradesByStatus(gameId, TradeStatus.ACCEPTED);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(TradeStatus.ACCEPTED);
  }

  @Test
  @DisplayName("getAllGameTrades returns all domain trades for game")
  void getAllGameTradesReturnsAllDomainTradesForGame() {
    UUID gameId = UUID.randomUUID();
    UUID tradeId1 = UUID.randomUUID();
    UUID tradeId2 = UUID.randomUUID();
    List<Trade> domainTrades =
        List.of(
            domainTrade(tradeId1, TradeStatus.REJECTED),
            domainTrade(tradeId2, TradeStatus.PENDING));

    when(tradeDomainRepository.findByGameId(gameId)).thenReturn(domainTrades);

    List<Trade> result = tradeQueryService.getAllGameTrades(gameId);

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("getGameTradeStatistics returns correct counts")
  void getGameTradeStatisticsReturnsCorrectCounts() {
    UUID gameId = UUID.randomUUID();

    when(tradeDomainRepository.countByGameIdAndStatus(gameId, TradeStatus.ACCEPTED)).thenReturn(5L);
    when(tradeDomainRepository.countByGameIdAndStatus(gameId, TradeStatus.PENDING)).thenReturn(3L);
    when(tradeDomainRepository.countByGameIdAndStatus(gameId, TradeStatus.REJECTED)).thenReturn(2L);

    Map<String, Object> stats = tradeQueryService.getGameTradeStatistics(gameId);

    assertThat(stats)
        .containsEntry("successfulTrades", 5L)
        .containsEntry("pendingOffers", 3L)
        .containsEntry("receivedOffers", 3L)
        .containsEntry("totalTrades", 10L);
  }

  @Test
  @DisplayName("getGameTradeStatistics returns zeros when no trades")
  void getGameTradeStatisticsReturnsZerosWhenNoTrades() {
    UUID gameId = UUID.randomUUID();

    when(tradeDomainRepository.countByGameIdAndStatus(gameId, TradeStatus.ACCEPTED)).thenReturn(0L);
    when(tradeDomainRepository.countByGameIdAndStatus(gameId, TradeStatus.PENDING)).thenReturn(0L);
    when(tradeDomainRepository.countByGameIdAndStatus(gameId, TradeStatus.REJECTED)).thenReturn(0L);

    Map<String, Object> stats = tradeQueryService.getGameTradeStatistics(gameId);

    assertThat(stats)
        .containsEntry("successfulTrades", 0L)
        .containsEntry("pendingOffers", 0L)
        .containsEntry("receivedOffers", 0L)
        .containsEntry("totalTrades", 0L);
  }

  @Test
  @DisplayName("getTeamTradeHistory returns empty list when no trades")
  void getTeamTradeHistoryReturnsEmptyListWhenNoTrades() {
    UUID teamId = UUID.randomUUID();

    when(tradeDomainRepository.findByTeamId(teamId)).thenReturn(List.of());

    List<Trade> result = tradeQueryService.getTeamTradeHistory(teamId);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("getGameTradesByStatus handles all domain statuses")
  void getGameTradesByStatusHandlesAllDomainStatuses() {
    UUID gameId = UUID.randomUUID();

    for (TradeStatus status : TradeStatus.values()) {
      UUID tradeId = UUID.randomUUID();
      when(tradeDomainRepository.findByGameIdAndStatus(gameId, status))
          .thenReturn(List.of(domainTrade(tradeId, status)));

      List<Trade> result = tradeQueryService.getGameTradesByStatus(gameId, status);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getStatus()).isEqualTo(status);
    }
  }

  private Trade domainTrade(UUID id, TradeStatus status) {
    return Trade.restore(
        id,
        UUID.randomUUID(),
        UUID.randomUUID(),
        List.of(UUID.randomUUID()),
        List.of(UUID.randomUUID()),
        status,
        java.time.LocalDateTime.now(),
        null,
        null,
        null,
        null);
  }
}
