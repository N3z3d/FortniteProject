package com.fortnite.pronos.adapter.out.persistence.player.alias;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerAliasJpaRepository extends JpaRepository<PlayerAliasEntity, UUID> {

  List<PlayerAliasEntity> findByPlayerId(UUID playerId);
}
