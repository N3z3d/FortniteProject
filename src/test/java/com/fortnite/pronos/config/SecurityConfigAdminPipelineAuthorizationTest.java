package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

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
import com.fortnite.pronos.core.error.ErrorCode;
import com.fortnite.pronos.core.error.FortnitePronosException;
import com.fortnite.pronos.dto.admin.EpicIdSuggestionResponse;
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

  @Test
  @DisplayName("Anonymous cannot access GET /api/admin/players/{playerId}/suggest-epic-id")
  void anonymousCannotAccessSuggestEpicId() throws Exception {
    int status =
        mockMvc
            .perform(get("/api/admin/players/{playerId}/suggest-epic-id", UUID.randomUUID()))
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

  @Test
  @DisplayName("Non-admin is forbidden from GET /api/admin/players/{playerId}/suggest-epic-id")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminForbiddenFromSuggestEpicId() throws Exception {
    mockMvc
        .perform(get("/api/admin/players/{playerId}/suggest-epic-id", UUID.randomUUID()))
        .andExpect(status().isForbidden());
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

  @Test
  @DisplayName("Admin can access GET /api/admin/players/{playerId}/suggest-epic-id")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessSuggestEpicId() throws Exception {
    UUID playerId = UUID.randomUUID();
    when(pipelineService.suggestEpicId(playerId)).thenReturn(EpicIdSuggestionResponse.notFound());

    mockMvc
        .perform(get("/api/admin/players/{playerId}/suggest-epic-id", playerId))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Admin gets HTTP 429 when suggest-epic-id hits rate limit")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminGets429WhenSuggestEpicIdIsRateLimited() throws Exception {
    UUID playerId = UUID.randomUUID();
    when(pipelineService.suggestEpicId(playerId))
        .thenThrow(new FortnitePronosException(ErrorCode.SYS_004, "Rate limit exceeded"));

    mockMvc
        .perform(get("/api/admin/players/{playerId}/suggest-epic-id", playerId))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("SYS_004"));
  }

  @Test
  @DisplayName("Admin gets HTTP 503 when suggest-epic-id is temporarily unavailable")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminGets503WhenSuggestEpicIdIsUnavailable() throws Exception {
    UUID playerId = UUID.randomUUID();
    when(pipelineService.suggestEpicId(playerId))
        .thenThrow(
            new FortnitePronosException(
                ErrorCode.SYS_002, "Suggestion Epic ID temporairement indisponible"));

    mockMvc
        .perform(get("/api/admin/players/{playerId}/suggest-epic-id", playerId))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("SYS_002"));
  }
}
