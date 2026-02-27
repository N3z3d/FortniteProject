package com.fortnite.pronos.dto.player;

import java.util.UUID;

import com.fortnite.pronos.domain.player.model.Player;

/** DTO representing a player entry in the catalogue view. Read-only, all authenticated roles. */
public record CataloguePlayerDto(
    UUID id, String nickname, String region, String tranche, boolean locked) {

  public static CataloguePlayerDto from(Player player) {
    return new CataloguePlayerDto(
        player.getId(),
        player.getNickname(),
        player.getRegionName(),
        player.getTranche(),
        player.isLocked());
  }
}
