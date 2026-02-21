package com.fortnite.pronos.service.ingestion;

import java.io.Reader;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.domain.port.out.ScoreRepositoryPort;

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
  private final PlayerRepositoryPort playerRepository;
  private final com.fortnite.pronos.repository.PrSnapshotRepository prSnapshotRepository;
  private final ScoreRepositoryPort scoreRepository;
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
    Counters counters = persistRows(parseResult.rows(), safeConfig, run);
    com.fortnite.pronos.model.IngestionRun.Status status =
        resolveStatus(counters, parseResult.errorCount());
    String message = buildMessage(status, counters, parseResult.errorCount());
    finishRun(run, status, counters.snapshotsWritten, message);

    log.info(
        "PR ingestion end: runId={}, status={}, playersCreated={}, playersUpdated={}, "
            + "snapshots={}, scores={}, skipped={}, parseErrors={}",
        run.getId(),
        status,
        counters.playersCreated,
        counters.playersUpdated,
        counters.snapshotsWritten,
        counters.scoresWritten,
        counters.skippedRows,
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
        run, com.fortnite.pronos.model.IngestionRun.Status.FAILED, new Counters(), parseErrors);
  }

  private Counters persistRows(
      List<PrCsvParser.PrCsvRow> rows,
      PrIngestionConfig config,
      com.fortnite.pronos.model.IngestionRun run) {
    Counters counters = new Counters();
    for (PrCsvParser.PrCsvRow row : rows) {
      com.fortnite.pronos.model.PrRegion prRegion = toRegion(row.region());
      if (prRegion == null) {
        counters.skippedRows++;
        continue;
      }
      com.fortnite.pronos.model.Player player = resolvePlayer(row, prRegion, config, counters);
      if (player == null) {
        continue;
      }
      upsertSnapshot(player, prRegion, row, run, counters);
      if (config.writeScores()) {
        upsertScore(player, row, config.season(), counters);
      }
    }
    return counters;
  }

  private com.fortnite.pronos.model.Player resolvePlayer(
      PrCsvParser.PrCsvRow row,
      com.fortnite.pronos.model.PrRegion prRegion,
      PrIngestionConfig config,
      Counters counters) {
    if (prRegion == com.fortnite.pronos.model.PrRegion.GLOBAL) {
      return resolveGlobalPlayer(row, config, counters);
    }

    com.fortnite.pronos.model.Player.Region playerRegion = toPlayerRegion(prRegion);
    if (playerRegion == null) {
      counters.skippedRows++;
      return null;
    }
    return findOrCreatePlayer(row, playerRegion, config.season(), counters);
  }

  private com.fortnite.pronos.model.Player resolveGlobalPlayer(
      PrCsvParser.PrCsvRow row, PrIngestionConfig config, Counters counters) {
    Optional<com.fortnite.pronos.model.Player> existing =
        playerRepository.findByNickname(row.nickname());
    if (existing.isPresent()) {
      return existing.get();
    }
    String tranche = trancheFromRank(row.rank());
    return createPlayer(
        row, com.fortnite.pronos.model.Player.Region.UNKNOWN, tranche, config.season(), counters);
  }

  private com.fortnite.pronos.model.Player findOrCreatePlayer(
      PrCsvParser.PrCsvRow row,
      com.fortnite.pronos.model.Player.Region region,
      int season,
      Counters counters) {
    Optional<com.fortnite.pronos.model.Player> existing =
        playerRepository.findByNickname(row.nickname());
    String tranche = trancheFromRank(row.rank());

    if (existing.isPresent()) {
      com.fortnite.pronos.model.Player player = existing.get();
      if (applyPlayerUpdates(player, region, tranche, season)) {
        playerRepository.save(player);
        counters.playersUpdated++;
      }
      return player;
    }

    return createPlayer(row, region, tranche, season, counters);
  }

  private com.fortnite.pronos.model.Player createPlayer(
      PrCsvParser.PrCsvRow row,
      com.fortnite.pronos.model.Player.Region region,
      String tranche,
      int season,
      Counters counters) {
    com.fortnite.pronos.model.Player player = new com.fortnite.pronos.model.Player();
    player.setNickname(row.nickname());
    player.setUsername(buildUsername(row.nickname()));
    player.setRegion(region);
    player.setTranche(tranche);
    player.setCurrentSeason(season);
    playerRepository.save(player);
    counters.playersCreated++;
    return player;
  }

  private boolean applyPlayerUpdates(
      com.fortnite.pronos.model.Player player,
      com.fortnite.pronos.model.Player.Region region,
      String tranche,
      int season) {
    boolean updated = false;
    if (player.getRegion() != region) {
      player.setRegion(region);
      updated = true;
    }
    if (player.getTranche() == null || !player.getTranche().equals(tranche)) {
      player.setTranche(tranche);
      updated = true;
    }
    if (player.getCurrentSeason() == null || player.getCurrentSeason() != season) {
      player.setCurrentSeason(season);
      updated = true;
    }
    return updated;
  }

  private void upsertSnapshot(
      com.fortnite.pronos.model.Player player,
      com.fortnite.pronos.model.PrRegion region,
      PrCsvParser.PrCsvRow row,
      com.fortnite.pronos.model.IngestionRun run,
      Counters counters) {
    com.fortnite.pronos.model.PrSnapshot.PrSnapshotId id =
        new com.fortnite.pronos.model.PrSnapshot.PrSnapshotId(
            player.getId(), region, row.snapshotDate());
    com.fortnite.pronos.model.PrSnapshot snapshot =
        prSnapshotRepository.findById(id).orElseGet(com.fortnite.pronos.model.PrSnapshot::new);
    snapshot.setPlayer(player);
    snapshot.setRegion(region);
    snapshot.setSnapshotDate(row.snapshotDate());
    snapshot.setPoints(row.points());
    snapshot.setRank(row.rank());
    snapshot.setCollectedAt(OffsetDateTime.now());
    snapshot.setRun(run);
    prSnapshotRepository.save(snapshot);
    counters.snapshotsWritten++;
  }

  private void upsertScore(
      com.fortnite.pronos.model.Player player,
      PrCsvParser.PrCsvRow row,
      int season,
      Counters counters) {
    Optional<com.fortnite.pronos.model.Score> existing =
        scoreRepository.findByPlayerAndSeason(player, season);
    com.fortnite.pronos.model.Score score =
        existing.orElseGet(com.fortnite.pronos.model.Score::new);
    score.setPlayer(player);
    score.setSeason(season);
    score.setPoints(row.points());
    score.setDate(row.snapshotDate());
    score.setTimestamp(OffsetDateTime.now());
    scoreRepository.save(score);
    counters.scoresWritten++;
  }

  private com.fortnite.pronos.model.IngestionRun.Status resolveStatus(
      Counters counters, int parseErrors) {
    if (parseErrors > 0 || counters.skippedRows > 0) {
      return com.fortnite.pronos.model.IngestionRun.Status.PARTIAL;
    }
    return com.fortnite.pronos.model.IngestionRun.Status.SUCCESS;
  }

  private String buildMessage(
      com.fortnite.pronos.model.IngestionRun.Status status, Counters counters, int parseErrors) {
    if (status == com.fortnite.pronos.model.IngestionRun.Status.SUCCESS) {
      return null;
    }
    if (status == com.fortnite.pronos.model.IngestionRun.Status.FAILED) {
      return "ingestion_failed";
    }
    return String.format("parse_errors=%d,skipped=%d", parseErrors, counters.skippedRows);
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
      Counters counters,
      int parseErrors) {
    return new PrIngestionResult(
        run.getId(),
        status,
        counters.playersCreated,
        counters.playersUpdated,
        counters.snapshotsWritten,
        counters.scoresWritten,
        counters.skippedRows,
        parseErrors);
  }

  private com.fortnite.pronos.model.PrRegion toRegion(String region) {
    try {
      return com.fortnite.pronos.model.PrRegion.valueOf(region);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private com.fortnite.pronos.model.Player.Region toPlayerRegion(
      com.fortnite.pronos.model.PrRegion prRegion) {
    try {
      return com.fortnite.pronos.model.Player.Region.valueOf(prRegion.name());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private String trancheFromRank(int rank) {
    if (rank <= 5) return "1-5";
    if (rank <= 10) return "6-10";
    if (rank <= 15) return "11-15";
    if (rank <= 20) return "16-20";
    if (rank <= 25) return "21-25";
    if (rank <= 30) return "26-30";
    return "31-infini";
  }

  private String buildUsername(String nickname) {
    String base = nickname == null ? "" : nickname.toLowerCase().replaceAll("[^a-z0-9]", "");
    if (!base.isEmpty()) {
      return base;
    }
    return "player" + Math.abs(Objects.hashCode(nickname));
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

  private static class Counters {
    private int playersCreated;
    private int playersUpdated;
    private int snapshotsWritten;
    private int scoresWritten;
    private int skippedRows;
  }
}
