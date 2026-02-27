package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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
  void handleGlobalExceptionReturns500ForUnknownException() {
    RuntimeException exception = new RuntimeException("Something unexpected");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleGlobalException(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
  }

  @Test
  void handlerMethodsUseGeneralExceptionTypes() throws Exception {
    assertThat(
            GlobalExceptionHandler.class.getDeclaredMethod(
                    "handleAccessDeniedException",
                    Throwable.class,
                    jakarta.servlet.http.HttpServletRequest.class)
                .getParameterTypes()[0])
        .isEqualTo(Throwable.class);

    assertThat(
            GlobalExceptionHandler.class.getDeclaredMethod(
                    "handleValidationException",
                    BindException.class,
                    jakarta.servlet.http.HttpServletRequest.class)
                .getParameterTypes()[0])
        .isEqualTo(BindException.class);

    assertThat(
            GlobalExceptionHandler.class.getDeclaredMethod(
                    "handleConstraintViolationException",
                    Throwable.class,
                    jakarta.servlet.http.HttpServletRequest.class)
                .getParameterTypes()[0])
        .isEqualTo(Throwable.class);

    assertThat(
            GlobalExceptionHandler.class.getDeclaredMethod(
                    "handleIllegalArgumentException",
                    Throwable.class,
                    jakarta.servlet.http.HttpServletRequest.class)
                .getParameterTypes()[0])
        .isEqualTo(Throwable.class);

    assertThat(
            GlobalExceptionHandler.class.getDeclaredMethod(
                    "handleIllegalStateException",
                    Throwable.class,
                    jakarta.servlet.http.HttpServletRequest.class)
                .getParameterTypes()[0])
        .isEqualTo(Throwable.class);

    assertThat(
            GlobalExceptionHandler.class.getDeclaredMethod(
                    "handleSecurityException",
                    Throwable.class,
                    jakarta.servlet.http.HttpServletRequest.class)
                .getParameterTypes()[0])
        .isEqualTo(Throwable.class);
  }

  @SuppressWarnings("unused")
  private void dummyValidationTarget(String value) {}
}
