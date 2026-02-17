package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.dto.admin.DashboardSummaryDto;
import com.fortnite.pronos.dto.admin.RecentActivityDto;
import com.fortnite.pronos.dto.admin.SystemHealthDto;
import com.fortnite.pronos.dto.admin.SystemMetricsDto;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.admin.AdminDashboardService;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

  @Mock private AdminDashboardService adminDashboardService;

  private AdminDashboardController controller;

  @BeforeEach
  void setUp() {
    controller = new AdminDashboardController(adminDashboardService);
  }

  @Nested
  class GetDashboardSummary {

    @Test
    void shouldReturnSummary() {
      var dto =
          DashboardSummaryDto.builder()
              .totalUsers(10)
              .totalGames(5)
              .totalTrades(3)
              .gamesByStatus(Map.of("CREATING", 2L, "ACTIVE", 3L))
              .build();
      when(adminDashboardService.getDashboardSummary()).thenReturn(dto);

      var response = controller.getDashboardSummary();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().isSuccess()).isTrue();
      assertThat(response.getBody().getData().getTotalUsers()).isEqualTo(10);
    }
  }

  @Nested
  class GetSystemHealth {

    @Test
    void shouldReturnHealth() {
      var dto = SystemHealthDto.builder().status("UP").uptimeMillis(60000).build();
      when(adminDashboardService.getSystemHealth()).thenReturn(dto);

      var response = controller.getSystemHealth();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData().getStatus()).isEqualTo("UP");
    }
  }

  @Nested
  class GetRecentActivity {

    @Test
    void shouldReturnActivityWithDefaultHours() {
      var dto = RecentActivityDto.builder().recentGamesCount(2).recentTradesCount(1).build();
      when(adminDashboardService.getRecentActivity(24)).thenReturn(dto);

      var response = controller.getRecentActivity(24);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData().getRecentGamesCount()).isEqualTo(2);
    }

    @Test
    void shouldAcceptCustomHours() {
      var dto = RecentActivityDto.builder().recentGamesCount(0).build();
      when(adminDashboardService.getRecentActivity(1)).thenReturn(dto);

      var response = controller.getRecentActivity(1);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      verify(adminDashboardService).getRecentActivity(1);
    }

    @Test
    void shouldRejectHoursBelowMinimum() {
      var response = controller.getRecentActivity(0);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().isSuccess()).isFalse();
      verify(adminDashboardService, never()).getRecentActivity(anyInt());
    }

    @Test
    void shouldRejectHoursAboveMaximum() {
      var response = controller.getRecentActivity(169);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().isSuccess()).isFalse();
      verify(adminDashboardService, never()).getRecentActivity(anyInt());
    }
  }

  @Nested
  class GetAllUsers {

    @Test
    void shouldReturnUsersList() {
      User user = new User();
      user.setUsername("Admin");
      when(adminDashboardService.getAllUsers()).thenReturn(List.of(user));

      var response = controller.getAllUsers();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData()).hasSize(1);
    }
  }

  @Nested
  class GetAllGames {

    @Test
    void shouldReturnAllGamesWithoutFilter() {
      Game game = new Game();
      game.setName("TestGame");
      when(adminDashboardService.getAllGames(null)).thenReturn(List.of(game));

      var response = controller.getAllGames(null);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void shouldReturnFilteredGames() {
      when(adminDashboardService.getAllGames("ACTIVE")).thenReturn(List.of());

      var response = controller.getAllGames("ACTIVE");

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      verify(adminDashboardService).getAllGames("ACTIVE");
    }
  }

  @Nested
  class GetSystemMetrics {

    @Test
    void shouldReturnMetrics() {
      var dto =
          SystemMetricsDto.builder()
              .jvm(
                  SystemMetricsDto.JvmInfo.builder().heapUsedBytes(1000).heapMaxBytes(2000).build())
              .build();
      when(adminDashboardService.getSystemMetrics()).thenReturn(dto);

      var response = controller.getSystemMetrics();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData().getJvm().getHeapUsedBytes()).isEqualTo(1000);
    }
  }
}
