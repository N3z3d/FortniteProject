package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fortnite.pronos.controller.AdminDraftRosterController;
import com.fortnite.pronos.service.admin.AdminDraftRosterService;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.VisitTrackingService;

@WebMvcTest(controllers = AdminDraftRosterController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SecurityConfig — AdminDraftRosterController authorization")
class SecurityConfigAdminDraftRosterAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private AdminDraftRosterService rosterService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID PLAYER_ID = UUID.randomUUID();
  private static final String ASSIGN_BODY =
      "{\"participantUserId\":\"" + UUID.randomUUID() + "\",\"playerId\":\"" + PLAYER_ID + "\"}";

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on POST assign")
  void unauthenticatedCannotAssignPlayer() throws Exception {
    int status =
        mockMvc
            .perform(
                post("/api/admin/games/{gameId}/roster", GAME_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ASSIGN_BODY))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on DELETE remove")
  void unauthenticatedCannotRemovePlayer() throws Exception {
    int status =
        mockMvc
            .perform(delete("/api/admin/games/{gameId}/roster/{playerId}", GAME_ID, PLAYER_ID))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }
}
