package com.fortnite.pronos.adapter.out.persistence.player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.game.model.PlayerRegion;

/**
 * Bidirectional mapper between pure domain models ({@code domain.player.model.Player}) and JPA
 * entities ({@code model.Player}). Lives in the adapter layer to keep both domain and entity layers
 * clean.
 */
@Component
public class PlayerEntityMapper {

  // ===============================
  // ENTITY -> DOMAIN
  // ===============================

  /** Maps a JPA Player entity to a pure domain Player using the reconstitution factory. */
  public com.fortnite.pronos.domain.player.model.Player toDomain(
      com.fortnite.pronos.model.Player entity) {
    if (entity == null) {
      return null;
    }

    return com.fortnite.pronos.domain.player.model.Player.restore(
        entity.getId(),
        entity.getFortniteId(),
        entity.getUsername(),
        entity.getNickname(),
        toDomainRegion(entity.getRegion()),
        entity.getTranche(),
        safeInt(entity.getCurrentSeason(), 2025),
        safeBool(entity.getLocked()));
  }

  // ===============================
  // DOMAIN -> ENTITY
  // ===============================

  /** Maps a domain Player to a JPA Player entity. */
  public com.fortnite.pronos.model.Player toEntity(
      com.fortnite.pronos.domain.player.model.Player domain) {
    if (domain == null) {
      return null;
    }

    com.fortnite.pronos.model.Player entity = new com.fortnite.pronos.model.Player();
    entity.setId(domain.getId());
    entity.setFortniteId(domain.getFortniteId());
    entity.setUsername(domain.getUsername());
    entity.setNickname(domain.getNickname());
    entity.setRegion(toEntityRegion(domain.getRegion()));
    entity.setTranche(domain.getTranche());
    entity.setCurrentSeason(domain.getCurrentSeason());
    entity.setLocked(domain.isLocked());
    return entity;
  }

  // ===============================
  // ENUM MAPPING
  // ===============================

  /** Maps domain PlayerRegion to JPA Player.Region. */
  public com.fortnite.pronos.model.Player.Region toEntityRegion(PlayerRegion domainRegion) {
    if (domainRegion == null) {
      return null;
    }
    return com.fortnite.pronos.model.Player.Region.valueOf(domainRegion.name());
  }

  /** Maps JPA Player.Region to domain PlayerRegion. */
  public PlayerRegion toDomainRegion(com.fortnite.pronos.model.Player.Region entityRegion) {
    if (entityRegion == null) {
      return null;
    }
    return PlayerRegion.valueOf(entityRegion.name());
  }

  // ===============================
  // COLLECTION MAPPING
  // ===============================

  /** Maps a list of JPA Player entities to domain Players. */
  public List<com.fortnite.pronos.domain.player.model.Player> toDomainList(
      List<com.fortnite.pronos.model.Player> entities) {
    if (entities == null) {
      return Collections.emptyList();
    }
    return entities.stream().map(this::toDomain).collect(Collectors.toList());
  }

  // ===============================
  // PRIVATE HELPERS
  // ===============================

  private static int safeInt(Integer value, int defaultValue) {
    return value != null ? value : defaultValue;
  }

  private static boolean safeBool(Boolean value) {
    return value != null && value;
  }
}
