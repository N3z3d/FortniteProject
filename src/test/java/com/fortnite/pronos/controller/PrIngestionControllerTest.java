package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fortnite.pronos.model.IngestionRun;
import com.fortnite.pronos.service.ingestion.PrIngestionService;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionConfig;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionResult;

@ExtendWith(MockitoExtension.class)
class PrIngestionControllerTest {

  @Mock private PrIngestionService ingestionService;

  @InjectMocks private PrIngestionController controller;

  @Test
  void returnsBadRequestWhenBodyIsEmpty() {
    ResponseEntity<?> response = controller.ingestCsv(" ", "LOCAL", 2025, true);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo(Map.of("error", "csv_body_required"));
    verifyNoInteractions(ingestionService);
  }

  @Test
  void forwardsCsvToService() {
    PrIngestionResult result =
        new PrIngestionResult(UUID.randomUUID(), IngestionRun.Status.SUCCESS, 1, 0, 1, 1, 0, 0);
    when(ingestionService.ingest(any(), any())).thenReturn(result);

    String csv = "nickname,region,points,rank,snapshot_date\npixie,EU,1,1,2025-01-10";
    ResponseEntity<?> response = controller.ingestCsv(csv, "LOCAL_PR", 2025, false);

    ArgumentCaptor<PrIngestionConfig> configCaptor =
        ArgumentCaptor.forClass(PrIngestionConfig.class);
    verify(ingestionService).ingest(any(), configCaptor.capture());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(result);
    assertThat(configCaptor.getValue().source()).isEqualTo("LOCAL_PR");
    assertThat(configCaptor.getValue().season()).isEqualTo(2025);
    assertThat(configCaptor.getValue().writeScores()).isFalse();
  }
}
