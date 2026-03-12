package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fortnite.pronos.service.admin.ErrorJournalService;

@DisplayName("ExceptionResponseBuilder")
class ExceptionResponseBuilderTest {

  private ErrorJournalService errorJournalService;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    errorJournalService = mock(ErrorJournalService.class);
    request = new MockHttpServletRequest();
    request.setRequestURI("/api/test");
  }

  @Test
  @DisplayName("buildAndRecord returns response with correct HTTP status")
  void returnsCorrectStatus() {
    Exception ex = new RuntimeException("test error");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        ExceptionResponseBuilder.buildAndRecord(
            ex, request, HttpStatus.BAD_REQUEST, "Bad Request", "BAD_REQUEST", errorJournalService);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
  }

  @Test
  @DisplayName("buildAndRecord returns response with correct error code")
  void returnsCorrectCode() {
    Exception ex = new RuntimeException("test error");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        ExceptionResponseBuilder.buildAndRecord(
            ex, request, HttpStatus.CONFLICT, "Conflict", "MY_CODE", errorJournalService);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("MY_CODE");
  }

  @Test
  @DisplayName("buildAndRecord returns response with correct path")
  void returnsCorrectPath() {
    Exception ex = new RuntimeException("test error");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        ExceptionResponseBuilder.buildAndRecord(
            ex, request, HttpStatus.NOT_FOUND, "Not Found", "NOT_FOUND", errorJournalService);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getPath()).isEqualTo("/api/test");
  }

  @Test
  @DisplayName("buildAndRecord delegates to errorJournalService.recordError")
  void delegatesToErrorJournalService() {
    Exception ex = new RuntimeException("test error");

    ExceptionResponseBuilder.buildAndRecord(
        ex, request, HttpStatus.INTERNAL_SERVER_ERROR, "Error", "ERR", errorJournalService);

    verify(errorJournalService).recordError(any());
  }
}
