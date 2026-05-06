package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fortnite.pronos.controller.DraftSimultaneousController;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.VisitTrackingService;
import com.fortnite.pronos.service.draft.DraftSimultaneousService;
import com.fortnite.pronos.service.draft.DraftTrancheService;

@WebMvcTest(controllers = DraftSimultaneousController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("SecurityConfig — DraftSimultaneousController authorization")
class SecurityConfigSimultaneousDraftAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private DraftSimultaneousService simultaneousService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private DraftTrancheService draftTrancheService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  private static final UUID DRAFT_ID = UUID.randomUUID();
  private static final UUID WINDOW_ID = UUID.randomUUID();
  private static final String SUBMIT_BODY =
      "{"
          + "\"windowId\":\""
          + WINDOW_ID
          + "\","
          + "\"participantId\":\""
          + UUID.randomUUID()
          + "\","
          + "\"playerId\":\""
          + UUID.randomUUID()
          + "\"}";

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on POST open-window")
  void unauthenticatedCannotOpenWindow() throws Exception {
    int status =
        mockMvc
            .perform(
                post("/api/draft/simultaneous/{id}/open-window", DRAFT_ID)
                    .param("slot", "R1P1")
                    .param("deadlineSeconds", "45")
                    .param("totalParticipants", "4"))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on POST submit")
  void unauthenticatedCannotSubmit() throws Exception {
    int status =
        mockMvc
            .perform(
                post("/api/draft/simultaneous/{id}/submit", DRAFT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SUBMIT_BODY))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on GET status")
  void unauthenticatedCannotGetStatus() throws Exception {
    int status =
        mockMvc
            .perform(get("/api/draft/simultaneous/{id}/status", DRAFT_ID))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on POST resolve-conflict")
  void unauthenticatedCannotResolveConflict() throws Exception {
    int status =
        mockMvc
            .perform(
                post(
                    "/api/draft/simultaneous/{id}/resolve-conflict/{windowId}",
                    DRAFT_ID,
                    WINDOW_ID))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }
}
