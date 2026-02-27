package com.fortnite.pronos.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.Player;

final class ReferenceGameRegionRuleSupport {

  private static final int MAX_REGION_PLAYERS = 10;
  private static final int MIN_REGION_PLAYERS = 1;

  private ReferenceGameRegionRuleSupport() {}

  static void addRegionRules(Game game, Map<String, List<Player>> assignments) {
    Map<Player.Region, Long> counts = countPlayersByRegion(assignments);
    for (Player.Region region : Player.Region.values()) {
      long count = counts.getOrDefault(region, 0L);
      int maxPlayers = (int) Math.min(MAX_REGION_PLAYERS, Math.max(MIN_REGION_PLAYERS, count));
      GameRegionRule rule =
          GameRegionRule.builder().game(game).region(region).maxPlayers(maxPlayers).build();
      game.addRegionRule(rule);
    }
  }

  private static Map<Player.Region, Long> countPlayersByRegion(
      Map<String, List<Player>> assignments) {
    Map<Player.Region, Long> counts = new LinkedHashMap<>();
    for (List<Player> players : assignments.values()) {
      for (Player player : players) {
        if (player != null && player.getRegion() != null) {
          counts.merge(player.getRegion(), 1L, Long::sum);
        }
      }
    }
    return counts;
  }
}
