package com.fortnite.pronos.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionResult;

@ExtendWith(MockitoExtension.class)
class PrIngestionOrchestrationServiceTest {

  @Mock private PrIngestionService ingestionService;
  @Mock private PrRegionCsvSourcePort regionCsvSourcePort;

  private PrIngestionOrchestrationService orchestrationService;
  private Clock clock;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-02-25T05:15:00Z"), ZoneOffset.UTC);
    orchestrationService =
        new PrIngestionOrchestrationService(ingestionService, regionCsvSourcePort, clock);
  }

  @Test
  void runsAllEightRegionsWithinWindow() {
    for (PrRegion region : PrIngestionOrchestrationService.SUPPORTED_REGIONS) {
      when(regionCsvSourcePort.fetchCsv(region))
          .thenReturn(
              Optional.of(
                  "nickname,region,points,rank,snapshot_date\nplayer,"
                      + region
                      + ",100,1,2026-02-25"));
    }
    when(ingestionService.ingest(any(Reader.class), any()))
        .thenReturn(successResult())
        .thenReturn(successResult())
        .thenReturn(successResult())
        .thenReturn(successResult())
        .thenReturn(successResult())
        .thenReturn(successResult())
        .thenReturn(successResult())
        .thenReturn(successResult());

    PrIngestionOrchestrationService.MultiRegionIngestionResult result =
        orchestrationService.runScheduledIngestion();

    assertThat(result.status()).isEqualTo(PrIngestionOrchestrationService.BatchStatus.SUCCESS);
    assertThat(result.regionsProcessed()).isEqualTo(8);
    assertThat(result.regionFailures()).isEmpty();
    verify(ingestionService, times(8)).ingest(any(Reader.class), any());
    verify(regionCsvSourcePort, times(1)).fetchCsv(PrRegion.EU);
    verify(regionCsvSourcePort, times(1)).fetchCsv(PrRegion.NAC);
    verify(regionCsvSourcePort, times(1)).fetchCsv(PrRegion.NAW);
    verify(regionCsvSourcePort, times(1)).fetchCsv(PrRegion.BR);
    verify(regionCsvSourcePort, times(1)).fetchCsv(PrRegion.ASIA);
    verify(regionCsvSourcePort, times(1)).fetchCsv(PrRegion.OCE);
    verify(regionCsvSourcePort, times(1)).fetchCsv(PrRegion.ME);
    verify(regionCsvSourcePort, times(1)).fetchCsv(PrRegion.GLOBAL);
  }

  @Test
  void continuesWhenOneRegionFailsAndMarksBatchPartial() {
    for (PrRegion region : PrIngestionOrchestrationService.SUPPORTED_REGIONS) {
      when(regionCsvSourcePort.fetchCsv(region))
          .thenReturn(
              Optional.of(
                  "nickname,region,points,rank,snapshot_date\nplayer,"
                      + region
                      + ",100,1,2026-02-25"));
    }
    when(regionCsvSourcePort.fetchCsv(PrRegion.NAW))
        .thenThrow(new IllegalStateException("source_down"));
    when(ingestionService.ingest(any(Reader.class), any()))
        .thenReturn(successResult())
        .thenReturn(successResult())
        .thenReturn(successResult())
        .thenReturn(successResult())
        .thenReturn(successResult())
        .thenReturn(successResult())
        .thenReturn(successResult());

    PrIngestionOrchestrationService.MultiRegionIngestionResult result =
        orchestrationService.runScheduledIngestion();

    assertThat(result.status()).isEqualTo(PrIngestionOrchestrationService.BatchStatus.PARTIAL);
    assertThat(result.regionsProcessed()).isEqualTo(7);
    assertThat(result.regionFailures()).containsKey(PrRegion.NAW);
    verify(ingestionService, times(7)).ingest(any(Reader.class), any());
  }

  @Test
  void skipsRunOutsideFiveToEightWindow() {
    Clock outsideWindowClock = Clock.fixed(Instant.parse("2026-02-25T09:00:00Z"), ZoneOffset.UTC);
    PrIngestionOrchestrationService outsideWindowService =
        new PrIngestionOrchestrationService(
            ingestionService, regionCsvSourcePort, outsideWindowClock);

    PrIngestionOrchestrationService.MultiRegionIngestionResult result =
        outsideWindowService.runScheduledIngestion();

    assertThat(result.status()).isEqualTo(PrIngestionOrchestrationService.BatchStatus.SKIPPED);
    assertThat(result.regionsProcessed()).isZero();
    verify(regionCsvSourcePort, never()).fetchCsv(any());
    verify(ingestionService, never()).ingest(any(Reader.class), any());
  }

  private PrIngestionResult successResult() {
    return new PrIngestionResult(
        UUID.randomUUID(), com.fortnite.pronos.model.IngestionRun.Status.SUCCESS, 1, 0, 1, 1, 0, 0);
  }
}
