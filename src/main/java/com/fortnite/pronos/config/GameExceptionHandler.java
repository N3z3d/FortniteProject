package com.fortnite.pronos.config;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fortnite.pronos.exception.GameFullException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameRequestException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.UserAlreadyInGameException;
import com.fortnite.pronos.service.admin.ErrorJournalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles game lifecycle exceptions and keeps the global handler focused on infrastructure.
 * Delegates response building to {@link ExceptionResponseBuilder} to eliminate duplication with
 * {@link DomainExceptionHandler}.
 */
@ControllerAdvice
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class GameExceptionHandler {

  private final ErrorJournalService errorJournalService;

  @ExceptionHandler(GameNotFoundException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleGameNotFoundException(
      GameNotFoundException ex, HttpServletRequest request) {
    log.warn("Game not found: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex, request, HttpStatus.NOT_FOUND, "Game Not Found", "GAME_NOT_FOUND", errorJournalService);
  }

  @ExceptionHandler(GameFullException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleGameFullException(
      GameFullException ex, HttpServletRequest request) {
    log.warn("Game is full: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex, request, HttpStatus.CONFLICT, "Game Full", "GAME_FULL", errorJournalService);
  }

  @ExceptionHandler(UserAlreadyInGameException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleUserAlreadyInGameException(
      UserAlreadyInGameException ex, HttpServletRequest request) {
    log.warn("User already in game: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.CONFLICT,
        "User Already In Game",
        "USER_ALREADY_IN_GAME",
        errorJournalService);
  }

  @ExceptionHandler(InvalidGameStateException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleInvalidGameStateException(
      InvalidGameStateException ex, HttpServletRequest request) {
    log.warn("Invalid game state: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.CONFLICT,
        "Invalid Game State",
        "INVALID_GAME_STATE",
        errorJournalService);
  }

  @ExceptionHandler(InvalidGameRequestException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleInvalidGameRequestException(
      InvalidGameRequestException ex, HttpServletRequest request) {
    log.warn("Invalid game request: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.BAD_REQUEST,
        "Invalid Game Request",
        "INVALID_GAME_REQUEST",
        errorJournalService);
  }
}
