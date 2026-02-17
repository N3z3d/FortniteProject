package com.fortnite.pronos.config;

import java.time.LocalDateTime;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;

/**
 * Encapsulates the fallback logic for detecting "already in game" errors that may not be thrown as
 * UserAlreadyInGameException directly (e.g., wrapped in RuntimeException).
 */
final class AlreadyInGameFallbackHelper {

  private AlreadyInGameFallbackHelper() {}

  static boolean isAlreadyInGameFallback(Exception ex, HttpServletRequest request) {
    if (hasAlreadyInGameExceptionInCauseChain(ex)) {
      return true;
    }
    if (!isJoinRequest(request)) {
      return false;
    }
    return hasAlreadyInGameMessage(ex.getMessage()) || hasAlreadyInGameMessageInCauseChain(ex);
  }

  static GlobalExceptionHandler.ErrorResponse buildAlreadyInGameErrorResponse(
      String exceptionMessage, HttpServletRequest request) {
    String message =
        (exceptionMessage == null || exceptionMessage.isBlank())
            ? "User is already participating in this game"
            : exceptionMessage;

    return GlobalExceptionHandler.ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.CONFLICT.value())
        .error("User Already In Game")
        .message(message)
        .path(request.getRequestURI())
        .code("USER_ALREADY_IN_GAME")
        .build();
  }

  static String extractAlreadyInGameMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      String currentMessage = current.getMessage();
      if (hasAlreadyInGameMessage(currentMessage)) {
        return currentMessage;
      }
      if ("UserAlreadyInGameException".equals(current.getClass().getSimpleName())
          && currentMessage != null
          && !currentMessage.isBlank()) {
        return currentMessage;
      }
      current = current.getCause();
    }
    return throwable == null ? null : throwable.getMessage();
  }

  private static boolean hasAlreadyInGameExceptionInCauseChain(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if ("UserAlreadyInGameException".equals(current.getClass().getSimpleName())) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private static boolean hasAlreadyInGameMessageInCauseChain(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (hasAlreadyInGameMessage(current.getMessage())) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private static boolean isJoinRequest(HttpServletRequest request) {
    String requestPath = request.getRequestURI();
    return requestPath != null && requestPath.contains("/join");
  }

  private static boolean hasAlreadyInGameMessage(String message) {
    if (message == null || message.isBlank()) {
      return false;
    }
    String normalizedMessage = message.toLowerCase(Locale.ROOT);
    return normalizedMessage.contains("already in this game")
        || normalizedMessage.contains("already participating")
        || normalizedMessage.contains("deja dans cette partie");
  }
}
