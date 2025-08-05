package com.fortnite.pronos.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified service response with essential accessibility support Reduces complexity while
 * maintaining WCAG compliance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleServiceResponse<T> {

  /** Operation success status */
  private boolean success;

  /** Response data */
  private T data;

  /** User-friendly message */
  private String message;

  /** Optional screen reader announcement (only when different from message) */
  private String announcement;

  /** Simple error information */
  private SimpleError error;

  /** Timestamp */
  private LocalDateTime timestamp;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SimpleError {
    /** Error code for programmatic handling */
    private String code;

    /** User-friendly error message */
    private String message;

    /** Field name for validation errors */
    private String field;

    /** Single suggested action */
    private String suggestion;
  }

  /** Factory method for successful operations */
  public static <T> SimpleServiceResponse<T> success(T data, String message) {
    return SimpleServiceResponse.<T>builder()
        .success(true)
        .data(data)
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /** Factory method for successful operations with custom announcement */
  public static <T> SimpleServiceResponse<T> success(T data, String message, String announcement) {
    return SimpleServiceResponse.<T>builder()
        .success(true)
        .data(data)
        .message(message)
        .announcement(announcement)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /** Factory method for failed operations */
  public static <T> SimpleServiceResponse<T> failure(String message, String code) {
    return SimpleServiceResponse.<T>builder()
        .success(false)
        .message(message)
        .error(SimpleError.builder().code(code).message(message).build())
        .timestamp(LocalDateTime.now())
        .build();
  }

  /** Factory method for validation errors */
  public static <T> SimpleServiceResponse<T> validationError(
      String field, String message, String suggestion) {
    return SimpleServiceResponse.<T>builder()
        .success(false)
        .message(message)
        .error(
            SimpleError.builder()
                .code("VALIDATION_ERROR")
                .message(message)
                .field(field)
                .suggestion(suggestion)
                .build())
        .timestamp(LocalDateTime.now())
        .build();
  }
}
