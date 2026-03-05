package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fortnite.pronos.controller.AdminPlayerPipelineController;
import com.fortnite.pronos.service.admin.AdminAuditLogService;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.PlayerIdentityPipelineService;
import com.fortnite.pronos.service.admin.VisitTrackingService;

@WebMvcTest(controllers = AdminPlayerPipelineController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SecurityConfig — AdminPlayerPipelineController authorization")
class SecurityConfigAdminPipelineAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private PlayerIdentityPipelineService pipelineService;

  @org.springframework.boot.test.mock.mockito.MockBean private AdminAuditLogService auditLogService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  // ── Anonymous ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Anonymous cannot access GET /api/admin/players/unresolved")
  void anonymousCannotAccessUnresolved() throws Exception {
    int status =
        mockMvc.perform(get("/api/admin/players/unresolved")).andReturn().getResponse().getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Anonymous cannot access GET /api/admin/players/resolved")
  void anonymousCannotAccessResolved() throws Exception {
    int status =
        mockMvc.perform(get("/api/admin/players/resolved")).andReturn().getResponse().getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Anonymous cannot access GET /api/admin/players/pipeline/count")
  void anonymousCannotAccessCount() throws Exception {
    int status =
        mockMvc
            .perform(get("/api/admin/players/pipeline/count"))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }

  // ── Non-admin ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Non-admin is forbidden from GET /api/admin/players/unresolved")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminForbiddenFromUnresolved() throws Exception {
    mockMvc.perform(get("/api/admin/players/unresolved")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Non-admin is forbidden from GET /api/admin/players/pipeline/count")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminForbiddenFromCount() throws Exception {
    mockMvc.perform(get("/api/admin/players/pipeline/count")).andExpect(status().isForbidden());
  }

  // ── Admin ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Admin can access GET /api/admin/players/unresolved")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessUnresolved() throws Exception {
    when(pipelineService.getUnresolved(anyInt(), anyInt())).thenReturn(List.of());
    mockMvc.perform(get("/api/admin/players/unresolved")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Admin can access GET /api/admin/players/resolved")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessResolved() throws Exception {
    when(pipelineService.getResolved(anyInt(), anyInt())).thenReturn(List.of());
    mockMvc.perform(get("/api/admin/players/resolved")).andExpect(status().isOk());
  }
}
