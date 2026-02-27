package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fortnite.pronos.exception.GameFullException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameRequestException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.UserAlreadyInGameException;
import com.fortnite.pronos.service.admin.ErrorJournalService;

class GameExceptionHandlerTest {

  private GameExceptionHandler handler;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    handler = new GameExceptionHandler(new ErrorJournalService());
    request = new MockHttpServletRequest();
    request.setRequestURI("/api/games");
  }

  @Test
  void handleGameNotFoundExceptionReturnsNotFound() {
    GameNotFoundException exception = new GameNotFoundException("Game xyz not found");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleGameNotFoundException(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("GAME_NOT_FOUND");
    assertThat(response.getBody().getMessage()).contains("xyz");
  }

  @Test
  void handleGameFullExceptionReturnsConflict() {
    GameFullException exception = new GameFullException("Game is full (8/8 participants)");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleGameFullException(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("GAME_FULL");
    assertThat(response.getBody().getMessage()).contains("full");
  }

  @Test
  void handleUserAlreadyInGameExceptionReturnsConflictWithCode() {
    UserAlreadyInGameException exception =
        new UserAlreadyInGameException("User is already participating in this game");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleUserAlreadyInGameException(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("USER_ALREADY_IN_GAME");
    assertThat(response.getBody().getMessage()).contains("already participating");
    assertThat(response.getBody().getPath()).isEqualTo("/api/games");
  }

  @Test
  void handleUserAlreadyInGameExceptionWithUsernameAndGameName() {
    UserAlreadyInGameException exception =
        new UserAlreadyInGameException("Marcel", "Championship 2026");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleUserAlreadyInGameException(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody().getMessage()).contains("Marcel").contains("Championship 2026");
  }

  @Test
  void handleInvalidGameStateExceptionReturnsConflict() {
    InvalidGameStateException exception = new InvalidGameStateException("Game already started");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleInvalidGameStateException(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("INVALID_GAME_STATE");
    assertThat(response.getBody().getMessage()).contains("started");
  }

  @Test
  void handleInvalidGameRequestExceptionReturnsBadRequest() {
    InvalidGameRequestException exception =
        new InvalidGameRequestException("User cannot have more than 5 active games. Current: 5");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleInvalidGameRequestException(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("INVALID_GAME_REQUEST");
    assertThat(response.getBody().getMessage()).contains("more than 5 active games");
    assertThat(response.getBody().getPath()).isEqualTo("/api/games");
  }
}
