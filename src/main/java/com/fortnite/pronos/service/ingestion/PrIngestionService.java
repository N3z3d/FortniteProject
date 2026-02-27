package com.fortnite.pronos.service.ingestion;

import java.io.Reader;
import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
@SuppressWarnings({"java:S135"})
public class PrIngestionService {
  private static final String DEFAULT_SOURCE = "LOCAL_PR_CSV";
  private static final int DEFAULT_SEASON = 2025;

  private final PrCsvParser parser;
  private final PrIngestionRowProcessor rowProcessor;
  private final com.fortnite.pronos.repository.IngestionRunRepository ingestionRunRepository;

  public PrIngestionResult ingest(Reader reader) {
    return ingest(reader, PrIngestionConfig.defaults());
  }

  public PrIngestionResult ingest(Reader reader, PrIngestionConfig config) {
    Objects.requireNonNull(reader, "reader");
    PrIngestionConfig safeConfig = config == null ? PrIngestionConfig.defaults() : config;
    log.info(
        "PR ingestion start: source={}, season={}, writeScores={}",
        safeConfig.source(),
        safeConfig.season(),
        safeConfig.writeScores());

    com.fortnite.pronos.model.IngestionRun run = startRun(safeConfig.source());
    PrCsvParser.ParseResult parseResult = parser.parse(reader);

    if (parseResult.failureReason() != null) {
      log.warn(
          "PR ingestion failed: runId={}, reason={}, parseErrors={}",
          run.getId(),
          parseResult.failureReason(),
          parseResult.errorCount());
      return failRun(run, parseResult.failureReason(), parseResult.errorCount());
    }

    log.debug(
        "PR parsing ok: rows={}, errors={}", parseResult.rows().size(), parseResult.errorCount());
    PrIngestionCounters counters = rowProcessor.persistRows(parseResult.rows(), safeConfig, run);
    com.fortnite.pronos.model.IngestionRun.Status status =
        resolveStatus(counters, parseResult.errorCount());
    String message = buildMessage(status, counters, parseResult.errorCount());
    finishRun(run, status, counters.snapshotsWritten(), message);

    log.info(
        "PR ingestion end: runId={}, status={}, playersCreated={}, playersUpdated={}, "
            + "snapshots={}, scores={}, skipped={}, parseErrors={}",
        run.getId(),
        status,
        counters.playersCreated(),
        counters.playersUpdated(),
        counters.snapshotsWritten(),
        counters.scoresWritten(),
        counters.skippedRows(),
        parseResult.errorCount());
    return buildResult(run, status, counters, parseResult.errorCount());
  }

  private com.fortnite.pronos.model.IngestionRun startRun(String source) {
    com.fortnite.pronos.model.IngestionRun run = new com.fortnite.pronos.model.IngestionRun();
    run.setSource(source);
    run.setStatus(com.fortnite.pronos.model.IngestionRun.Status.RUNNING);
    return ingestionRunRepository.save(run);
  }

  private PrIngestionResult failRun(
      com.fortnite.pronos.model.IngestionRun run, String reason, int parseErrors) {
    String message = "parse_failed:" + reason;
    finishRun(run, com.fortnite.pronos.model.IngestionRun.Status.FAILED, 0, message);
    return buildResult(
        run,
        com.fortnite.pronos.model.IngestionRun.Status.FAILED,
        PrIngestionCounters.empty(),
        parseErrors);
  }

  private com.fortnite.pronos.model.IngestionRun.Status resolveStatus(
      PrIngestionCounters counters, int parseErrors) {
    if (parseErrors > 0 || counters.skippedRows() > 0) {
      return com.fortnite.pronos.model.IngestionRun.Status.PARTIAL;
    }
    return com.fortnite.pronos.model.IngestionRun.Status.SUCCESS;
  }

  private String buildMessage(
      com.fortnite.pronos.model.IngestionRun.Status status,
      PrIngestionCounters counters,
      int parseErrors) {
    if (status == com.fortnite.pronos.model.IngestionRun.Status.SUCCESS) {
      return null;
    }
    if (status == com.fortnite.pronos.model.IngestionRun.Status.FAILED) {
      return "ingestion_failed";
    }
    return String.format("parse_errors=%d,skipped=%d", parseErrors, counters.skippedRows());
  }

  private void finishRun(
      com.fortnite.pronos.model.IngestionRun run,
      com.fortnite.pronos.model.IngestionRun.Status status,
      int totalRows,
      String errorMessage) {
    run.setStatus(status);
    run.setFinishedAt(OffsetDateTime.now());
    run.setTotalRowsWritten(totalRows);
    if (errorMessage != null && !errorMessage.isBlank()) {
      run.setErrorMessage(errorMessage);
    }
    ingestionRunRepository.save(run);
  }

  private PrIngestionResult buildResult(
      com.fortnite.pronos.model.IngestionRun run,
      com.fortnite.pronos.model.IngestionRun.Status status,
      PrIngestionCounters counters,
      int parseErrors) {
    return new PrIngestionResult(
        run.getId(),
        status,
        counters.playersCreated(),
        counters.playersUpdated(),
        counters.snapshotsWritten(),
        counters.scoresWritten(),
        counters.skippedRows(),
        parseErrors);
  }

  public record PrIngestionConfig(String source, int season, boolean writeScores) {
    public static PrIngestionConfig defaults() {
      return new PrIngestionConfig(DEFAULT_SOURCE, DEFAULT_SEASON, true);
    }
  }

  public record PrIngestionResult(
      java.util.UUID runId,
      com.fortnite.pronos.model.IngestionRun.Status status,
      int playersCreated,
      int playersUpdated,
      int snapshotsWritten,
      int scoresWritten,
      int skippedRows,
      int parseErrors) {}
}
