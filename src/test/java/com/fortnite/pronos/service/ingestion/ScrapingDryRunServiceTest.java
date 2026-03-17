package com.fortnite.pronos.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.admin.DryRunResultDto;
import com.fortnite.pronos.model.PrRegion;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScrapingDryRunService")
class ScrapingDryRunServiceTest {

  @Mock private PrRegionCsvSourcePort csvSourcePort;

  private ScrapingDryRunService service;

  @BeforeEach
  void setUp() {
    service = new ScrapingDryRunService(csvSourcePort);
  }

  private String buildCsv(int rowCount, int points) {
    StringBuilder sb = new StringBuilder("nickname,region,points,rank,snapshot_date\n");
    for (int i = 1; i <= rowCount; i++) {
      sb.append("Player")
          .append(i)
          .append(",EU,")
          .append(points)
          .append(",")
          .append(i)
          .append(",2026-03-17\n");
    }
    return sb.toString();
  }

  @Nested
  @DisplayName("Happy path")
  class HappyPath {

    @Test
    @DisplayName("returns valid=true when adapter returns well-formed CSV with 15 rows")
    void returnsValid_whenAdapterReturnsGoodCsv() {
      when(csvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.of(buildCsv(15, 12500)));

      DryRunResultDto result = service.runDryRun(PrRegion.EU);

      assertThat(result.valid()).isTrue();
      assertThat(result.rowCount()).isEqualTo(15);
      assertThat(result.region()).isEqualTo("EU");
      assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("sampleRows is capped at 5 even when more rows are present")
    void capsSampleRowsAt5() {
      when(csvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.of(buildCsv(15, 12500)));

      DryRunResultDto result = service.runDryRun(PrRegion.EU);

      assertThat(result.sampleRows()).hasSize(5);
    }

    @Test
    @DisplayName("forwards the region parameter to the port")
    void forwardsRegionToPort() {
      when(csvSourcePort.fetchCsv(PrRegion.NAC)).thenReturn(Optional.of(buildCsv(10, 9000)));

      service.runDryRun(PrRegion.NAC);

      verify(csvSourcePort).fetchCsv(PrRegion.NAC);
    }
  }

  @Nested
  @DisplayName("Failure cases")
  class FailureCases {

    @Test
    @DisplayName("returns invalid when adapter returns empty Optional")
    void returnsInvalid_whenAdapterReturnsEmpty() {
      when(csvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.empty());

      DryRunResultDto result = service.runDryRun(PrRegion.EU);

      assertThat(result.valid()).isFalse();
      assertThat(result.rowCount()).isEqualTo(0);
      assertThat(result.errors()).isNotEmpty();
      assertThat(result.errors().get(0)).contains("check application logs");
    }

    @Test
    @DisplayName("fails smoke check when rowCount is below 10")
    void failsSmoke_whenRowCountBelow10() {
      when(csvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.of(buildCsv(7, 5000)));

      DryRunResultDto result = service.runDryRun(PrRegion.EU);

      assertThat(result.valid()).isFalse();
      assertThat(result.rowCount()).isEqualTo(7);
      assertThat(result.errors()).anyMatch(e -> e.contains("smoke") || e.contains("Smoke"));
    }

    @Test
    @DisplayName("fails score validation when points is 0 (below minimum)")
    void failsScore_whenPointsIsZero() {
      when(csvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.of(buildCsv(12, 0)));

      DryRunResultDto result = service.runDryRun(PrRegion.EU);

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).anyMatch(e -> e.contains("Score") || e.contains("score"));
    }

    @Test
    @DisplayName("fails score validation when points exceed 9_999_999")
    void failsScore_whenPointsExceedMax() {
      when(csvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.of(buildCsv(12, 10_000_000)));

      DryRunResultDto result = service.runDryRun(PrRegion.EU);

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).anyMatch(e -> e.contains("Score") || e.contains("score"));
    }

    @Test
    @DisplayName("fails with malformed row error when a row has fewer than 3 fields")
    void failsMalformedRow_whenFieldsAreMissing() {
      String malformedCsv =
          "nickname,region,points,rank,snapshot_date\n"
              + "Player1,EU\n" // only 2 fields — missing points
              + "Player2,EU,12500,2,2026-03-17\n";
      when(csvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.of(malformedCsv));

      DryRunResultDto result = service.runDryRun(PrRegion.EU);

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).anyMatch(e -> e.contains("Malformed") || e.contains("malformed"));
    }

    @Test
    @DisplayName("errors list in returned DTO is immutable")
    void errorsListIsImmutable() {
      when(csvSourcePort.fetchCsv(PrRegion.EU)).thenReturn(Optional.empty());

      DryRunResultDto result = service.runDryRun(PrRegion.EU);

      org.junit.jupiter.api.Assertions.assertThrows(
          UnsupportedOperationException.class, () -> result.errors().add("hack"));
    }
  }
}
