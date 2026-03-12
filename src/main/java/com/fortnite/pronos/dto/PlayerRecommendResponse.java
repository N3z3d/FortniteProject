package com.fortnite.pronos.dto;

import java.util.UUID;

import com.fortnite.pronos.domain.player.model.Player;

/** Response containing the recommended player for the current draft slot. */
public record PlayerRecommendResponse(
    UUID id, String nickname, String region, String tranche, int trancheFloor) {

  public static PlayerRecommendResponse from(Player player) {
    int floor = parseFloor(player.getTranche());
    return new PlayerRecommendResponse(
        player.getId(),
        player.getNickname(),
        player.getRegion() != null ? player.getRegion().name() : null,
        player.getTranche(),
        floor);
  }

  private static int parseFloor(String tranche) {
    if (tranche == null || tranche.isBlank()) return 1;
    return Integer.parseInt(tranche.split("-")[0]);
  }
}
