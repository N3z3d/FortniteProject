package com.fortnite.pronos.service.ingestion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.domain.port.out.PrSnapshotQueryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Daily quality job for the player pipeline. Computes main region (FR-07), detects duplicate Epic
 * IDs (FR-08), and alerts on stale UNRESOLVED entries (FR-09).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerQualityService {

  static final int STALE_UNRESOLVED_HOURS = 24;
  private static final int MONTHS_HISTORY = 12;

  private final PlayerDomainRepositoryPort playerRepository;
  private final PlayerIdentityRepositoryPort identityRepository;
  private final PrSnapshotQueryPort snapshotQuery;

  /**
   * Runs the full daily quality job. Triggered automatically every day at 03:00 UTC.
   *
   * @return summary of actions taken
   */
  @Scheduled(cron = "0 0 3 * * *")
  public PlayerQualityJobResult runDailyQualityJob() {
    int regionsUpdated = computeAndUpdateMainRegions();
    int duplicates = detectDuplicateEpicIds();
    int staleAlerted = alertStaleUnresolved();
    log.info(
        "Quality job complete: regionsUpdated={} duplicates={} staleAlerted={}",
        regionsUpdated,
        duplicates,
        staleAlerted);
    return new PlayerQualityJobResult(regionsUpdated, duplicates, staleAlerted);
  }

  private int computeAndUpdateMainRegions() {
    LocalDate since = LocalDate.now().minusMonths(MONTHS_HISTORY);
    List<Player> players = playerRepository.findAll();
    int updated = 0;
    for (Player player : players) {
      updated += tryUpdateMainRegion(player, since);
    }
    return updated;
  }

  private int tryUpdateMainRegion(Player player, LocalDate since) {
    Optional<String> mainRegionName =
        snapshotQuery.findMainRegionNameForPlayer(player.getId(), since);
    if (mainRegionName.isEmpty()) {
      return 0;
    }
    try {
      PlayerRegion computedRegion = PlayerRegion.valueOf(mainRegionName.get());
      if (player.getRegion() != computedRegion) {
        player.updateRegion(computedRegion);
        playerRepository.save(player);
        log.debug("Region updated: playerId={} region={}", player.getId(), computedRegion);
        return 1;
      }
    } catch (IllegalArgumentException e) {
      log.warn(
          "Unknown region {} for player {}: {}",
          mainRegionName.get(),
          player.getId(),
          e.getMessage());
    }
    return 0;
  }

  private int detectDuplicateEpicIds() {
    List<PlayerIdentityEntry> resolved = identityRepository.findByStatus(IdentityStatus.RESOLVED);
    Map<String, List<PlayerIdentityEntry>> byEpicId =
        resolved.stream()
            .filter(e -> e.getEpicId() != null)
            .collect(Collectors.groupingBy(PlayerIdentityEntry::getEpicId));
    int duplicates = 0;
    for (Map.Entry<String, List<PlayerIdentityEntry>> entry : byEpicId.entrySet()) {
      if (entry.getValue().size() > 1) {
        duplicates++;
        List<String> playerIds =
            entry.getValue().stream().map(e -> e.getPlayerId().toString()).toList();
        log.warn(
            "[ALERT] Duplicate epicId detected: epicId={} playerIds={}", entry.getKey(), playerIds);
      }
    }
    return duplicates;
  }

  private int alertStaleUnresolved() {
    LocalDateTime threshold = LocalDateTime.now().minusHours(STALE_UNRESOLVED_HOURS);
    List<PlayerIdentityEntry> stale =
        identityRepository.findByStatus(IdentityStatus.UNRESOLVED).stream()
            .filter(e -> e.getCreatedAt().isBefore(threshold))
            .toList();
    for (PlayerIdentityEntry entry : stale) {
      log.warn(
          "[ALERT] Player UNRESOLVED > {}h: pseudo={} playerId={} createdAt={}",
          STALE_UNRESOLVED_HOURS,
          entry.getPlayerUsername(),
          entry.getPlayerId(),
          entry.getCreatedAt());
    }
    return stale.size();
  }

  public record PlayerQualityJobResult(
      int mainRegionsUpdated, int duplicateEpicIdsDetected, int staleUnresolvedAlerted) {}
}
