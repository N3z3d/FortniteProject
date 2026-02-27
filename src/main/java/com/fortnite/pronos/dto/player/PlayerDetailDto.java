package com.fortnite.pronos.dto.player;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.player.model.RankSnapshot;

/**
 * Rich player profile for the catalogue detail view (FR-11). Includes latest PR value per region
 * and the timestamp of the most recent snapshot.
 */
public record PlayerDetailDto(
    UUID id,
    String nickname,
    String region,
    String tranche,
    boolean locked,
    Map<String, Integer> prByRegion,
    LocalDate lastSnapshotDate) {

  /**
   * Builds a PlayerDetailDto from a domain Player and its recent snapshots.
   *
   * <p>For each region, the most recent snapshot's prValue is kept. {@code lastSnapshotDate} is the
   * maximum snapshotDate across all regions.
   */
  public static PlayerDetailDto from(Player player, List<RankSnapshot> snapshots) {
    Map<String, Integer> prByRegion =
        snapshots.stream()
            .collect(
                Collectors.toMap(
                    RankSnapshot::getRegion,
                    RankSnapshot::getPrValue,
                    (older, newer) -> newer)); // list is ASC by date — newer wins

    LocalDate lastSnapshotDate =
        snapshots.stream()
            .map(RankSnapshot::getSnapshotDate)
            .max(Comparator.naturalOrder())
            .orElse(null);

    return new PlayerDetailDto(
        player.getId(),
        player.getNickname(),
        player.getRegionName(),
        player.getTranche(),
        player.isLocked(),
        prByRegion,
        lastSnapshotDate);
  }
}
