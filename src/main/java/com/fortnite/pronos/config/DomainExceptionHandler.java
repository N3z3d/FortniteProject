package com.fortnite.pronos.config;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fortnite.pronos.exception.AccountDeletionBlockedException;
import com.fortnite.pronos.exception.BusinessException;
import com.fortnite.pronos.exception.DraftIncompleteException;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.InvalidEpicIdException;
import com.fortnite.pronos.exception.InvalidInvitationCodeException;
import com.fortnite.pronos.exception.InvalidSwapException;
import com.fortnite.pronos.exception.InvalidTrancheViolationException;
import com.fortnite.pronos.exception.NotYourTurnException;
import com.fortnite.pronos.exception.PlayerAlreadySelectedException;
import com.fortnite.pronos.exception.PlayerIdentityNotFoundException;
import com.fortnite.pronos.exception.TeamNotFoundException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.service.admin.ErrorJournalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles domain-specific exceptions for trade, draft, and team operations. Extracted from
 * GlobalExceptionHandler to respect the 500-line class limit. Delegates response building to {@link
 * ExceptionResponseBuilder} to eliminate duplication with {@link GameExceptionHandler}.
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
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.BAD_REQUEST,
        "Business Rule Violation",
        "BUSINESS_RULE_VIOLATION",
        errorJournalService);
  }

  @ExceptionHandler(UnauthorizedAccessException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleUnauthorizedAccessException(
      UnauthorizedAccessException ex, HttpServletRequest request) {
    log.warn("Unauthorized access: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.FORBIDDEN,
        "Unauthorized Access",
        "UNAUTHORIZED_ACCESS",
        errorJournalService);
  }

  @ExceptionHandler(TeamNotFoundException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleTeamNotFoundException(
      TeamNotFoundException ex, HttpServletRequest request) {
    log.warn("Team not found: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex, request, HttpStatus.NOT_FOUND, "Team Not Found", "TEAM_NOT_FOUND", errorJournalService);
  }

  @ExceptionHandler(DraftIncompleteException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleDraftIncompleteException(
      DraftIncompleteException ex, HttpServletRequest request) {
    log.warn("Draft incomplete: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.CONFLICT,
        "Draft Incomplete",
        "DRAFT_INCOMPLETE",
        errorJournalService);
  }

  @ExceptionHandler(NotYourTurnException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleNotYourTurnException(
      NotYourTurnException ex, HttpServletRequest request) {
    log.warn("Not your turn: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex, request, HttpStatus.CONFLICT, "Not Your Turn", "NOT_YOUR_TURN", errorJournalService);
  }

  @ExceptionHandler(PlayerAlreadySelectedException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handlePlayerAlreadySelectedException(
      PlayerAlreadySelectedException ex, HttpServletRequest request) {
    log.warn("Player already selected: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.CONFLICT,
        "Player Already Selected",
        "PLAYER_ALREADY_SELECTED",
        errorJournalService);
  }

  @ExceptionHandler(InvalidSwapException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleInvalidSwapException(
      InvalidSwapException ex, HttpServletRequest request) {
    log.warn("Invalid swap: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex, request, HttpStatus.BAD_REQUEST, "Invalid Swap", "INVALID_SWAP", errorJournalService);
  }

  @ExceptionHandler(InvalidTrancheViolationException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleInvalidTrancheViolation(
      InvalidTrancheViolationException ex, HttpServletRequest request) {
    log.warn("Tranche violation: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.BAD_REQUEST,
        "Tranche Violation",
        "INVALID_TRANCHE_VIOLATION",
        errorJournalService);
  }

  @ExceptionHandler(InvalidEpicIdException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleInvalidEpicId(
      InvalidEpicIdException ex, HttpServletRequest request) {
    log.warn("Invalid Epic ID: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Invalid Epic ID",
        "INVALID_EPIC_ID",
        errorJournalService);
  }

  @ExceptionHandler(InvalidInvitationCodeException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleInvalidInvitationCode(
      InvalidInvitationCodeException ex, HttpServletRequest request) {
    log.warn("Invalid invitation code: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.BAD_REQUEST,
        "Invalid Invitation Code",
        "INVALID_INVITATION_CODE",
        errorJournalService);
  }

  @ExceptionHandler(AccountDeletionBlockedException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleAccountDeletionBlocked(
      AccountDeletionBlockedException ex, HttpServletRequest request) {
    log.warn("Account deletion blocked: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.CONFLICT,
        "Account Deletion Blocked",
        "ACCOUNT_DELETION_BLOCKED",
        errorJournalService);
  }

  @ExceptionHandler(PlayerIdentityNotFoundException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handlePlayerIdentityNotFound(
      PlayerIdentityNotFoundException ex, HttpServletRequest request) {
    log.warn("Player identity not found: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.NOT_FOUND,
        "Player Identity Not Found",
        "PLAYER_IDENTITY_NOT_FOUND",
        errorJournalService);
  }

  @ExceptionHandler(InvalidDraftStateException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleInvalidDraftState(
      InvalidDraftStateException ex, HttpServletRequest request) {
    log.warn("Invalid draft state: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.CONFLICT,
        "Invalid Draft State",
        "INVALID_DRAFT_STATE",
        errorJournalService);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleIllegalState(
      IllegalStateException ex, HttpServletRequest request) {
    log.warn("Draft window state violation: {}", ex.getMessage());
    return ExceptionResponseBuilder.buildAndRecord(
        ex,
        request,
        HttpStatus.CONFLICT,
        "Draft Window Violation",
        "DRAFT_WINDOW_VIOLATION",
        errorJournalService);
  }
}
