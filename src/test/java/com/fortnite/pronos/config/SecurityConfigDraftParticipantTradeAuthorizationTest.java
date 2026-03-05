package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.fortnite.pronos.controller.DraftParticipantTradeController;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.VisitTrackingService;
import com.fortnite.pronos.service.draft.DraftParticipantTradeService;

@WebMvcTest(controllers = DraftParticipantTradeController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SecurityConfig — DraftParticipantTradeController authorization")
class SecurityConfigDraftParticipantTradeAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private DraftParticipantTradeService draftParticipantTradeService;

  @org.springframework.boot.test.mock.mockito.MockBean private UserResolver userResolver;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID TRADE_ID = UUID.randomUUID();
  private static final String PROPOSE_BODY =
      "{\"targetParticipantId\":\""
          + UUID.randomUUID()
          + "\",\"playerFromProposerId\":\""
          + UUID.randomUUID()
          + "\",\"playerFromTargetId\":\""
          + UUID.randomUUID()
          + "\"}";

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on POST propose-trade")
  void unauthenticatedCannotProposeTrade() throws Exception {
    int status =
        mockMvc
            .perform(
                post("/api/games/{gameId}/draft/trade", GAME_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(PROPOSE_BODY))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on POST accept-trade")
  void unauthenticatedCannotAcceptTrade() throws Exception {
    int status =
        mockMvc
            .perform(post("/api/games/{gameId}/draft/trade/{tradeId}/accept", GAME_ID, TRADE_ID))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on POST reject-trade")
  void unauthenticatedCannotRejectTrade() throws Exception {
    int status =
        mockMvc
            .perform(post("/api/games/{gameId}/draft/trade/{tradeId}/reject", GAME_ID, TRADE_ID))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }
}
