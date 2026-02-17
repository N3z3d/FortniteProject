package com.fortnite.pronos.service.admin;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import com.fortnite.pronos.config.GlobalExceptionHandler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Immutable snapshot of a handled exception for admin monitoring. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class ErrorEntry {

  private static final int MAX_STACK_TRACE_LENGTH = 1000;

  private UUID id;
  private LocalDateTime timestamp;
  private String exceptionType;
  private String message;
  private int statusCode;
  private String errorCode;
  private String path;
  private String stackTrace;

  /**
   * Creates an ErrorEntry from an exception, request, and error response.
   *
   * @param ex the caught exception
   * @param request the HTTP request that triggered the exception
   * @param response the error response sent to the client
   * @return a new ErrorEntry capturing the error details
   */
  public static ErrorEntry from(
      Exception ex, HttpServletRequest request, GlobalExceptionHandler.ErrorResponse response) {
    return ErrorEntry.builder()
        .id(UUID.randomUUID())
        .timestamp(LocalDateTime.now())
        .exceptionType(ex.getClass().getSimpleName())
        .message(ex.getMessage() != null ? ex.getMessage() : "No message")
        .statusCode(response.getStatus())
        .errorCode(response.getCode() != null ? response.getCode() : "UNKNOWN")
        .path(request.getRequestURI())
        .stackTrace(truncateStackTrace(ex))
        .build();
  }

  private static String truncateStackTrace(Exception ex) {
    StringBuilder sb = new StringBuilder();
    sb.append(ex.toString());
    StackTraceElement[] elements = ex.getStackTrace();
    int limit = Math.min(elements.length, 10);
    for (int i = 0; i < limit; i++) {
      sb.append("\n\tat ").append(elements[i]);
    }
    if (sb.length() > MAX_STACK_TRACE_LENGTH) {
      return sb.substring(0, MAX_STACK_TRACE_LENGTH);
    }
    return sb.toString();
  }
}
