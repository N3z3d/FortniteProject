package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.TradeRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.admin.RecentActivityDto;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Trade;

@ExtendWith(MockitoExtension.class)
class AdminRecentActivityServiceTest {

  @Mock private GameRepositoryPort gameRepository;
  @Mock private TradeRepositoryPort tradeRepository;
  @Mock private UserRepositoryPort userRepository;

  private AdminRecentActivityService service;

  @BeforeEach
  void setUp() {
    service = new AdminRecentActivityService(gameRepository, tradeRepository, userRepository);
  }

  @Test
  void keepsOnlyTheTenMostRecentGamesInDescendingOrder() {
    when(gameRepository.findByCreatedAtAfter(any()))
        .thenReturn(
            List.of(
                buildGame("Game-03", 3),
                buildGame("Game-01", 1),
                buildGame("Game-12", 12),
                buildGame("Game-02", 2),
                buildGame("Game-11", 11),
                buildGame("Game-08", 8),
                buildGame("Game-05", 5),
                buildGame("Game-04", 4),
                buildGame("Game-10", 10),
                buildGame("Game-09", 9),
                buildGame("Game-06", 6),
                buildGame("Game-07", 7)));
    when(tradeRepository.findAll()).thenReturn(List.of());
    when(userRepository.count()).thenReturn(2L);

    RecentActivityDto result = service.getRecentActivity(24);

    assertThat(result.getRecentGamesCount()).isEqualTo(12);
    assertThat(result.getRecentGames()).hasSize(10);
    assertThat(result.getRecentGames())
        .extracting(RecentActivityDto.ActivityEntry::getName)
        .containsExactly(
            "Game-01", "Game-02", "Game-03", "Game-04", "Game-05", "Game-06", "Game-07", "Game-08",
            "Game-09", "Game-10");
  }

  @Test
  void keepsOnlyTheTenMostRecentTradesInDescendingOrder() {
    when(gameRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
    when(tradeRepository.findAll())
        .thenReturn(
            List.of(
                buildTrade(3),
                buildTrade(1),
                buildTrade(12),
                buildTrade(2),
                buildTrade(11),
                buildTrade(8),
                buildTrade(5),
                buildTrade(4),
                buildTrade(10),
                buildTrade(9),
                buildTrade(6),
                buildTrade(7),
                buildOldTrade(48)));
    when(userRepository.count()).thenReturn(1L);

    RecentActivityDto result = service.getRecentActivity(24);

    assertThat(result.getRecentTradesCount()).isEqualTo(12);
    assertThat(result.getRecentTrades()).hasSize(10);
    assertThat(result.getRecentTrades())
        .extracting(RecentActivityDto.ActivityEntry::getCreatedAt)
        .isSortedAccordingTo(Comparator.reverseOrder());
  }

  private Game buildGame(String name, int hoursAgo) {
    Game game = new Game();
    game.setId(UUID.randomUUID());
    game.setName(name);
    game.setStatus(GameStatus.CREATING);
    game.setCreatedAt(LocalDateTime.now().minusHours(hoursAgo));
    return game;
  }

  private Trade buildTrade(int hoursAgo) {
    Trade trade = new Trade();
    trade.setId(UUID.randomUUID());
    trade.setStatus(Trade.Status.PENDING);
    trade.setProposedAt(LocalDateTime.now().minusHours(hoursAgo));
    return trade;
  }

  private Trade buildOldTrade(int hoursAgo) {
    return buildTrade(hoursAgo);
  }
}
