package com.fortnite.pronos.service.catalogue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.player.model.RankSnapshot;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.RankSnapshotRepositoryPort;
import com.fortnite.pronos.dto.player.PlayerDetailDto;

import lombok.RequiredArgsConstructor;

/**
 * Provides the rich player profile for the catalogue detail view (FR-11). Aggregates player info
 * and latest PR per region from recent snapshots.
 */
@Service
@RequiredArgsConstructor
public class PlayerDetailService {

  static final int MAX_SNAPSHOT_DAYS = 90;

  private final PlayerDomainRepositoryPort playerRepository;
  private final RankSnapshotRepositoryPort snapshotRepository;

  /**
   * Returns the full player profile, or empty if the player does not exist.
   *
   * @param playerId the player UUID
   * @return the player detail, or {@link Optional#empty()} if not found
   */
  public Optional<PlayerDetailDto> getPlayerDetail(UUID playerId) {
    return playerRepository
        .findById(playerId)
        .map(
            player -> {
              List<RankSnapshot> snapshots =
                  snapshotRepository.findByPlayerRecent(playerId, MAX_SNAPSHOT_DAYS);
              return PlayerDetailDto.from(player, snapshots);
            });
  }
}
