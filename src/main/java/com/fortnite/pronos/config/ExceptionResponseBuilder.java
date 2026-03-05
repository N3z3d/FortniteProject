package com.fortnite.pronos.config;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fortnite.pronos.service.admin.ErrorEntry;
import com.fortnite.pronos.service.admin.ErrorJournalService;

/**
 * Shared builder for {@link GlobalExceptionHandler.ErrorResponse} responses, eliminating the
 * duplicated {@code buildAndRecordError} method that existed in both {@link DomainExceptionHandler}
 * and {@link GameExceptionHandler}.
 */
public final class ExceptionResponseBuilder {

  private ExceptionResponseBuilder() {}

  /**
   * Builds an error response, records it in the error journal, and wraps it in a {@link
   * ResponseEntity}.
   *
   * @param ex the exception to build the response from
   * @param request the current HTTP request (for path/URI)
   * @param status the HTTP status to return
   * @param error human-readable error label
   * @param code machine-readable error code
   * @param errorJournalService the journal to record the error in
   * @return a {@link ResponseEntity} with the error response body
   */
  public static ResponseEntity<GlobalExceptionHandler.ErrorResponse> buildAndRecord(
      Exception ex,
      HttpServletRequest request,
      HttpStatus status,
      String error,
      String code,
      ErrorJournalService errorJournalService) {

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
