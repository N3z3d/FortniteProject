package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.dto.admin.ErrorStatisticsDto;
import com.fortnite.pronos.service.admin.ErrorEntry;
import com.fortnite.pronos.service.admin.ErrorJournalService;

@ExtendWith(MockitoExtension.class)
class ErrorJournalControllerTest {

  @Mock private ErrorJournalService errorJournalService;

  private ErrorJournalController controller;

  @BeforeEach
  void setUp() {
    controller = new ErrorJournalController(errorJournalService);
  }

  @Nested
  class GetErrors {

    @Test
    void shouldReturnErrorsWithDefaultLimit() {
      ErrorEntry entry = buildEntry("TestException", "error msg", 500);
      when(errorJournalService.getRecentErrors(50, null, null)).thenReturn(List.of(entry));

      var response = controller.getErrors(50, null, null);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().isSuccess()).isTrue();
      assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void shouldPassFiltersToService() {
      when(errorJournalService.getRecentErrors(10, 400, "Business")).thenReturn(List.of());

      var response = controller.getErrors(10, 400, "Business");

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      verify(errorJournalService).getRecentErrors(10, 400, "Business");
    }

    @Test
    void shouldClampLimitToMax() {
      when(errorJournalService.getRecentErrors(500, null, null)).thenReturn(List.of());

      controller.getErrors(9999, null, null);

      verify(errorJournalService).getRecentErrors(500, null, null);
    }

    @Test
    void shouldClampLimitToMin() {
      when(errorJournalService.getRecentErrors(1, null, null)).thenReturn(List.of());

      controller.getErrors(-5, null, null);

      verify(errorJournalService).getRecentErrors(1, null, null);
    }
  }

  @Nested
  class GetErrorStatistics {

    @Test
    void shouldReturnStatistics() {
      ErrorStatisticsDto stats =
          ErrorStatisticsDto.builder()
              .totalErrors(5)
              .errorsByType(Map.of("TestEx", 3L, "OtherEx", 2L))
              .errorsByStatusCode(Map.of(400, 2L, 500, 3L))
              .topErrors(List.of())
              .build();
      when(errorJournalService.getErrorStatistics(24)).thenReturn(stats);

      var response = controller.getErrorStatistics(24);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData().getTotalErrors()).isEqualTo(5);
    }

    @Test
    void shouldClampHoursToValidRange() {
      when(errorJournalService.getErrorStatistics(168))
          .thenReturn(ErrorStatisticsDto.builder().totalErrors(0).build());

      controller.getErrorStatistics(9999);

      verify(errorJournalService).getErrorStatistics(168);
    }
  }

  @Nested
  class GetErrorDetail {

    @Test
    void shouldReturnEntryWhenFound() {
      ErrorEntry entry = buildEntry("TestEx", "test msg", 500);
      when(errorJournalService.findById(entry.getId())).thenReturn(Optional.of(entry));

      var response = controller.getErrorDetail(entry.getId());

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData().getExceptionType()).isEqualTo("TestEx");
    }

    @Test
    void shouldReturn404WhenNotFound() {
      UUID unknownId = UUID.randomUUID();
      when(errorJournalService.findById(unknownId)).thenReturn(Optional.empty());

      var response = controller.getErrorDetail(unknownId);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody().isSuccess()).isFalse();
    }
  }

  private ErrorEntry buildEntry(String type, String message, int statusCode) {
    return ErrorEntry.builder()
        .id(UUID.randomUUID())
        .timestamp(LocalDateTime.now())
        .exceptionType(type)
        .message(message)
        .statusCode(statusCode)
        .errorCode("TEST")
        .path("/api/test")
        .stackTrace("stack trace")
        .build();
  }
}
