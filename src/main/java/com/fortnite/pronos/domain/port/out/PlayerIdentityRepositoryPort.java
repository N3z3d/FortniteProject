package com.fortnite.pronos.domain.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.player.identity.model.RegionalStatRow;

public interface PlayerIdentityRepositoryPort {

  List<PlayerIdentityEntry> findByStatus(IdentityStatus status);

  List<PlayerIdentityEntry> findByStatusPaged(IdentityStatus status, int page, int size);

  Optional<PlayerIdentityEntry> findByPlayerId(UUID playerId);

  long countByStatus(IdentityStatus status);

  PlayerIdentityEntry save(PlayerIdentityEntry entry);

  /** Returns (region, status, count) triplets for all regions with entries. */
  List<RegionalStatRow> countByRegionAndStatus();

  /** Returns the most recent createdAt per region: key=region, value=lastIngestedAt. */
  Map<String, LocalDateTime> findLastIngestedAtByRegion();

  /** Returns the oldest createdAt among entries with the given status, or empty if none. */
  Optional<LocalDateTime> findOldestCreatedAtByStatus(IdentityStatus status);
}
