package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.fortnite.pronos.exception.GameFullException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameRequestException;
import com.fortnite.pronos.exception.UserAlreadyInGameException;
import com.fortnite.pronos.service.admin.ErrorJournalService;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler(new ErrorJournalService());
    request = new MockHttpServletRequest();
    request.setRequestURI("/api/games");
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
    assertThat(response.getBody().getMessage()).contains("Marcel");
    assertThat(response.getBody().getMessage()).contains("Championship 2026");
  }

  @Test
  void handleInvalidGameRequestExceptionReturnsConflictWithMessage() {
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

  @Test
  void handleGlobalExceptionMapsAlreadyInGameFallbackToConflict() {
    request.setRequestURI("/api/games/join-with-code");
    RuntimeException exception = new RuntimeException("User is already in this game");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleGlobalException(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("USER_ALREADY_IN_GAME");
    assertThat(response.getBody().getMessage()).contains("already in this game");
    assertThat(response.getBody().getPath()).isEqualTo("/api/games/join-with-code");
  }

  @Test
  void handleGlobalExceptionMapsWrappedAlreadyInGameFallbackToConflict() {
    request.setRequestURI("/api/games/join-with-code");
    RuntimeException exception =
        new RuntimeException(
            "Request processing failed",
            new UserAlreadyInGameException("User is already participating in this game"));

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleGlobalException(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("USER_ALREADY_IN_GAME");
    assertThat(response.getBody().getMessage()).contains("already participating");
    assertThat(response.getBody().getPath()).isEqualTo("/api/games/join-with-code");
  }

  @Test
  void handleValidationExceptionSupportsGlobalErrorsWithoutClassCast() throws Exception {
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "joinGameWithCodeRequest");
    bindingResult.addError(
        new ObjectError("joinGameWithCodeRequest", "Le code d'invitation est requis"));

    Method method = this.getClass().getDeclaredMethod("dummyValidationTarget", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(parameter, bindingResult);
    request.setRequestURI("/api/games/join-with-code");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleValidationException(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getValidationErrors())
        .containsEntry("joinGameWithCodeRequest", "Le code d'invitation est requis");
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
  void handleGlobalExceptionReturns500ForUnknownException() {
    RuntimeException exception = new RuntimeException("Something unexpected");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleGlobalException(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
  }

  @SuppressWarnings("unused")
  private void dummyValidationTarget(String value) {}
}
