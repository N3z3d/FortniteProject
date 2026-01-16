package com.fortnite.pronos.service.ingestion;

import java.io.Reader;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.IngestionRun;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.model.PrSnapshot;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.repository.IngestionRunRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.PrSnapshotRepository;
import com.fortnite.pronos.repository.ScoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PrIngestionService {
  private static final String DEFAULT_SOURCE = "LOCAL_PR_CSV";
  private static final int DEFAULT_SEASON = 2025;

  private final PrCsvParser parser;
  private final PlayerRepository playerRepository;
  private final PrSnapshotRepository prSnapshotRepository;
  private final ScoreRepository scoreRepository;
  private final IngestionRunRepository ingestionRunRepository;

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

    IngestionRun run = startRun(safeConfig.source());
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
    IngestionRun.Status status = resolveStatus(counters, parseResult.errorCount());
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

  private IngestionRun startRun(String source) {
    IngestionRun run = new IngestionRun();
    run.setSource(source);
    run.setStatus(IngestionRun.Status.RUNNING);
    return ingestionRunRepository.save(run);
  }

  private PrIngestionResult failRun(IngestionRun run, String reason, int parseErrors) {
    String message = "parse_failed:" + reason;
    finishRun(run, IngestionRun.Status.FAILED, 0, message);
    return buildResult(run, IngestionRun.Status.FAILED, new Counters(), parseErrors);
  }

  private Counters persistRows(
      List<PrCsvParser.PrCsvRow> rows, PrIngestionConfig config, IngestionRun run) {
    Counters counters = new Counters();
    for (PrCsvParser.PrCsvRow row : rows) {
      PrRegion prRegion = toRegion(row.region());
      if (prRegion == null) {
        counters.skippedRows++;
        continue;
      }
      Player player = resolvePlayer(row, prRegion, config, counters);
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

  private Player resolvePlayer(
      PrCsvParser.PrCsvRow row, PrRegion prRegion, PrIngestionConfig config, Counters counters) {
    if (prRegion == PrRegion.GLOBAL) {
      return resolveGlobalPlayer(row, config, counters);
    }

    Player.Region playerRegion = toPlayerRegion(prRegion);
    if (playerRegion == null) {
      counters.skippedRows++;
      return null;
    }
    return findOrCreatePlayer(row, playerRegion, config.season(), counters);
  }

  private Player resolveGlobalPlayer(
      PrCsvParser.PrCsvRow row, PrIngestionConfig config, Counters counters) {
    Optional<Player> existing = playerRepository.findByNickname(row.nickname());
    if (existing.isPresent()) {
      return existing.get();
    }
    String tranche = trancheFromRank(row.rank());
    return createPlayer(row, Player.Region.UNKNOWN, tranche, config.season(), counters);
  }

  private Player findOrCreatePlayer(
      PrCsvParser.PrCsvRow row, Player.Region region, int season, Counters counters) {
    Optional<Player> existing = playerRepository.findByNickname(row.nickname());
    String tranche = trancheFromRank(row.rank());

    if (existing.isPresent()) {
      Player player = existing.get();
      if (applyPlayerUpdates(player, region, tranche, season)) {
        playerRepository.save(player);
        counters.playersUpdated++;
      }
      return player;
    }

    return createPlayer(row, region, tranche, season, counters);
  }

  private Player createPlayer(
      PrCsvParser.PrCsvRow row,
      Player.Region region,
      String tranche,
      int season,
      Counters counters) {
    Player player = new Player();
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
      Player player, Player.Region region, String tranche, int season) {
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
      Player player,
      PrRegion region,
      PrCsvParser.PrCsvRow row,
      IngestionRun run,
      Counters counters) {
    PrSnapshot.PrSnapshotId id =
        new PrSnapshot.PrSnapshotId(player.getId(), region, row.snapshotDate());
    PrSnapshot snapshot = prSnapshotRepository.findById(id).orElseGet(PrSnapshot::new);
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

  private void upsertScore(Player player, PrCsvParser.PrCsvRow row, int season, Counters counters) {
    Optional<Score> existing = scoreRepository.findByPlayerAndSeason(player, season);
    Score score = existing.orElseGet(Score::new);
    score.setPlayer(player);
    score.setSeason(season);
    score.setPoints(row.points());
    score.setDate(row.snapshotDate());
    score.setTimestamp(OffsetDateTime.now());
    scoreRepository.save(score);
    counters.scoresWritten++;
  }

  private IngestionRun.Status resolveStatus(Counters counters, int parseErrors) {
    if (parseErrors > 0 || counters.skippedRows > 0) {
      return IngestionRun.Status.PARTIAL;
    }
    return IngestionRun.Status.SUCCESS;
  }

  private String buildMessage(IngestionRun.Status status, Counters counters, int parseErrors) {
    if (status == IngestionRun.Status.SUCCESS) {
      return null;
    }
    if (status == IngestionRun.Status.FAILED) {
      return "ingestion_failed";
    }
    return String.format("parse_errors=%d,skipped=%d", parseErrors, counters.skippedRows);
  }

  private void finishRun(
      IngestionRun run, IngestionRun.Status status, int totalRows, String errorMessage) {
    run.setStatus(status);
    run.setFinishedAt(OffsetDateTime.now());
    run.setTotalRowsWritten(totalRows);
    if (errorMessage != null && !errorMessage.isBlank()) {
      run.setErrorMessage(errorMessage);
    }
    ingestionRunRepository.save(run);
  }

  private PrIngestionResult buildResult(
      IngestionRun run, IngestionRun.Status status, Counters counters, int parseErrors) {
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

  private PrRegion toRegion(String region) {
    try {
      return PrRegion.valueOf(region);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private Player.Region toPlayerRegion(PrRegion prRegion) {
    try {
      return Player.Region.valueOf(prRegion.name());
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
      IngestionRun.Status status,
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
