package com.fortnite.pronos.dto.common;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API Response wrapper for all endpoints
 *
 * <p>Provides consistent response format across the entire API with: - Success/error status
 * indication - Structured data payload - Meaningful messages - Timestamp for debugging - Pagination
 * support - Error details when applicable
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

  @Schema(description = "Indicates if the operation was successful", example = "true")
  private boolean success;

  @Schema(description = "Response data payload")
  private T data;

  @Schema(
      description = "Human-readable message describing the result",
      example = "Operation completed successfully")
  private String message;

  @Schema(description = "Response timestamp", example = "2025-08-03T10:30:00")
  private LocalDateTime timestamp;

  @Schema(description = "Pagination information for list responses")
  private PaginationInfo pagination;

  @Schema(description = "Error details when success is false")
  private ErrorDetails error;

  /** Create successful response with data */
  public static <T> ApiResponse<T> success(T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .data(data)
        .message("Operation completed successfully")
        .timestamp(LocalDateTime.now())
        .build();
  }

  /** Create successful response with data and custom message */
  public static <T> ApiResponse<T> success(T data, String message) {
    return ApiResponse.<T>builder()
        .success(true)
        .data(data)
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /** Create successful response with pagination */
  public static <T> ApiResponse<T> success(T data, PaginationInfo pagination) {
    return ApiResponse.<T>builder()
        .success(true)
        .data(data)
        .message("Operation completed successfully")
        .pagination(pagination)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /** Create error response */
  public static <T> ApiResponse<T> error(String message, String errorCode) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .error(ErrorDetails.builder().code(errorCode).message(message).build())
        .timestamp(LocalDateTime.now())
        .build();
  }

  /** Create error response with details */
  public static <T> ApiResponse<T> error(String message, String errorCode, List<String> details) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .error(ErrorDetails.builder().code(errorCode).message(message).details(details).build())
        .timestamp(LocalDateTime.now())
        .build();
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Pagination information")
  public static class PaginationInfo {
    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;

    @Schema(description = "Number of items per page", example = "20")
    private int size;

    @Schema(description = "Total number of items", example = "150")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "8")
    private int totalPages;

    @Schema(description = "Whether this is the first page", example = "true")
    private boolean first;

    @Schema(description = "Whether this is the last page", example = "false")
    private boolean last;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Error details")
  public static class ErrorDetails {
    @Schema(description = "Error code for programmatic handling", example = "VALIDATION_ERROR")
    private String code;

    @Schema(description = "Human-readable error message", example = "Validation failed")
    private String message;

    @Schema(description = "Additional error details")
    private List<String> details;

    @Schema(description = "Field-specific validation errors")
    private List<FieldError> fieldErrors;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Field-specific validation error")
  public static class FieldError {
    @Schema(description = "Field name that failed validation", example = "email")
    private String field;

    @Schema(description = "Rejected value", example = "invalid-email")
    private Object rejectedValue;

    @Schema(description = "Validation error message", example = "must be a valid email address")
    private String message;
  }
}
