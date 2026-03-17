package com.fortnite.pronos.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
  @Mock private CsvCachePort csvCachePort;

  private PrIngestionOrchestrationService orchestrationService;
  private Clock clock;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-02-25T05:15:00Z"), ZoneOffset.UTC);
    orchestrationService =
        new PrIngestionOrchestrationService(
            ingestionService, regionCsvSourcePort, clock, csvCachePort);
  }

  private String csvWithRows(PrRegion region, int count) {
    StringBuilder sb = new StringBuilder("nickname,region,points,rank,snapshot_date\n");
    for (int i = 1; i <= count; i++) {
      sb.append("Player")
          .append(i)
          .append(",")
          .append(region)
          .append(",")
          .append(1000 + i)
          .append(",")
          .append(i)
          .append(",2026-03-17\n");
    }
    return sb.toString();
  }

  @Test
  @DisplayName("runs all 8 regions and returns SUCCESS when all ingestions succeed")
  void runsAllEightRegionsWithinWindow() {
    for (PrRegion region : PrIngestionOrchestrationService.SUPPORTED_REGIONS) {
      when(regionCsvSourcePort.fetchCsv(region)).thenReturn(Optional.of(csvWithRows(region, 11)));
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
  @DisplayName("continues processing all regions when one fails and marks batch PARTIAL")
  void continuesWhenOneRegionFailsAndMarksBatchPartial() {
    for (PrRegion region : PrIngestionOrchestrationService.SUPPORTED_REGIONS) {
      when(regionCsvSourcePort.fetchCsv(region)).thenReturn(Optional.of(csvWithRows(region, 11)));
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
  @DisplayName("skips ingestion entirely when called outside 05h-08h UTC window")
  void skipsRunOutsideFiveToEightWindow() {
    Clock outsideWindowClock = Clock.fixed(Instant.parse("2026-02-25T09:00:00Z"), ZoneOffset.UTC);
    PrIngestionOrchestrationService outsideWindowService =
        new PrIngestionOrchestrationService(
            ingestionService, regionCsvSourcePort, outsideWindowClock, csvCachePort);

    PrIngestionOrchestrationService.MultiRegionIngestionResult result =
        outsideWindowService.runScheduledIngestion();

    assertThat(result.status()).isEqualTo(PrIngestionOrchestrationService.BatchStatus.SKIPPED);
    assertThat(result.regionsProcessed()).isZero();
    verify(regionCsvSourcePort, never()).fetchCsv(any());
    verify(ingestionService, never()).ingest(any(Reader.class), any());
  }

  @Nested
  @DisplayName("processRegion() — cache and smoke check")
  class ProcessRegionTests {

    @Test
    @DisplayName("uses cache fallback when fetchCsv returns empty")
    void processRegion_usesCacheFallback_whenFetchCsvEmpty() {
      String cachedCsv = csvWithRows(PrRegion.EU, 11);
      when(regionCsvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.empty());
      when(csvCachePort.load(PrRegion.EU)).thenReturn(Optional.of(cachedCsv));

      for (PrRegion region : PrIngestionOrchestrationService.SUPPORTED_REGIONS) {
        if (region != PrRegion.EU) {
          when(regionCsvSourcePort.fetchCsv(region))
              .thenReturn(Optional.of(csvWithRows(region, 11)));
        }
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
      verify(csvCachePort, times(1)).load(PrRegion.EU);
      verify(csvCachePort, never()).save(eq(PrRegion.EU), any());
      verify(ingestionService, times(8)).ingest(any(Reader.class), any());
    }

    @Test
    @DisplayName("skips ingestion when smoke check fails (fewer than 10 rows)")
    void processRegion_skipsIngestion_whenSmokeCheckFails() {
      String shortCsv = csvWithRows(PrRegion.EU, 5);
      when(regionCsvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.of(shortCsv));
      for (PrRegion region : PrIngestionOrchestrationService.SUPPORTED_REGIONS) {
        if (region != PrRegion.EU) {
          when(regionCsvSourcePort.fetchCsv(region))
              .thenReturn(Optional.of(csvWithRows(region, 11)));
        }
      }
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
      assertThat(result.regionFailures()).containsEntry(PrRegion.EU, "smoke_check_failed");
      verify(ingestionService, times(7)).ingest(any(Reader.class), any());
    }

    @Test
    @DisplayName("saves CSV to cache after successful ingestion")
    void processRegion_savesToCache_afterSuccessfulIngestion() {
      String validCsv = csvWithRows(PrRegion.EU, 11);
      when(regionCsvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.of(validCsv));
      for (PrRegion region : PrIngestionOrchestrationService.SUPPORTED_REGIONS) {
        if (region != PrRegion.EU) {
          when(regionCsvSourcePort.fetchCsv(region))
              .thenReturn(Optional.of(csvWithRows(region, 11)));
        }
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

      orchestrationService.runScheduledIngestion();

      verify(csvCachePort, times(1)).save(PrRegion.EU, validCsv);
    }

    @Test
    @DisplayName("saves CSV to cache even when ingestion returns non-SUCCESS status")
    void processRegion_savesToCache_evenWhenIngestionFails() {
      String validCsv = csvWithRows(PrRegion.EU, 11);
      when(regionCsvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.of(validCsv));
      for (PrRegion region : PrIngestionOrchestrationService.SUPPORTED_REGIONS) {
        if (region != PrRegion.EU) {
          when(regionCsvSourcePort.fetchCsv(region))
              .thenReturn(Optional.of(csvWithRows(region, 11)));
        }
      }
      when(ingestionService.ingest(any(Reader.class), any()))
          .thenReturn(failedResult())
          .thenReturn(successResult())
          .thenReturn(successResult())
          .thenReturn(successResult())
          .thenReturn(successResult())
          .thenReturn(successResult())
          .thenReturn(successResult())
          .thenReturn(successResult());

      orchestrationService.runScheduledIngestion();

      // Cache is updated after smoke check passes, before ingestion result is known
      verify(csvCachePort, times(1)).save(PrRegion.EU, validCsv);
    }

    @Test
    @DisplayName("returns no_data when both live and cache are empty")
    void processRegion_returnsNoData_whenBothLiveAndCacheEmpty() {
      when(regionCsvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.empty());
      when(csvCachePort.load(PrRegion.EU)).thenReturn(Optional.empty());
      for (PrRegion region : PrIngestionOrchestrationService.SUPPORTED_REGIONS) {
        if (region != PrRegion.EU) {
          when(regionCsvSourcePort.fetchCsv(region))
              .thenReturn(Optional.of(csvWithRows(region, 11)));
        }
      }
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

      assertThat(result.regionFailures()).containsEntry(PrRegion.EU, "no_data");
      assertThat(result.status()).isEqualTo(PrIngestionOrchestrationService.BatchStatus.PARTIAL);
    }
  }

  @Nested
  @DisplayName("runAllRegions() — bypasses window check")
  class RunAllRegionsTests {

    @Test
    @DisplayName("processes all 8 regions regardless of time window (outside 05h-08h)")
    void runAllRegions_processesAllRegionsRegardlessOfWindow() {
      Clock outsideClock = Clock.fixed(Instant.parse("2026-02-25T09:00:00Z"), ZoneOffset.UTC);
      PrIngestionOrchestrationService outsideService =
          new PrIngestionOrchestrationService(
              ingestionService, regionCsvSourcePort, outsideClock, csvCachePort);

      for (PrRegion region : PrIngestionOrchestrationService.SUPPORTED_REGIONS) {
        when(regionCsvSourcePort.fetchCsv(region)).thenReturn(Optional.of(csvWithRows(region, 11)));
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
          outsideService.runAllRegions();

      assertThat(result.status()).isEqualTo(PrIngestionOrchestrationService.BatchStatus.SUCCESS);
      assertThat(result.regionsProcessed()).isEqualTo(8);
      assertThat(result.regionFailures()).isEmpty();
      verify(ingestionService, times(8)).ingest(any(Reader.class), any());
    }

    @Test
    @DisplayName("returns PARTIAL when one region fetch throws exception")
    void runAllRegions_returnsPartial_whenOneRegionFails() {
      for (PrRegion region : PrIngestionOrchestrationService.SUPPORTED_REGIONS) {
        when(regionCsvSourcePort.fetchCsv(region)).thenReturn(Optional.of(csvWithRows(region, 11)));
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
          orchestrationService.runAllRegions();

      assertThat(result.status()).isEqualTo(PrIngestionOrchestrationService.BatchStatus.PARTIAL);
      assertThat(result.regionsProcessed()).isEqualTo(7);
      assertThat(result.regionFailures()).containsKey(PrRegion.NAW);
    }
  }

  private PrIngestionResult successResult() {
    return new PrIngestionResult(
        UUID.randomUUID(), com.fortnite.pronos.model.IngestionRun.Status.SUCCESS, 1, 0, 1, 1, 0, 0);
  }

  private PrIngestionResult failedResult() {
    return new PrIngestionResult(
        UUID.randomUUID(), com.fortnite.pronos.model.IngestionRun.Status.FAILED, 0, 1, 0, 0, 1, 0);
  }
}
