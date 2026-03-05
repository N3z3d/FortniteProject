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

import com.fortnite.pronos.application.usecase.GameQueryUseCase;
import com.fortnite.pronos.controller.GameController;
import com.fortnite.pronos.core.usecase.CreateGameUseCase;
import com.fortnite.pronos.service.GameService;
import com.fortnite.pronos.service.InvitationCodeAttemptGuard;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.ValidationService;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.VisitTrackingService;

@WebMvcTest(controllers = GameController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SecurityConfig — GameController.configureCompetitionPeriod authorization")
class SecurityConfigGameConfigurePeriodAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean private GameService gameService;

  @org.springframework.boot.test.mock.mockito.MockBean private GameQueryUseCase gameQueryUseCase;

  @org.springframework.boot.test.mock.mockito.MockBean private ValidationService validationService;

  @org.springframework.boot.test.mock.mockito.MockBean private UserResolver userResolver;

  @org.springframework.boot.test.mock.mockito.MockBean private CreateGameUseCase createGameUseCase;

  @org.springframework.boot.test.mock.mockito.MockBean
  private InvitationCodeAttemptGuard invitationCodeAttemptGuard;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final String PERIOD_BODY =
      "{\"startDate\":\"2026-03-01\",\"endDate\":\"2026-03-31\"}";

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on POST configure-period")
  void unauthenticatedCannotConfigurePeriod() throws Exception {
    int status =
        mockMvc
            .perform(
                post("/api/games/{id}/configure-period", GAME_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(PERIOD_BODY))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }
}
