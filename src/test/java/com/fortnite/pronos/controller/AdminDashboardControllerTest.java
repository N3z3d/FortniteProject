package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.dto.admin.AdminAlertDto;
import com.fortnite.pronos.dto.admin.AdminUserDto;
import com.fortnite.pronos.dto.admin.DashboardSummaryDto;
import com.fortnite.pronos.dto.admin.RealTimeAnalyticsDto;
import com.fortnite.pronos.dto.admin.RecentActivityDto;
import com.fortnite.pronos.dto.admin.SystemHealthDto;
import com.fortnite.pronos.dto.admin.SystemMetricsDto;
import com.fortnite.pronos.dto.admin.VisitAnalyticsDto;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.service.admin.AdminAlertService;
import com.fortnite.pronos.service.admin.AdminDashboardService;
import com.fortnite.pronos.service.admin.AdminVisitAnalyticsService;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

  @Mock private AdminDashboardService adminDashboardService;
  @Mock private AdminAlertService adminAlertService;
  @Mock private AdminVisitAnalyticsService adminVisitAnalyticsService;

  private AdminDashboardController controller;

  @BeforeEach
  void setUp() {
    controller =
        new AdminDashboardController(
            adminDashboardService, adminAlertService, adminVisitAnalyticsService);
  }

  @Nested
  @DisplayName("Get Dashboard Summary")
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
  @DisplayName("Get System Health")
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
  @DisplayName("Get Recent Activity")
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
  @DisplayName("Get All Users")
  class GetAllUsers {

    @Test
    void shouldReturnUsersList() {
      var dto =
          AdminUserDto.builder()
              .id(UUID.randomUUID())
              .username("admin_user")
              .email("admin@test.com")
              .role("ADMIN")
              .currentSeason(2025)
              .deleted(false)
              .build();
      when(adminDashboardService.getAllUsers()).thenReturn(List.of(dto));

      var response = controller.getAllUsers();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData()).hasSize(1);
      assertThat(response.getBody().getData().get(0).getUsername()).isEqualTo("admin_user");
    }

    @Test
    void shouldReturnDeletedUsersInList() {
      var active =
          AdminUserDto.builder()
              .id(UUID.randomUUID())
              .username("active_user")
              .email("active@test.com")
              .role("USER")
              .currentSeason(2025)
              .deleted(false)
              .build();
      var deleted =
          AdminUserDto.builder()
              .id(UUID.randomUUID())
              .username("deleted_user")
              .email("deleted@test.com")
              .role("USER")
              .currentSeason(2025)
              .deleted(true)
              .build();
      when(adminDashboardService.getAllUsers()).thenReturn(List.of(active, deleted));

      var response = controller.getAllUsers();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData()).hasSize(2);
      assertThat(response.getBody().getData().get(1).isDeleted()).isTrue();
    }
  }

  @Nested
  @DisplayName("Get All Games")
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
  @DisplayName("Get System Metrics")
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

  @Nested
  @DisplayName("Get Visit Analytics")
  class GetVisitAnalytics {

    @Test
    void shouldReturnVisitAnalytics() {
      var dto =
          VisitAnalyticsDto.builder()
              .pageViews(120)
              .uniqueVisitors(14)
              .activeSessions(10)
              .averageSessionDurationSeconds(88.4)
              .build();
      when(adminVisitAnalyticsService.getVisitAnalytics(24)).thenReturn(dto);

      var response = controller.getVisitAnalytics(24);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getData().getPageViews()).isEqualTo(120);
    }

    @Test
    void shouldRejectInvalidHoursForVisitAnalytics() {
      var response = controller.getVisitAnalytics(0);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      verify(adminVisitAnalyticsService, never()).getVisitAnalytics(anyInt());
    }
  }

  @Nested
  @DisplayName("Get Real Time Analytics")
  class GetRealTimeAnalytics {

    @Test
    void shouldReturnRealTimeSnapshot() {
      var dto =
          RealTimeAnalyticsDto.builder()
              .activeUsersNow(5)
              .activeSessionsNow(4)
              .activePagesNow(new ArrayList<>())
              .build();
      when(adminVisitAnalyticsService.getRealTimeSnapshot()).thenReturn(dto);

      var response = controller.getRealTimeAnalytics();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getData().getActiveUsersNow()).isEqualTo(5);
      assertThat(response.getBody().getData().getActiveSessionsNow()).isEqualTo(4);
    }

    @Test
    void shouldReturnEmptySnapshotWhenNoActivity() {
      var dto =
          RealTimeAnalyticsDto.builder()
              .activeUsersNow(0)
              .activeSessionsNow(0)
              .activePagesNow(new ArrayList<>())
              .build();
      when(adminVisitAnalyticsService.getRealTimeSnapshot()).thenReturn(dto);

      var response = controller.getRealTimeAnalytics();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData().getActiveUsersNow()).isZero();
      assertThat(response.getBody().getData().getActivePagesNow()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Get Alerts")
  class GetAlerts {

    @Test
    void shouldReturnAlertList() {
      var alert =
          AdminAlertDto.builder()
              .code("SYSTEM_DOWN")
              .severity(AdminAlertDto.Severity.CRITICAL)
              .title("System down")
              .message("System status is DOWN")
              .build();
      when(adminAlertService.getActiveAlerts(eq(24), any())).thenReturn(List.of(alert));

      var response = controller.getAlerts(24, 5, 85, 90, 80, 10);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getData()).hasSize(1);
      assertThat(response.getBody().getData().get(0).getCode()).isEqualTo("SYSTEM_DOWN");
    }
  }
}
