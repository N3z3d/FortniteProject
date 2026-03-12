package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.dto.PlayerRecommendResponse;
import com.fortnite.pronos.dto.SnakePickRequest;
import com.fortnite.pronos.dto.SnakeTurnResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.draft.DraftTrancheService;
import com.fortnite.pronos.service.draft.SnakeDraftService;
import com.fortnite.pronos.service.game.GameDraftService;

@ExtendWith(MockitoExtension.class)
class SnakeDraftControllerTest {

  @Mock private SnakeDraftService snakeDraftService;
  @Mock private GameDraftService gameDraftService;
  @Mock private UserResolver userResolver;
  @Mock private DraftTrancheService draftTrancheService;
  @Mock private HttpServletRequest httpRequest;

  private SnakeDraftController controller;

  private final UUID gameId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID draftId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    controller =
        new SnakeDraftController(
            snakeDraftService, gameDraftService, userResolver, draftTrancheService);
  }

  private User stubUser() {
    User user = new User();
    user.setId(userId);
    user.setUsername("player1");
    return user;
  }

  private SnakeTurnResponse buildTurnResponse() {
    return new SnakeTurnResponse(draftId, "GLOBAL", userId, 1, 1, false);
  }

  @Nested
  @DisplayName("Initialize Cursors")
  class InitializeCursors {

    @Test
    void whenAuthenticated_returns201WithFirstTurn() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(stubUser());
      when(snakeDraftService.initializeCursors(gameId)).thenReturn(buildTurnResponse());

      var response = controller.initializeCursors(gameId, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().isSuccess()).isTrue();
      assertThat(response.getBody().getData().region()).isEqualTo("GLOBAL");
    }

    @Test
    void whenNotAuthenticated_returns401() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(null);

      var response = controller.initializeCursors(gameId, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      verifyNoInteractions(snakeDraftService);
    }
  }

  @Nested
  @DisplayName("Get Current Turn")
  class GetCurrentTurn {

    @Test
    void whenCursorExists_returns200() {
      when(snakeDraftService.getCurrentTurn(gameId, "GLOBAL"))
          .thenReturn(Optional.of(buildTurnResponse()));

      var response = controller.getCurrentTurn(gameId, "GLOBAL");

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData().draftId()).isEqualTo(draftId);
    }

    @Test
    void whenNoCursorYet_returns404() {
      when(snakeDraftService.getCurrentTurn(gameId, "GLOBAL")).thenReturn(Optional.empty());

      var response = controller.getCurrentTurn(gameId, "GLOBAL");

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("Recommend")
  class Recommend {

    @Test
    void whenPlayerAvailable_returns200() {
      PlayerRecommendResponse recommended =
          new PlayerRecommendResponse(UUID.randomUUID(), "Best Player", "EU", "11-20", 11);
      when(draftTrancheService.recommendPlayer(gameId, "GLOBAL"))
          .thenReturn(Optional.of(recommended));

      var response = controller.recommend(gameId, "GLOBAL");

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData().tranche()).isEqualTo("11-20");
    }

    @Test
    void whenNoPlayerAvailable_returns404() {
      when(draftTrancheService.recommendPlayer(gameId, "GLOBAL")).thenReturn(Optional.empty());

      var response = controller.recommend(gameId, "GLOBAL");

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("Process Pick")
  class ProcessPick {

    @Test
    void whenUserTurn_returns200WithNextTurnAndValidatePickCalled() {
      UUID nextParticipantId = UUID.randomUUID();
      SnakeTurnResponse nextTurn =
          new SnakeTurnResponse(draftId, "GLOBAL", nextParticipantId, 1, 2, false);
      SnakePickRequest request = new SnakePickRequest();
      request.setPlayerId(UUID.randomUUID());
      request.setRegion("GLOBAL");

      when(userResolver.resolve(null, httpRequest)).thenReturn(stubUser());
      when(snakeDraftService.validateAndAdvance(gameId, userId, "GLOBAL")).thenReturn(nextTurn);

      var response = controller.processPick(gameId, request, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData().participantId()).isEqualTo(nextParticipantId);
      verify(draftTrancheService).validatePick(gameId, "GLOBAL", request.getPlayerId());
      verify(gameDraftService).selectPlayer(gameId, userId, request.getPlayerId());
    }

    @Test
    void whenNotAuthenticated_returns401() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(null);

      var response = controller.processPick(gameId, new SnakePickRequest(), null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      verifyNoInteractions(snakeDraftService);
    }
  }
}
