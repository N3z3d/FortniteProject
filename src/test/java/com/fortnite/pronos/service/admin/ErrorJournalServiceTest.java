package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.dto.admin.ErrorStatisticsDto;

class ErrorJournalServiceTest {

  private ErrorJournalService service;

  @BeforeEach
  void setUp() {
    service = new ErrorJournalService();
  }

  @Nested
  class RecordError {

    @Test
    void shouldRecordSingleError() {
      ErrorEntry entry = buildEntry("TestException", "Something failed", 500);

      service.recordError(entry);

      assertThat(service.getCurrentSize()).isEqualTo(1);
    }

    @Test
    void shouldRecordMultipleErrors() {
      service.recordError(buildEntry("Ex1", "msg1", 400));
      service.recordError(buildEntry("Ex2", "msg2", 500));
      service.recordError(buildEntry("Ex3", "msg3", 404));

      assertThat(service.getCurrentSize()).isEqualTo(3);
    }

    @Test
    void shouldEvictOldestWhenBufferFull() {
      for (int i = 0; i < ErrorJournalService.MAX_ENTRIES + 10; i++) {
        service.recordError(buildEntry("Ex", "msg" + i, 500));
      }

      assertThat(service.getCurrentSize()).isEqualTo(ErrorJournalService.MAX_ENTRIES);
    }

    @Test
    void shouldKeepMostRecentEntriesWhenEvicting() {
      ErrorEntry oldEntry = buildEntry("OldException", "old message", 400);
      service.recordError(oldEntry);

      for (int i = 0; i < ErrorJournalService.MAX_ENTRIES; i++) {
        service.recordError(buildEntry("NewException", "new" + i, 500));
      }

      List<ErrorEntry> results =
          service.getRecentErrors(ErrorJournalService.MAX_ENTRIES, null, null);
      assertThat(results)
          .noneMatch(
              e ->
                  "OldException".equals(e.getExceptionType())
                      && "old message".equals(e.getMessage()));
    }
  }

  @Nested
  class GetRecentErrors {

    @Test
    void shouldReturnEmptyWhenNoErrors() {
      List<ErrorEntry> results = service.getRecentErrors(10, null, null);

      assertThat(results).isEmpty();
    }

    @Test
    void shouldReturnMostRecentFirst() {
      service.recordError(buildEntry("First", "first", 400));
      service.recordError(buildEntry("Second", "second", 500));

      List<ErrorEntry> results = service.getRecentErrors(10, null, null);

      assertThat(results).hasSize(2);
      assertThat(results.get(0).getExceptionType()).isEqualTo("Second");
    }

    @Test
    void shouldRespectLimit() {
      for (int i = 0; i < 20; i++) {
        service.recordError(buildEntry("Ex", "msg" + i, 500));
      }

      List<ErrorEntry> results = service.getRecentErrors(5, null, null);

      assertThat(results).hasSize(5);
    }

    @Test
    void shouldFilterByStatusCode() {
      service.recordError(buildEntry("Ex1", "bad request", 400));
      service.recordError(buildEntry("Ex2", "server error", 500));
      service.recordError(buildEntry("Ex3", "another bad", 400));

      List<ErrorEntry> results = service.getRecentErrors(10, 400, null);

      assertThat(results).hasSize(2);
      assertThat(results).allMatch(e -> e.getStatusCode() == 400);
    }

    @Test
    void shouldFilterByExceptionType() {
      service.recordError(buildEntry("GameNotFoundException", "game not found", 404));
      service.recordError(buildEntry("UserNotFoundException", "user not found", 404));
      service.recordError(buildEntry("BusinessException", "business error", 400));

      List<ErrorEntry> results = service.getRecentErrors(10, null, "NotFound");

      assertThat(results).hasSize(2);
    }

    @Test
    void shouldFilterByBothStatusAndType() {
      service.recordError(buildEntry("GameNotFoundException", "game 1", 404));
      service.recordError(buildEntry("GameNotFoundException", "game 2", 404));
      service.recordError(buildEntry("BusinessException", "biz error", 400));

      List<ErrorEntry> results = service.getRecentErrors(10, 404, "Game");

      assertThat(results).hasSize(2);
    }
  }

  @Nested
  class FindById {

