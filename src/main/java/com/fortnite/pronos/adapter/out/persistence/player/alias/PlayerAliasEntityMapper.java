package com.fortnite.pronos.adapter.out.persistence.player.alias;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.player.alias.model.PlayerAliasEntry;

@Component
public class PlayerAliasEntityMapper {

  public PlayerAliasEntry toDomain(PlayerAliasEntity entity) {
    return PlayerAliasEntry.restore(
        entity.getId(),
        entity.getPlayerId(),
        entity.getNickname(),
        entity.getSource(),
        entity.isCurrent(),
        entity.getCreatedAt());
  }

  public PlayerAliasEntity toEntity(PlayerAliasEntry domain) {
    return PlayerAliasEntity.builder()
        .id(domain.getId())
        .playerId(domain.getPlayerId())
        .nickname(domain.getNickname())
        .source(domain.getSource())
        .current(domain.isCurrent())
        .createdAt(domain.getCreatedAt())
        .build();
  }
}
