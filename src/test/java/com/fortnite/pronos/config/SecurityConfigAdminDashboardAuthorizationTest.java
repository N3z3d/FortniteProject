package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fortnite.pronos.controller.AdminDashboardController;
import com.fortnite.pronos.dto.admin.DashboardSummaryDto;
import com.fortnite.pronos.service.admin.AdminAlertService;
import com.fortnite.pronos.service.admin.AdminDashboardService;
import com.fortnite.pronos.service.admin.AdminVisitAnalyticsService;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.VisitTrackingService;

@WebMvcTest(controllers = AdminDashboardController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SecurityConfig — AdminDashboardController authorization")
class SecurityConfigAdminDashboardAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private AdminDashboardService adminDashboardService;

  @org.springframework.boot.test.mock.mockito.MockBean private AdminAlertService adminAlertService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private AdminVisitAnalyticsService adminVisitAnalyticsService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  // ── Anonymous ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Anonymous cannot access GET /api/admin/dashboard/summary")
  void anonymousCannotAccessSummary() throws Exception {
    int status =
        mockMvc.perform(get("/api/admin/dashboard/summary")).andReturn().getResponse().getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Anonymous cannot access GET /api/admin/users")
  void anonymousCannotAccessUsers() throws Exception {
    int status = mockMvc.perform(get("/api/admin/users")).andReturn().getResponse().getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Anonymous cannot access GET /api/admin/dashboard/health")
  void anonymousCannotAccessHealth() throws Exception {
    int status =
        mockMvc.perform(get("/api/admin/dashboard/health")).andReturn().getResponse().getStatus();
    assertThat(status).isIn(401, 403);
  }

  // ── Non-admin ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Non-admin is forbidden from GET /api/admin/dashboard/summary")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminForbiddenFromSummary() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/summary")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Non-admin is forbidden from GET /api/admin/users")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminForbiddenFromUsers() throws Exception {
    mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
  }

  // ── Admin ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Admin can access GET /api/admin/dashboard/summary")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessSummary() throws Exception {
    when(adminDashboardService.getDashboardSummary())
        .thenReturn(
            DashboardSummaryDto.builder()
                .totalUsers(0)
                .totalGames(0)
                .totalTrades(0)
                .gamesByStatus(Map.of())
                .build());
    mockMvc.perform(get("/api/admin/dashboard/summary")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Admin can access GET /api/admin/users")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessUsers() throws Exception {
    when(adminDashboardService.getAllUsers()).thenReturn(List.of());
    mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk());
  }
}
