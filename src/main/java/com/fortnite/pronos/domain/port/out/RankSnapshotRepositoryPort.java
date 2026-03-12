package com.fortnite.pronos.domain.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.player.model.RankSnapshot;

/**
 * Output port for RankSnapshot persistence. Table is append-only: only INSERT + SELECT, no UPDATE.
 */
public interface RankSnapshotRepositoryPort {

  /**
   * Returns snapshots for the given player/region over the last {@code days} days, ordered by
   * snapshotDate ASC.
   */
  List<RankSnapshot> findByPlayerAndRegion(UUID playerId, String region, int days);

  /**
   * Returns all snapshots for the given player across all regions over the last {@code days} days,
   * ordered by snapshotDate ASC. Used for building the PR-per-region profile view.
   */
  List<RankSnapshot> findByPlayerRecent(UUID playerId, int days);

  RankSnapshot save(RankSnapshot snapshot);

  /**
   * Returns the most recent snapshot for the given player with snapshotDate &lt;= date, or empty if
   * none exists. Used to handle weekend/ingestion gaps in competition period calculations.
   */
  Optional<RankSnapshot> findLatestOnOrBefore(UUID playerId, LocalDate date);
}
