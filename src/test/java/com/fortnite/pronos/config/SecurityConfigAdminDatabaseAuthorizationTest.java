package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.fortnite.pronos.controller.AdminDatabaseController;
import com.fortnite.pronos.service.admin.AdminDatabaseService;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.VisitTrackingService;

@WebMvcTest(controllers = AdminDatabaseController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SecurityConfig — AdminDatabaseController authorization")
class SecurityConfigAdminDatabaseAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private AdminDatabaseService adminDatabaseService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  // ── Anonymous ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Anonymous cannot access GET /api/admin/database/tables")
  void anonymousCannotAccessDatabaseTables() throws Exception {
    int status =
        mockMvc.perform(get("/api/admin/database/tables")).andReturn().getResponse().getStatus();
    assertThat(status).isIn(401, 403);
  }

  // ── Non-admin ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Non-admin is forbidden from GET /api/admin/database/tables")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminForbiddenFromDatabaseTables() throws Exception {
    mockMvc.perform(get("/api/admin/database/tables")).andExpect(status().isForbidden());
  }

  // ── Admin ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Admin can access GET /api/admin/database/tables")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessDatabaseTables() throws Exception {
    when(adminDatabaseService.getTableInfo()).thenReturn(List.of());
    mockMvc.perform(get("/api/admin/database/tables")).andExpect(status().isOk());
  }
}
