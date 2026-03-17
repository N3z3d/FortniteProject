package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
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

import com.fortnite.pronos.controller.AdminScrapeController;
import com.fortnite.pronos.dto.admin.DryRunResultDto;
import com.fortnite.pronos.dto.admin.PipelineAlertDto;
import com.fortnite.pronos.dto.admin.PipelineAlertDto.AlertLevel;
import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.ScrapeLogService;
import com.fortnite.pronos.service.admin.UnresolvedAlertService;
import com.fortnite.pronos.service.admin.VisitTrackingService;
import com.fortnite.pronos.service.ingestion.ScrapingDryRunService;

@WebMvcTest(controllers = AdminScrapeController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SecurityConfig — AdminScrapeController authorization")
class SecurityConfigAdminScrapeAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean private ScrapeLogService scrapeLogService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UnresolvedAlertService unresolvedAlertService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ScrapingDryRunService scrapingDryRunService;

  @Test
  @DisplayName("Anonymous user cannot access GET /api/admin/scraping/logs")
  void anonymousCannotAccessLogs() throws Exception {
    int status =
        mockMvc.perform(get("/api/admin/scraping/logs")).andReturn().getResponse().getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Anonymous user cannot access GET /api/admin/scraping/alert")
  void anonymousCannotAccessAlert() throws Exception {
    int status =
        mockMvc.perform(get("/api/admin/scraping/alert")).andReturn().getResponse().getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Non-admin authenticated user is forbidden from GET /api/admin/scraping/logs")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminForbiddenFromLogs() throws Exception {
    mockMvc.perform(get("/api/admin/scraping/logs")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Non-admin authenticated user is forbidden from GET /api/admin/scraping/alert")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminForbiddenFromAlert() throws Exception {
    mockMvc.perform(get("/api/admin/scraping/alert")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Admin can access GET /api/admin/scraping/logs")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessLogs() throws Exception {
    when(scrapeLogService.getRecentLogs(50)).thenReturn(List.of());
    mockMvc.perform(get("/api/admin/scraping/logs")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Admin can access GET /api/admin/scraping/alert")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessAlert() throws Exception {
    when(unresolvedAlertService.getAlertStatus())
        .thenReturn(new PipelineAlertDto(AlertLevel.NONE, 0L, null, 0L, OffsetDateTime.now()));
    mockMvc.perform(get("/api/admin/scraping/alert")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("limit=0 on GET /api/admin/scraping/logs returns 400")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void limitZeroReturnsBadRequest() throws Exception {
    mockMvc
        .perform(get("/api/admin/scraping/logs").param("limit", "0"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("limit=201 on GET /api/admin/scraping/logs returns 400")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void limitAboveMaxReturnsBadRequest() throws Exception {
    mockMvc
        .perform(get("/api/admin/scraping/logs").param("limit", "201"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Anonymous user cannot access POST /api/admin/scraping/dry-run")
  void anonymousCannotAccessDryRun() throws Exception {
    int status =
        mockMvc.perform(post("/api/admin/scraping/dry-run")).andReturn().getResponse().getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Non-admin authenticated user is forbidden from POST /api/admin/scraping/dry-run")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminForbiddenFromDryRun() throws Exception {
    mockMvc.perform(post("/api/admin/scraping/dry-run")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Admin can access POST /api/admin/scraping/dry-run")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessDryRun() throws Exception {
    when(scrapingDryRunService.runDryRun(any(PrRegion.class)))
        .thenReturn(new DryRunResultDto("EU", 15, true, List.of(), List.of()));
    mockMvc
        .perform(post("/api/admin/scraping/dry-run").param("region", "EU"))
        .andExpect(status().isOk());
  }
}
