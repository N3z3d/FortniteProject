package com.fortnite.pronos.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.player.model.RankSnapshot;
import com.fortnite.pronos.domain.port.out.RankSnapshotRepositoryPort;
import com.fortnite.pronos.dto.player.RankSnapshotResponse;

import lombok.RequiredArgsConstructor;

/**
 * Provides sparkline data (rank history) for players. Days parameter is capped at MAX_DAYS to avoid
 * unbounded queries.
 */
@Service
@RequiredArgsConstructor
public class RankSnapshotService {

  static final int MAX_DAYS = 90;
  static final int DEFAULT_DAYS = 14;

  private final RankSnapshotRepositoryPort repository;

  /**
   * Returns rank snapshots for a player/region over the last {@code days} days, sorted by date
   * ascending (oldest → newest for chart rendering).
   */
  public List<RankSnapshotResponse> getSparkline(UUID playerId, String region, int days) {
    int capped = Math.min(days, MAX_DAYS);
    return repository.findByPlayerAndRegion(playerId, region, capped).stream()
        .sorted(Comparator.comparing(RankSnapshot::getSnapshotDate))
        .map(s -> new RankSnapshotResponse(s.getSnapshotDate(), s.getRank()))
        .toList();
  }

  /** Records a new daily snapshot (append-only). Called by the ingestion pipeline or admin seed. */
  public RankSnapshot recordSnapshot(UUID playerId, String region, int rank, int prValue) {
    RankSnapshot snapshot = new RankSnapshot(playerId, region, rank, prValue, LocalDate.now());
    return repository.save(snapshot);
  }
}