    @Test
    void shouldFindExistingEntry() {
      ErrorEntry entry = buildEntry("TestEx", "test msg", 500);
      service.recordError(entry);

      Optional<ErrorEntry> result = service.findById(entry.getId());

      assertThat(result).isPresent();
      assertThat(result.get().getExceptionType()).isEqualTo("TestEx");
    }

    @Test
    void shouldReturnEmptyForUnknownId() {
      Optional<ErrorEntry> result = service.findById(UUID.randomUUID());

      assertThat(result).isEmpty();
    }
  }

  @Nested
  class GetErrorStatistics {

    @Test
    void shouldReturnEmptyStatisticsWhenNoErrors() {
      ErrorStatisticsDto stats = service.getErrorStatistics(24);

      assertThat(stats.getTotalErrors()).isZero();
      assertThat(stats.getErrorsByType()).isEmpty();
      assertThat(stats.getErrorsByStatusCode()).isEmpty();
      assertThat(stats.getTopErrors()).isEmpty();
    }

    @Test
    void shouldCountErrorsByType() {
      service.recordError(buildEntry("GameNotFoundException", "msg1", 404));
      service.recordError(buildEntry("GameNotFoundException", "msg2", 404));
      service.recordError(buildEntry("BusinessException", "msg3", 400));

      ErrorStatisticsDto stats = service.getErrorStatistics(24);

      assertThat(stats.getTotalErrors()).isEqualTo(3);
      assertThat(stats.getErrorsByType()).containsEntry("GameNotFoundException", 2L);
      assertThat(stats.getErrorsByType()).containsEntry("BusinessException", 1L);
    }

    @Test
    void shouldCountErrorsByStatusCode() {
      service.recordError(buildEntry("Ex1", "msg1", 400));
      service.recordError(buildEntry("Ex2", "msg2", 500));
      service.recordError(buildEntry("Ex3", "msg3", 400));

      ErrorStatisticsDto stats = service.getErrorStatistics(24);

      assertThat(stats.getErrorsByStatusCode()).containsEntry(400, 2L);
      assertThat(stats.getErrorsByStatusCode()).containsEntry(500, 1L);
    }

    @Test
    void shouldBuildTopErrorsSortedByCount() {
      for (int i = 0; i < 5; i++) {
        service.recordError(buildEntry("FrequentEx", "frequent error", 500));
      }
      service.recordError(buildEntry("RareEx", "rare error", 400));

      ErrorStatisticsDto stats = service.getErrorStatistics(24);

      assertThat(stats.getTopErrors()).isNotEmpty();
      assertThat(stats.getTopErrors().get(0).getType()).isEqualTo("FrequentEx");
      assertThat(stats.getTopErrors().get(0).getCount()).isEqualTo(5);
    }
  }

  @Nested
  class ClearAll {

    @Test
    void shouldClearAllEntries() {
      service.recordError(buildEntry("Ex1", "msg1", 400));
      service.recordError(buildEntry("Ex2", "msg2", 500));

      service.clearAll();

      assertThat(service.getCurrentSize()).isZero();
      assertThat(service.getRecentErrors(10, null, null)).isEmpty();
    }
  }

  @Nested
  class ThreadSafety {

    @Test
    void shouldHandleConcurrentWrites() throws InterruptedException {
      int threadCount = 10;
      int entriesPerThread = 100;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        executor.submit(
            () -> {
              try {
                for (int i = 0; i < entriesPerThread; i++) {
                  service.recordError(buildEntry("Ex" + threadId, "msg" + i, 400 + (threadId % 3)));
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await();
      executor.shutdown();

      assertThat(service.getCurrentSize()).isLessThanOrEqualTo(ErrorJournalService.MAX_ENTRIES);
      assertThat(service.getRecentErrors(ErrorJournalService.MAX_ENTRIES, null, null)).isNotEmpty();
    }
  }

  private ErrorEntry buildEntry(String type, String message, int statusCode) {
    return ErrorEntry.builder()
        .id(UUID.randomUUID())
        .timestamp(LocalDateTime.now())
        .exceptionType(type)
        .message(message)
        .statusCode(statusCode)
        .errorCode("TEST_CODE")
        .path("/api/test")
        .stackTrace("test stack trace")
        .build();
  }
}
