package com.fortnite.pronos.service.ingestion;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.player.alias.model.PlayerAliasEntry;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.port.out.PlayerAliasRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.domain.port.out.ScoreRepositoryPort;
import com.fortnite.pronos.model.IngestionRun;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.model.PrSnapshot;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.repository.PrSnapshotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
class PrIngestionRowProcessor {

  private static final int TRANCHE_SIZE = 5;
  private static final int MAX_FINITE_TRANCHE_RANK = 30;
  private static final String LAST_TRANCHE_LABEL = "31-infini";
  private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]");

  private final PlayerRepositoryPort playerRepository;
  private final PrSnapshotRepository prSnapshotRepository;
  private final ScoreRepositoryPort scoreRepository;
  private final PlayerIdentityRepositoryPort identityRepository;
  private final PlayerAliasRepositoryPort aliasRepository;

  PrIngestionCounters persistRows(
      List<PrCsvParser.PrCsvRow> rows,
      PrIngestionService.PrIngestionConfig config,
      IngestionRun run) {
    MutableCounters counters = new MutableCounters();
    for (PrCsvParser.PrCsvRow row : rows) {
      PrRegion prRegion = toRegion(row.region());
      if (prRegion != null) {
        Player player = resolvePlayer(row, prRegion, config, counters);
        if (player != null) {
          upsertSnapshot(player, prRegion, row, run, counters);
          if (config.writeScores()) {
            upsertScore(player, row, config.season(), counters);
          }
        }
      } else {
        counters.skippedRows++;
      }
    }
    return counters.toImmutable();
  }

  private Player resolvePlayer(
      PrCsvParser.PrCsvRow row,
      PrRegion prRegion,
      PrIngestionService.PrIngestionConfig config,
      MutableCounters counters) {
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
      PrCsvParser.PrCsvRow row,
      PrIngestionService.PrIngestionConfig config,
      MutableCounters counters) {
    Optional<Player> existing = playerRepository.findByNickname(row.nickname());
    if (existing.isPresent()) {
      return existing.get();
    }
    String tranche = trancheFromRank(row.rank());
    return createPlayer(row, Player.Region.UNKNOWN, tranche, config.season(), counters);
  }

  private Player findOrCreatePlayer(
      PrCsvParser.PrCsvRow row, Player.Region region, int season, MutableCounters counters) {
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
      MutableCounters counters) {
    Player player = new Player();
    player.setNickname(row.nickname());
    player.setUsername(buildUsername(row.nickname()));
    player.setRegion(region);
    player.setTranche(tranche);
    player.setCurrentSeason(season);
    playerRepository.save(player);
    counters.playersCreated++;
    queueForResolution(player);
    recordAlias(player);
    return player;
  }

  private void recordAlias(Player player) {
    try {
      PlayerAliasEntry alias =
          new PlayerAliasEntry(
              player.getId(), player.getNickname(), "FT_INGESTION", LocalDateTime.now());
      aliasRepository.save(alias);
      log.debug("Recorded alias: playerId={} nickname={}", player.getId(), player.getNickname());
    } catch (Exception e) {
      log.warn(
          "Failed to record alias: nickname={} error={}", player.getNickname(), e.getMessage());
    }
  }

  private void queueForResolution(Player player) {
    try {
      PlayerIdentityEntry entry =
          new PlayerIdentityEntry(
              player.getId(), player.getNickname(), player.getRegion().name(), LocalDateTime.now());
      identityRepository.save(entry);
      log.debug(
          "Queued for resolution: playerId={} pseudo={}", player.getId(), player.getNickname());
    } catch (Exception e) {
      log.warn(
          "Failed to queue player for resolution: pseudo={} error={}",
          player.getNickname(),
          e.getMessage());
    }
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
      MutableCounters counters) {
    java.util.Optional<PrSnapshot> existingSnapshot =
        prSnapshotRepository
            .findForUpsert(player.getId(), region.name(), row.snapshotDate())
            .map(snapshot -> snapshot);
    PrSnapshot snapshot = existingSnapshot.orElseGet(PrSnapshot::new);
    snapshot.setPlayer(player);
    snapshot.setRegion(region);
    snapshot.setSnapshotDate(row.snapshotDate());
    snapshot.setPoints(row.points());
    snapshot.setPrValue(row.points());
    snapshot.setRank(row.rank());
    snapshot.setCollectedAt(OffsetDateTime.now());
    snapshot.setRun(run);
    if (existingSnapshot.isEmpty()) {
      prSnapshotRepository.persist(snapshot);
    }
    counters.snapshotsWritten++;
  }

  private void upsertScore(
      Player player, PrCsvParser.PrCsvRow row, int season, MutableCounters counters) {
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
    if (rank > MAX_FINITE_TRANCHE_RANK) {
      return LAST_TRANCHE_LABEL;
    }
    int safeRank = Math.max(rank, 1);
    int trancheStart = ((safeRank - 1) / TRANCHE_SIZE) * TRANCHE_SIZE + 1;
    int trancheEnd = trancheStart + TRANCHE_SIZE - 1;
    return trancheStart + "-" + trancheEnd;
  }

  private String buildUsername(String nickname) {
    String base = nickname == null ? "" : sanitizeNickname(nickname);
    if (!base.isEmpty()) {
      return base;
    }
    return "player" + Math.abs(Objects.hashCode(nickname));
  }

  private String sanitizeNickname(String nickname) {
    String normalizedNickname = nickname.toLowerCase(Locale.ROOT);
    return NON_ALPHANUMERIC_PATTERN.matcher(normalizedNickname).replaceAll("");
  }

  private static final class MutableCounters {
    private int playersCreated;
    private int playersUpdated;
    private int snapshotsWritten;
    private int scoresWritten;
    private int skippedRows;

    private PrIngestionCounters toImmutable() {
      return new PrIngestionCounters(
          playersCreated, playersUpdated, snapshotsWritten, scoresWritten, skippedRows);
    }
  }
}
