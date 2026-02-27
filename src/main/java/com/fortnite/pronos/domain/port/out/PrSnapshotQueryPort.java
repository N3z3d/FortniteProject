package com.fortnite.pronos.domain.port.out;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PrSnapshotQueryPort {

  /**
   * Returns the name of the most frequent non-GLOBAL region in snapshots for the given player since
   * the given date. Returns empty if no qualifying snapshots exist.
   */
  Optional<String> findMainRegionNameForPlayer(UUID playerId, LocalDate since);
}
