package com.fortnite.pronos.config;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fortnite.pronos.exception.AccountDeletionBlockedException;
import com.fortnite.pronos.exception.BusinessException;
import com.fortnite.pronos.exception.DraftIncompleteException;
import com.fortnite.pronos.exception.InvalidEpicIdException;
import com.fortnite.pronos.exception.InvalidInvitationCodeException;
import com.fortnite.pronos.exception.InvalidSwapException;
import com.fortnite.pronos.exception.NotYourTurnException;
import com.fortnite.pronos.exception.PlayerAlreadySelectedException;
import com.fortnite.pronos.exception.PlayerIdentityNotFoundException;
import com.fortnite.pronos.exception.TeamNotFoundException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.service.admin.ErrorEntry;
import com.fortnite.pronos.service.admin.ErrorJournalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles domain-specific exceptions for trade, draft, and team operations. Extracted from
 * GlobalExceptionHandler to respect the 500-line class limit.
 */
@ControllerAdvice
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class DomainExceptionHandler {

  private final ErrorJournalService errorJournalService;

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleBusinessException(
      BusinessException ex, HttpServletRequest request) {
    log.warn("Business rule violation: {}", ex.getMessage());
    return buildAndRecordError(
        ex, request, HttpStatus.BAD_REQUEST, "Business Rule Violation", "BUSINESS_RULE_VIOLATION");
  }

  @ExceptionHandler(UnauthorizedAccessException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleUnauthorizedAccessException(
      UnauthorizedAccessException ex, HttpServletRequest request) {
    log.warn("Unauthorized access: {}", ex.getMessage());
    return buildAndRecordError(
        ex, request, HttpStatus.FORBIDDEN, "Unauthorized Access", "UNAUTHORIZED_ACCESS");
  }

  @ExceptionHandler(TeamNotFoundException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleTeamNotFoundException(
      TeamNotFoundException ex, HttpServletRequest request) {
    log.warn("Team not found: {}", ex.getMessage());
    return buildAndRecordError(
        ex, request, HttpStatus.NOT_FOUND, "Team Not Found", "TEAM_NOT_FOUND");
  }

  @ExceptionHandler(DraftIncompleteException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleDraftIncompleteException(
      DraftIncompleteException ex, HttpServletRequest request) {
    log.warn("Draft incomplete: {}", ex.getMessage());
    return buildAndRecordError(
        ex, request, HttpStatus.CONFLICT, "Draft Incomplete", "DRAFT_INCOMPLETE");
  }

  @ExceptionHandler(NotYourTurnException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleNotYourTurnException(
      NotYourTurnException ex, HttpServletRequest request) {
    log.warn("Not your turn: {}", ex.getMessage());
    return buildAndRecordError(ex, request, HttpStatus.CONFLICT, "Not Your Turn", "NOT_YOUR_TURN");
  }

  @ExceptionHandler(PlayerAlreadySelectedException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handlePlayerAlreadySelectedException(
      PlayerAlreadySelectedException ex, HttpServletRequest request) {
    log.warn("Player already selected: {}", ex.getMessage());
    return buildAndRecordError(
        ex, request, HttpStatus.CONFLICT, "Player Already Selected", "PLAYER_ALREADY_SELECTED");
  }

  @ExceptionHandler(InvalidSwapException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleInvalidSwapException(
      InvalidSwapException ex, HttpServletRequest request) {
    log.warn("Invalid swap: {}", ex.getMessage());
    return buildAndRecordError(ex, request, HttpStatus.BAD_REQUEST, "Invalid Swap", "INVALID_SWAP");
  }

  @ExceptionHandler(InvalidEpicIdException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleInvalidEpicId(
      InvalidEpicIdException ex, HttpServletRequest request) {
    log.warn("Invalid Epic ID: {}", ex.getMessage());
    return buildAndRecordError(
        ex, request, HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Epic ID", "INVALID_EPIC_ID");
  }

  @ExceptionHandler(InvalidInvitationCodeException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleInvalidInvitationCode(
      InvalidInvitationCodeException ex, HttpServletRequest request) {
    log.warn("Invalid invitation code: {}", ex.getMessage());
    return buildAndRecordError(
        ex, request, HttpStatus.BAD_REQUEST, "Invalid Invitation Code", "INVALID_INVITATION_CODE");
  }

  @ExceptionHandler(AccountDeletionBlockedException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleAccountDeletionBlocked(
      AccountDeletionBlockedException ex, HttpServletRequest request) {
    log.warn("Account deletion blocked: {}", ex.getMessage());
    return buildAndRecordError(
        ex, request, HttpStatus.CONFLICT, "Account Deletion Blocked", "ACCOUNT_DELETION_BLOCKED");
  }

  @ExceptionHandler(PlayerIdentityNotFoundException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handlePlayerIdentityNotFound(
      PlayerIdentityNotFoundException ex, HttpServletRequest request) {
    log.warn("Player identity not found: {}", ex.getMessage());
    return buildAndRecordError(
        ex,
        request,
        HttpStatus.NOT_FOUND,
        "Player Identity Not Found",
        "PLAYER_IDENTITY_NOT_FOUND");
  }

  private ResponseEntity<GlobalExceptionHandler.ErrorResponse> buildAndRecordError(
      Exception ex, HttpServletRequest request, HttpStatus status, String error, String code) {

    GlobalExceptionHandler.ErrorResponse errorResponse =
        GlobalExceptionHandler.ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(error)
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .code(code)
            .build();

    errorJournalService.recordError(ErrorEntry.from(ex, request, errorResponse));
    return ResponseEntity.status(status).body(errorResponse);
  }
}
