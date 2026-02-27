package com.fortnite.pronos.adapter.out.persistence.player.alias;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.player.alias.model.PlayerAliasEntry;
import com.fortnite.pronos.domain.port.out.PlayerAliasRepositoryPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlayerAliasRepositoryAdapter implements PlayerAliasRepositoryPort {

  private final PlayerAliasJpaRepository jpaRepository;
  private final PlayerAliasEntityMapper mapper;

  @Override
  public PlayerAliasEntry save(PlayerAliasEntry entry) {
    PlayerAliasEntity saved = jpaRepository.save(mapper.toEntity(entry));
    return mapper.toDomain(saved);
  }

  @Override
  public List<PlayerAliasEntry> findByPlayerId(UUID playerId) {
    return jpaRepository.findByPlayerId(playerId).stream().map(mapper::toDomain).toList();
  }
}
