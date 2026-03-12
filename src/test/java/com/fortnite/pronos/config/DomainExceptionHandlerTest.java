package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fortnite.pronos.exception.BusinessException;
import com.fortnite.pronos.exception.DraftIncompleteException;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.InvalidEpicIdException;
import com.fortnite.pronos.exception.InvalidSwapException;
import com.fortnite.pronos.exception.NotYourTurnException;
import com.fortnite.pronos.exception.PlayerAlreadySelectedException;
import com.fortnite.pronos.exception.PlayerIdentityNotFoundException;
import com.fortnite.pronos.exception.TeamNotFoundException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.service.admin.ErrorJournalService;

class DomainExceptionHandlerTest {

  private DomainExceptionHandler handler;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    handler = new DomainExceptionHandler(new ErrorJournalService());
    request = new MockHttpServletRequest();
    request.setRequestURI("/api/test");
  }

  @Test
  void handleBusinessExceptionReturnsBadRequest() {
    BusinessException ex = new BusinessException("Trade value exceeds cap");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleBusinessException(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("BUSINESS_RULE_VIOLATION");
    assertThat(response.getBody().getMessage()).isEqualTo("Trade value exceeds cap");
    assertThat(response.getBody().getError()).isEqualTo("Business Rule Violation");
  }

  @Test
  void handleUnauthorizedAccessExceptionReturnsForbidden() {
    UnauthorizedAccessException ex = new UnauthorizedAccessException("Not allowed to access game");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleUnauthorizedAccessException(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(403);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UNAUTHORIZED_ACCESS");
    assertThat(response.getBody().getMessage()).isEqualTo("Not allowed to access game");
  }

  @Test
  void handleTeamNotFoundExceptionReturnsNotFound() {
    TeamNotFoundException ex = new TeamNotFoundException("Team abc-123 not found");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleTeamNotFoundException(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("TEAM_NOT_FOUND");
    assertThat(response.getBody().getMessage()).contains("abc-123");
  }

  @Test
  void handleDraftIncompleteExceptionReturnsConflict() {
    DraftIncompleteException ex = new DraftIncompleteException("Draft is not complete");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleDraftIncompleteException(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("DRAFT_INCOMPLETE");
    assertThat(response.getBody().getMessage()).isEqualTo("Draft is not complete");
  }

  @Test
  void handleNotYourTurnExceptionReturnsConflict() {
    NotYourTurnException ex = new NotYourTurnException("It is not your turn to pick");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleNotYourTurnException(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("NOT_YOUR_TURN");
    assertThat(response.getBody().getMessage()).contains("not your turn");
  }

  @Test
  void handlePlayerAlreadySelectedExceptionReturnsConflict() {
    PlayerAlreadySelectedException ex =
        new PlayerAlreadySelectedException("Player already drafted");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handlePlayerAlreadySelectedException(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("PLAYER_ALREADY_SELECTED");
    assertThat(response.getBody().getMessage()).contains("already drafted");
  }

  @Test
  void handleInvalidSwapExceptionReturnsBadRequest() {
    InvalidSwapException ex = new InvalidSwapException("Cannot swap bench for starter");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleInvalidSwapException(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("INVALID_SWAP");
    assertThat(response.getBody().getMessage()).contains("Cannot swap");
  }

  @Test
  void handleInvalidEpicIdReturnsUnprocessableEntity() {
    InvalidEpicIdException ex = new InvalidEpicIdException("bad_epic_id");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleInvalidEpicId(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(422);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("INVALID_EPIC_ID");
    assertThat(response.getBody().getMessage()).contains("bad_epic_id");
  }

  @Test
  void handlePlayerIdentityNotFoundReturnsNotFound() {
    PlayerIdentityNotFoundException ex =
        new PlayerIdentityNotFoundException(java.util.UUID.randomUUID());

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handlePlayerIdentityNotFound(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("PLAYER_IDENTITY_NOT_FOUND");
  }

  @Test
  void handleInvalidDraftStateExceptionReturnsConflict() {
    InvalidDraftStateException ex =
        new InvalidDraftStateException("No active draft found for game: abc-123");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleInvalidDraftState(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("INVALID_DRAFT_STATE");
    assertThat(response.getBody().getMessage()).contains("No active draft");
  }

  @Test
  void handleIllegalStateExceptionReturnsConflict() {
    IllegalStateException ex = new IllegalStateException("Window is not OPEN: abc-123");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleIllegalState(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("DRAFT_WINDOW_VIOLATION");
    assertThat(response.getBody().getMessage()).contains("Window is not OPEN");
  }

  @Test
  void handleIllegalStateExceptionAlreadySubmittedReturnsConflict() {
    IllegalStateException ex =
        new IllegalStateException("Participant p1 already submitted in window w1");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleIllegalState(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody().getCode()).isEqualTo("DRAFT_WINDOW_VIOLATION");
    assertThat(response.getBody().getMessage()).contains("already submitted");
  }

  @Test
  void allHandlersIncludeRequestPath() {
    request.setRequestURI("/api/trades/propose");
    BusinessException ex = new BusinessException("test");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleBusinessException(ex, request);

    assertThat(response.getBody().getPath()).isEqualTo("/api/trades/propose");
  }

  @Test
  void allHandlersIncludeTimestamp() {
    BusinessException ex = new BusinessException("test");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        handler.handleBusinessException(ex, request);

    assertThat(response.getBody().getTimestamp()).isNotNull();
  }
}
