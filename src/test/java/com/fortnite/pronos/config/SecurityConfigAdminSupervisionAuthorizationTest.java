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

import com.fortnite.pronos.controller.AdminGameSupervisionController;
import com.fortnite.pronos.service.admin.AdminGameSupervisionService;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.VisitTrackingService;

@WebMvcTest(controllers = AdminGameSupervisionController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SecurityConfig — AdminGameSupervisionController authorization")
class SecurityConfigAdminSupervisionAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private AdminGameSupervisionService supervisionService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  @Test
  @DisplayName("Anonymous user cannot access GET /api/admin/supervision/games")
  void anonymousCannotAccessGames() throws Exception {
    int statusCode =
        mockMvc.perform(get("/api/admin/supervision/games")).andReturn().getResponse().getStatus();
    assertThat(statusCode).isIn(401, 403);
  }

  @Test
  @DisplayName("Non-admin authenticated user is forbidden from GET /api/admin/supervision/games")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminForbiddenFromGames() throws Exception {
    mockMvc.perform(get("/api/admin/supervision/games")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Admin can access GET /api/admin/supervision/games")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessGames() throws Exception {
    when(supervisionService.getAllActiveGames()).thenReturn(List.of());
    mockMvc.perform(get("/api/admin/supervision/games")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Admin can filter GET /api/admin/supervision/games?status=DRAFTING")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanFilterByStatus() throws Exception {
    when(supervisionService.getActiveGamesByStatus(com.fortnite.pronos.model.GameStatus.DRAFTING))
        .thenReturn(List.of());
    mockMvc
        .perform(get("/api/admin/supervision/games").param("status", "DRAFTING"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Invalid enum value ?status=NOT_A_STATUS returns 400")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void invalidStatusValueReturns400() throws Exception {
    mockMvc
        .perform(get("/api/admin/supervision/games").param("status", "NOT_A_STATUS"))
        .andExpect(status().isBadRequest());
  }
}
