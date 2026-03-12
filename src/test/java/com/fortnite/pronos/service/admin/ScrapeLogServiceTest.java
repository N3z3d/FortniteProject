package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.port.out.IngestionRunRepositoryPort;
import com.fortnite.pronos.dto.admin.ScrapeLogDto;
import com.fortnite.pronos.model.IngestionRun;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScrapeLogService")
class ScrapeLogServiceTest {

  @Mock private IngestionRunRepositoryPort ingestionRunRepository;

  private ScrapeLogService service;

  @BeforeEach
  void setUp() {
    service = new ScrapeLogService(ingestionRunRepository);
  }

  private IngestionRun buildRun(
      String source, IngestionRun.Status status, Integer totalRows, String errorMsg) {
    IngestionRun run = new IngestionRun();
    run.setId(UUID.randomUUID());
    run.setSource(source);
    run.setStartedAt(OffsetDateTime.now().minusHours(2));
    run.setFinishedAt(OffsetDateTime.now().minusHours(1));
    run.setStatus(status);
    run.setTotalRowsWritten(totalRows);
    run.setErrorMessage(errorMsg);
    return run;
  }

  @Nested
  @DisplayName("getRecentLogs")
  class GetRecentLogs {

    @Test
    @DisplayName("returns DTOs sorted by startedAt DESC from repository")
    void returnsSortedList() {
      IngestionRun run = buildRun("EU", IngestionRun.Status.SUCCESS, 100, null);
      when(ingestionRunRepository.findRecentLogs(anyInt())).thenReturn(List.of(run));

      List<ScrapeLogDto> result = service.getRecentLogs(10);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).source()).isEqualTo("EU");
      assertThat(result.get(0).status()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("clamps limit below MIN_LIMIT to MIN_LIMIT and passes correct limit to port")
    void clampsLimitLow() {
      when(ingestionRunRepository.findRecentLogs(anyInt())).thenReturn(List.of());

      service.getRecentLogs(0);

      verify(ingestionRunRepository).findRecentLogs(ScrapeLogService.MIN_LIMIT);
    }

    @Test
    @DisplayName("clamps limit above MAX_LIMIT to MAX_LIMIT and passes correct limit to port")
    void clampsLimitHigh() {
      when(ingestionRunRepository.findRecentLogs(anyInt())).thenReturn(List.of());

      service.getRecentLogs(9999);

      verify(ingestionRunRepository).findRecentLogs(ScrapeLogService.MAX_LIMIT);
    }

    @Test
    @DisplayName("returns empty list when repository is empty")
    void emptyRepository() {
      when(ingestionRunRepository.findRecentLogs(anyInt())).thenReturn(List.of());

      List<ScrapeLogDto> result = service.getRecentLogs(50);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all DTO fields correctly")
    void mapsAllFields() {
      IngestionRun run = buildRun("GLOBAL", IngestionRun.Status.PARTIAL, 42, "partial error");
      when(ingestionRunRepository.findRecentLogs(anyInt())).thenReturn(List.of(run));

      ScrapeLogDto dto = service.getRecentLogs(1).get(0);

      assertThat(dto.id()).isEqualTo(run.getId());
      assertThat(dto.source()).isEqualTo("GLOBAL");
      assertThat(dto.status()).isEqualTo("PARTIAL");
      assertThat(dto.totalRowsWritten()).isEqualTo(42);
      assertThat(dto.errorMessage()).isEqualTo("partial error");
      assertThat(dto.startedAt()).isEqualTo(run.getStartedAt());
      assertThat(dto.finishedAt()).isEqualTo(run.getFinishedAt());
    }

    @Test
    @DisplayName("handles null finishedAt gracefully")
    void handlesNullFinishedAt() {
      IngestionRun run = buildRun("NAW", IngestionRun.Status.RUNNING, null, null);
      run.setFinishedAt(null);
      when(ingestionRunRepository.findRecentLogs(anyInt())).thenReturn(List.of(run));

      ScrapeLogDto dto = service.getRecentLogs(1).get(0);

      assertThat(dto.finishedAt()).isNull();
      assertThat(dto.totalRowsWritten()).isNull();
      assertThat(dto.errorMessage()).isNull();
    }

    @Test
    @DisplayName("passes exact clamped limit to port for nominal limit")
    void passesNominalLimit() {
      when(ingestionRunRepository.findRecentLogs(eq(50))).thenReturn(List.of());

      service.getRecentLogs(50);

      verify(ingestionRunRepository).findRecentLogs(50);
    }
  }
}
