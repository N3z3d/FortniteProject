package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.admin.DryRunResultDto;
import com.fortnite.pronos.dto.admin.IngestionTriggerResultDto;
import com.fortnite.pronos.dto.admin.PipelineAlertDto;
import com.fortnite.pronos.dto.admin.PipelineAlertDto.AlertLevel;
import com.fortnite.pronos.dto.admin.ScrapeLogDto;
import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.service.admin.ScrapeLogService;
import com.fortnite.pronos.service.admin.UnresolvedAlertService;
import com.fortnite.pronos.service.ingestion.PrIngestionOrchestrationService;
import com.fortnite.pronos.service.ingestion.PrIngestionOrchestrationService.BatchStatus;
import com.fortnite.pronos.service.ingestion.PrIngestionOrchestrationService.MultiRegionIngestionResult;
import com.fortnite.pronos.service.ingestion.ScrapingDryRunService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminScrapeController")
class AdminScrapeControllerTest {

  @Mock private ScrapeLogService scrapeLogService;
  @Mock private UnresolvedAlertService unresolvedAlertService;
  @Mock private ScrapingDryRunService scrapingDryRunService;
  @Mock private PrIngestionOrchestrationService orchestrationService;

  private AdminScrapeController controllerWithScheduler() {
    return new AdminScrapeController(
        scrapeLogService,
        unresolvedAlertService,
        scrapingDryRunService,
        Optional.of(orchestrationService));
  }

  private AdminScrapeController controllerWithoutScheduler() {
    return new AdminScrapeController(
        scrapeLogService, unresolvedAlertService, scrapingDryRunService, Optional.empty());
  }

  private ScrapeLogDto sampleLog() {
    return new ScrapeLogDto(
        UUID.randomUUID(),
        "EU",
        OffsetDateTime.now().minusHours(2),
        OffsetDateTime.now().minusHours(1),
        "SUCCESS",
        100,
        null);
  }

  private PipelineAlertDto alertDto(AlertLevel level) {
    return new PipelineAlertDto(level, 0L, null, 0L, OffsetDateTime.now());
  }

  @Nested
  @DisplayName("GET /logs")
  class GetLogs {

    @Test
    @DisplayName("returns 200 with list of logs")
    void returns200WithLogs() {
      when(scrapeLogService.getRecentLogs(50)).thenReturn(List.of(sampleLog()));

      var response = controllerWithoutScheduler().getLogs(50);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).hasSize(1);
      verify(scrapeLogService).getRecentLogs(50);
    }

    @Test
    @DisplayName("returns 200 with empty list when no logs")
    void returns200Empty() {
      when(scrapeLogService.getRecentLogs(50)).thenReturn(List.of());

      var response = controllerWithoutScheduler().getLogs(50);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("forwards custom limit to service")
    void forwardsCustomLimit() {
      when(scrapeLogService.getRecentLogs(10)).thenReturn(List.of());

      controllerWithoutScheduler().getLogs(10);

      verify(scrapeLogService).getRecentLogs(10);
    }
  }

  @Nested
  @DisplayName("GET /alert")
  class GetAlert {

    @Test
    @DisplayName("returns 200 with NONE level when no unresolved")
    void returnsNoneAlert() {
      when(unresolvedAlertService.getAlertStatus()).thenReturn(alertDto(AlertLevel.NONE));

      var response = controllerWithoutScheduler().getAlert();

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().level()).isEqualTo(AlertLevel.NONE);
      assertThat(response.getBody().checkedAt()).isNotNull();
    }

    @Test
    @DisplayName("returns 200 with CRITICAL level when alert is escalated")
    void returnsCriticalAlert() {
      OffsetDateTime oldest = OffsetDateTime.now().minusHours(72);
      PipelineAlertDto alert =
          new PipelineAlertDto(AlertLevel.CRITICAL, 5L, oldest, 72L, OffsetDateTime.now());
      when(unresolvedAlertService.getAlertStatus()).thenReturn(alert);

      var response = controllerWithoutScheduler().getAlert();

      assertThat(response.getBody().level()).isEqualTo(AlertLevel.CRITICAL);
      assertThat(response.getBody().unresolvedCount()).isEqualTo(5L);
      assertThat(response.getBody().oldestUnresolvedAt()).isEqualTo(oldest);
    }
  }

  @Nested
  @DisplayName("POST /dry-run")
  class PostDryRun {

    @Test
    @DisplayName("returns 200 with valid result when service returns success")
    void returns200WithValidResult() {
      DryRunResultDto dto = new DryRunResultDto("EU", 15, true, List.of(), List.of());
      when(scrapingDryRunService.runDryRun(PrRegion.EU)).thenReturn(dto);

      var response = controllerWithoutScheduler().dryRun("EU");

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("returns 400 for unknown region string")
    void returns400ForUnknownRegion() {
      var response = controllerWithoutScheduler().dryRun("UNKNOWN");

      assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("forwards NAC region to dry-run service")
    void forwardsRegionToService() {
      DryRunResultDto dto = new DryRunResultDto("NAC", 10, true, List.of(), List.of());
      when(scrapingDryRunService.runDryRun(PrRegion.NAC)).thenReturn(dto);

      controllerWithoutScheduler().dryRun("NAC");

      org.mockito.Mockito.verify(scrapingDryRunService).runDryRun(PrRegion.NAC);
    }
  }

  @Nested
  @DisplayName("POST /trigger")
  class PostTrigger {

    @Test
    @DisplayName("returns 200 with SUCCESS result when scheduler enabled")
    void trigger_returnsOk_whenSchedulerEnabled() {
      MultiRegionIngestionResult serviceResult =
          new MultiRegionIngestionResult(BatchStatus.SUCCESS, 8, Map.of(), 1234L);
      when(orchestrationService.runAllRegions()).thenReturn(serviceResult);

      var response = controllerWithScheduler().triggerIngestion();

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      IngestionTriggerResultDto dto = (IngestionTriggerResultDto) response.getBody();
      assertThat(dto).isNotNull();
      assertThat(dto.status()).isEqualTo("SUCCESS");
      assertThat(dto.regionsProcessed()).isEqualTo(8);
      assertThat(dto.regionFailures()).isEmpty();
      assertThat(dto.durationMs()).isEqualTo(1234L);
    }

    @Test
    @DisplayName("returns 503 when scheduler disabled")
    void trigger_returns503_whenSchedulerDisabled() {
      var response = controllerWithoutScheduler().triggerIngestion();

      assertThat(response.getStatusCode().value()).isEqualTo(503);
    }

    @Test
    @DisplayName("maps region failures to string keys in DTO")
    void trigger_mapsFailuresToDto() {
      MultiRegionIngestionResult serviceResult =
          new MultiRegionIngestionResult(
              BatchStatus.PARTIAL, 7, Map.of(PrRegion.NAW, "smoke_check_failed"), 5000L);
      when(orchestrationService.runAllRegions()).thenReturn(serviceResult);

      var response = controllerWithScheduler().triggerIngestion();

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      IngestionTriggerResultDto dto = (IngestionTriggerResultDto) response.getBody();
      assertThat(dto).isNotNull();
      assertThat(dto.status()).isEqualTo("PARTIAL");
      assertThat(dto.regionsProcessed()).isEqualTo(7);
      assertThat(dto.regionFailures()).containsEntry("NAW", "smoke_check_failed");
    }
  }
}
