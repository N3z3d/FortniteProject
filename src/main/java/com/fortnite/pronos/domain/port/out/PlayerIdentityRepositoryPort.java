package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;

public interface PlayerIdentityRepositoryPort {

  List<PlayerIdentityEntry> findByStatus(IdentityStatus status);

  List<PlayerIdentityEntry> findByStatusPaged(IdentityStatus status, int page, int size);

  Optional<PlayerIdentityEntry> findByPlayerId(UUID playerId);

  long countByStatus(IdentityStatus status);

  PlayerIdentityEntry save(PlayerIdentityEntry entry);
}
