package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
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

import com.fortnite.pronos.controller.SnakeDraftController;
import com.fortnite.pronos.dto.SnakeTurnResponse;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.draft.DraftTrancheService;
import com.fortnite.pronos.service.draft.SnakeDraftService;
import com.fortnite.pronos.service.game.GameDraftService;

@WebMvcTest(controllers = SnakeDraftController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SecurityConfig — SnakeDraftController authorization")
class SecurityConfigSnakeDraftAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean private SnakeDraftService snakeDraftService;

  @org.springframework.boot.test.mock.mockito.MockBean private GameDraftService gameDraftService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private DraftTrancheService draftTrancheService;

  @org.springframework.boot.test.mock.mockito.MockBean private UserResolver userResolver;

  @org.springframework.boot.test.mock.mockito.MockBean
  private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

  @org.springframework.boot.test.mock.mockito.MockBean
  private com.fortnite.pronos.service.admin.ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private com.fortnite.pronos.service.admin.VisitTrackingService visitTrackingService;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final String PICK_BODY =
      "{\"playerId\":\"" + UUID.randomUUID() + "\",\"region\":\"GLOBAL\"}";

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on POST initialize")
  void unauthenticatedCannotInitialize() throws Exception {
    int status =
        mockMvc
            .perform(post("/api/games/{id}/draft/snake/initialize", GAME_ID))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on POST pick")
  void unauthenticatedCannotPick() throws Exception {
    int status =
        mockMvc
            .perform(
                post("/api/games/{id}/draft/snake/pick", GAME_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(PICK_BODY))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on GET turn")
  void unauthenticatedCannotGetTurn() throws Exception {
    int status =
        mockMvc
            .perform(get("/api/games/{id}/draft/snake/turn", GAME_ID))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Authenticated user is allowed through Spring Security on POST initialize")
  @WithMockUser(username = "player1")
  void authenticatedUserIsAllowedOnInitialize() throws Exception {
    when(userResolver.resolve(nullable(String.class), any(HttpServletRequest.class)))
        .thenReturn(null);

    // userResolver returns null → controller returns 401 (not 403 from Spring Security)
    mockMvc
        .perform(post("/api/games/{id}/draft/snake/initialize", GAME_ID))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Authenticated user receives 200 on GET turn when cursor exists")
  @WithMockUser(username = "player1")
  void authenticatedUserCanGetTurn() throws Exception {
    UUID draftId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    when(snakeDraftService.getCurrentTurn(GAME_ID, "GLOBAL"))
        .thenReturn(
            Optional.of(
                new SnakeTurnResponse(draftId, "GLOBAL", participantId, null, 1, 1, false, null)));

    mockMvc
        .perform(get("/api/games/{id}/draft/snake/turn", GAME_ID).param("region", "GLOBAL"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on GET recommend")
  void unauthenticatedCannotGetRecommend() throws Exception {
    int status =
        mockMvc
            .perform(
                get("/api/games/{id}/draft/snake/recommend", GAME_ID).param("region", "GLOBAL"))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Authenticated user receives 404 on GET recommend when no player available")
  @WithMockUser(username = "player1")
  void authenticatedUserGets404OnRecommendWhenNoPlayer() throws Exception {
    when(draftTrancheService.recommendPlayer(GAME_ID, "GLOBAL")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/games/{id}/draft/snake/recommend", GAME_ID).param("region", "GLOBAL"))
        .andExpect(status().isNotFound());
  }
}
