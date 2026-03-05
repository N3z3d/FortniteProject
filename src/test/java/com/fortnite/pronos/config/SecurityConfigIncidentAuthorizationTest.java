package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fortnite.pronos.controller.GameIncidentController;
import com.fortnite.pronos.dto.IncidentReportRequest.IncidentType;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.admin.GameIncidentService;
import com.fortnite.pronos.service.admin.IncidentEntry;
import com.fortnite.pronos.service.admin.IncidentReportingService;

@WebMvcTest(controllers = GameIncidentController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SecurityConfig — GameIncidentController authorization")
class SecurityConfigIncidentAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private IncidentReportingService incidentReportingService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private GameIncidentService gameIncidentService;

  @org.springframework.boot.test.mock.mockito.MockBean private UserResolver userResolver;

  @org.springframework.boot.test.mock.mockito.MockBean
  private com.fortnite.pronos.service.admin.ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private com.fortnite.pronos.service.admin.VisitTrackingService visitTrackingService;

  private static final String REPORT_BODY =
      "{\"incidentType\":\"BUG\",\"description\":\"Game froze\"}";

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on POST /api/games/{id}/incidents")
  void unauthenticatedCannotReportIncident() throws Exception {
    int status =
        mockMvc
            .perform(
                post("/api/games/{id}/incidents", UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REPORT_BODY))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on GET /api/admin/incidents")
  void unauthenticatedCannotGetAdminIncidents() throws Exception {
    int status = mockMvc.perform(get("/api/admin/incidents")).andReturn().getResponse().getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Non-admin authenticated user receives 403 on GET /api/admin/incidents")
  @WithMockUser(
      username = "player1",
      roles = {"USER"})
  void nonAdminCannotGetAdminIncidents() throws Exception {
    mockMvc.perform(get("/api/admin/incidents")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Admin user receives 200 on GET /api/admin/incidents")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanGetAdminIncidents() throws Exception {
    IncidentEntry entry =
        IncidentEntry.builder()
            .id(UUID.randomUUID())
            .gameId(UUID.randomUUID())
            .gameName("Test Game")
            .reporterId(UUID.randomUUID())
            .reporterUsername("player1")
            .incidentType(IncidentType.BUG)
            .description("Game froze")
            .timestamp(OffsetDateTime.now())
            .build();
    when(gameIncidentService.getRecentIncidents(50, null)).thenReturn(List.of(entry));

    mockMvc.perform(get("/api/admin/incidents")).andExpect(status().isOk());
  }

  @Test
  @DisplayName(
      "Authenticated user is allowed through Spring Security on POST /api/games/{id}/incidents")
  @WithMockUser(username = "player1")
  void authenticatedUserIsAllowedThroughSecurityOnReportIncident() throws Exception {
    when(userResolver.resolve(nullable(String.class), any(HttpServletRequest.class)))
        .thenReturn(null);

    // userResolver returns null → controller returns 401 (not 403 from Spring Security)
    mockMvc
        .perform(
            post("/api/games/{id}/incidents", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(REPORT_BODY))
        .andExpect(status().isUnauthorized());
  }
}
