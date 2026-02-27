package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.UUID;

import com.fortnite.pronos.domain.player.alias.model.PlayerAliasEntry;

public interface PlayerAliasRepositoryPort {

  PlayerAliasEntry save(PlayerAliasEntry entry);

  List<PlayerAliasEntry> findByPlayerId(UUID playerId);
}
