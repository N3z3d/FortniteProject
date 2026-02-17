package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import com.fortnite.pronos.dto.admin.DashboardSummaryDto;
import com.fortnite.pronos.dto.admin.RecentActivityDto;
import com.fortnite.pronos.dto.admin.SystemHealthDto;
import com.fortnite.pronos.dto.admin.SystemMetricsDto;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Trade;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.TradeRepository;
import com.fortnite.pronos.repository.UserRepository;

import io.micrometer.core.instrument.MeterRegistry;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private GameRepository gameRepository;
  @Mock private TradeRepository tradeRepository;
  @Mock private MeterRegistry meterRegistry;
  @Mock private DataSource dataSource;

  private AdminDashboardService service;

  @BeforeEach
  void setUp() {
    service =
        new AdminDashboardService(
            userRepository, gameRepository, tradeRepository, meterRegistry, dataSource);
  }

  @Nested
  class GetDashboardSummary {

    @Test
    void shouldReturnTotalCounts() {
      when(userRepository.count()).thenReturn(42L);
      when(gameRepository.count()).thenReturn(15L);
      when(tradeRepository.count()).thenReturn(8L);
      when(gameRepository.countByStatus(any())).thenReturn(0L);

      DashboardSummaryDto result = service.getDashboardSummary();

      assertThat(result.getTotalUsers()).isEqualTo(42L);
      assertThat(result.getTotalGames()).isEqualTo(15L);
      assertThat(result.getTotalTrades()).isEqualTo(8L);
    }

    @Test
    void shouldReturnGamesByStatus() {
      when(userRepository.count()).thenReturn(0L);
      when(gameRepository.count()).thenReturn(10L);
      when(tradeRepository.count()).thenReturn(0L);
      when(gameRepository.countByStatus(GameStatus.CREATING)).thenReturn(3L);
      when(gameRepository.countByStatus(GameStatus.DRAFTING)).thenReturn(2L);
      when(gameRepository.countByStatus(GameStatus.ACTIVE)).thenReturn(4L);
      when(gameRepository.countByStatus(GameStatus.FINISHED)).thenReturn(1L);
      when(gameRepository.countByStatus(GameStatus.CANCELLED)).thenReturn(0L);

      DashboardSummaryDto result = service.getDashboardSummary();

      assertThat(result.getGamesByStatus()).containsEntry("CREATING", 3L);
      assertThat(result.getGamesByStatus()).containsEntry("DRAFTING", 2L);
      assertThat(result.getGamesByStatus()).containsEntry("ACTIVE", 4L);
      assertThat(result.getGamesByStatus()).containsEntry("FINISHED", 1L);
      assertThat(result.getGamesByStatus()).containsEntry("CANCELLED", 0L);
    }

    @Test
    void shouldHandleEmptyDatabase() {
      when(userRepository.count()).thenReturn(0L);
      when(gameRepository.count()).thenReturn(0L);
      when(tradeRepository.count()).thenReturn(0L);
      when(gameRepository.countByStatus(any())).thenReturn(0L);

      DashboardSummaryDto result = service.getDashboardSummary();

      assertThat(result.getTotalUsers()).isZero();
      assertThat(result.getTotalGames()).isZero();
      assertThat(result.getTotalTrades()).isZero();
      assertThat(result.getGamesByStatus()).hasSize(5);
    }
  }

  @Nested
  class GetSystemHealth {

    @Test
    void shouldReturnHealthyStatusWithHikari() {
      HikariDataSource hikari = mock(HikariDataSource.class);
      HikariPoolMXBean poolBean = mock(HikariPoolMXBean.class);
      when(hikari.getHikariPoolMXBean()).thenReturn(poolBean);
      when(poolBean.getActiveConnections()).thenReturn(2);
      when(poolBean.getIdleConnections()).thenReturn(8);
      when(poolBean.getTotalConnections()).thenReturn(10);
      when(hikari.getMaximumPoolSize()).thenReturn(20);

      AdminDashboardService svc =
          new AdminDashboardService(
              userRepository, gameRepository, tradeRepository, meterRegistry, hikari);

      SystemHealthDto result = svc.getSystemHealth();

      assertThat(result.getStatus()).isEqualTo("UP");
      assertThat(result.getUptimeMillis()).isPositive();
      assertThat(result.getDatabasePool().getActiveConnections()).isEqualTo(2);
      assertThat(result.getDatabasePool().getIdleConnections()).isEqualTo(8);
      assertThat(result.getDatabasePool().getTotalConnections()).isEqualTo(10);
      assertThat(result.getDatabasePool().getMaxConnections()).isEqualTo(20);
    }

    @Test
    void shouldReturnDiskInfo() {
      SystemHealthDto result = service.getSystemHealth();

      assertThat(result.getDisk()).isNotNull();
      assertThat(result.getDisk().getTotalSpaceBytes()).isPositive();
    }

    @Test
    void shouldReturnFallbackWhenDataSourceIsNotHikari() {
      SystemHealthDto result = service.getSystemHealth();

      assertThat(result.getStatus()).isEqualTo("UP");
      assertThat(result.getDatabasePool().getActiveConnections()).isEqualTo(-1);
    }
  }

  @Nested
  class GetRecentActivity {

    @Test
    void shouldReturnRecentGames() {
      Game game = new Game();
      game.setId(UUID.randomUUID());
      game.setName("TestGame");
      game.setStatus(GameStatus.CREATING);
      game.setCreatedAt(LocalDateTime.now().minusHours(1));

      when(gameRepository.findByCreatedAtAfter(any())).thenReturn(List.of(game));
      when(tradeRepository.findAll()).thenReturn(List.of());
      when(userRepository.count()).thenReturn(5L);

      RecentActivityDto result = service.getRecentActivity(24);

      assertThat(result.getRecentGamesCount()).isEqualTo(1);
      assertThat(result.getRecentGames()).hasSize(1);
      assertThat(result.getRecentGames().get(0).getName()).isEqualTo("TestGame");
    }

    @Test
    void shouldReturnRecentTrades() {
      Trade trade = new Trade();
      trade.setId(UUID.randomUUID());
      trade.setStatus(Trade.Status.PENDING);
      trade.setProposedAt(LocalDateTime.now().minusHours(2));

      when(gameRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
      when(tradeRepository.findAll()).thenReturn(List.of(trade));
      when(userRepository.count()).thenReturn(0L);

      RecentActivityDto result = service.getRecentActivity(24);

      assertThat(result.getRecentTradesCount()).isEqualTo(1);
    }

    @Test
    void shouldFilterByHoursParameter() {
      when(gameRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
      when(tradeRepository.findAll()).thenReturn(List.of());
      when(userRepository.count()).thenReturn(0L);

      RecentActivityDto result = service.getRecentActivity(1);

      assertThat(result.getRecentGamesCount()).isZero();
      assertThat(result.getRecentTradesCount()).isZero();
    }

    @Test
    void shouldDefaultToTwentyFourHours() {
      when(gameRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
      when(tradeRepository.findAll()).thenReturn(List.of());
      when(userRepository.count()).thenReturn(0L);

      RecentActivityDto result = service.getRecentActivity(0);

      assertThat(result).isNotNull();
      verify(gameRepository).findByCreatedAtAfter(any());
    }
  }

  @Nested
  class GetAllUsers {

    @Test
    void shouldReturnAllUsers() {
      User user1 = new User();
      user1.setUsername("Alice");
      User user2 = new User();
      user2.setUsername("Bob");

      when(userRepository.findAll()).thenReturn(List.of(user1, user2));

      List<User> result = service.getAllUsers();

      assertThat(result).hasSize(2);
    }

    @Test
    void shouldReturnEmptyListWhenNoUsers() {
      when(userRepository.findAll()).thenReturn(List.of());

      List<User> result = service.getAllUsers();

      assertThat(result).isEmpty();
    }
  }

  @Nested
  class GetAllGames {

    @Test
    void shouldReturnAllGamesWhenNoFilter() {
      Game game = new Game();
      game.setName("Game1");
      when(gameRepository.findAll()).thenReturn(List.of(game));

      List<Game> result = service.getAllGames(null);

      assertThat(result).hasSize(1);
      verify(gameRepository).findAll();
    }

    @Test
    void shouldFilterGamesByStatus() {
      Game game = new Game();
      game.setName("ActiveGame");
      game.setStatus(GameStatus.ACTIVE);
      when(gameRepository.findByStatus(GameStatus.ACTIVE)).thenReturn(List.of(game));

      List<Game> result = service.getAllGames("ACTIVE");

      assertThat(result).hasSize(1);
      verify(gameRepository).findByStatus(GameStatus.ACTIVE);
    }

    @Test
    void shouldReturnAllGamesForInvalidStatus() {
      when(gameRepository.findAll()).thenReturn(List.of());

      List<Game> result = service.getAllGames("INVALID_STATUS");

      assertThat(result).isEmpty();
      verify(gameRepository).findAll();
    }
  }

  @Nested
  class GetSystemMetrics {

    @Test
    void shouldReturnJvmMetrics() {
      SystemMetricsDto result = service.getSystemMetrics();

      assertThat(result.getJvm()).isNotNull();
      assertThat(result.getJvm().getHeapMaxBytes()).isPositive();
      assertThat(result.getJvm().getThreadCount()).isPositive();
    }

    @Test
    void shouldReturnHttpMetrics() {
      SystemMetricsDto result = service.getSystemMetrics();

      assertThat(result.getHttp()).isNotNull();
    }
  }
}
