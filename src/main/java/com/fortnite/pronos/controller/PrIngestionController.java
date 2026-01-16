package com.fortnite.pronos.controller;

import java.io.StringReader;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.service.ingestion.PrIngestionService;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionConfig;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/ingestion/pr")
@Profile({"dev", "h2", "test"})
@ConditionalOnProperty(
    name = "ingestion.local.enabled",
    havingValue = "true",
    matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class PrIngestionController {
  private final PrIngestionService ingestionService;

  @PostMapping(
      value = "/csv",
      consumes = {MediaType.TEXT_PLAIN_VALUE, "text/csv"})
  public ResponseEntity<?> ingestCsv(
      @RequestBody(required = false) String csv,
      @RequestParam(defaultValue = "LOCAL_PR_CSV") String source,
      @RequestParam(defaultValue = "2025") int season,
      @RequestParam(defaultValue = "true") boolean writeScores) {
    if (csv == null || csv.isBlank()) {
      log.warn("PR ingestion rejected: empty CSV body");
      return ResponseEntity.badRequest().body(Map.of("error", "csv_body_required"));
    }

    int csvLength = csv.length();
    int lineCount = csv.split("\n").length;
    log.info(
        "PR ingestion request: source={}, season={}, csvLength={}, lines={}",
        source,
        season,
        csvLength,
        lineCount);

    PrIngestionResult result =
        ingestionService.ingest(
            new StringReader(csv), new PrIngestionConfig(source, season, writeScores));

    log.info(
        "PR ingestion response: status={}, playersCreated={}, snapshots={}",
        result.status(),
        result.playersCreated(),
        result.snapshotsWritten());
    return ResponseEntity.ok(result);
  }
}
