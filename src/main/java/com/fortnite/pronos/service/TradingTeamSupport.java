package com.fortnite.pronos.service;

import java.util.List;
import java.util.UUID;

final class TradingTeamSupport {

  private TradingTeamSupport() {}

  static com.fortnite.pronos.model.Team fromTeamOf(com.fortnite.pronos.model.Trade trade) {
    return trade.getFromTeam();
  }

  static com.fortnite.pronos.model.Team toTeamOf(com.fortnite.pronos.model.Trade trade) {
    return trade.getToTeam();
  }

  static com.fortnite.pronos.model.Game teamGame(com.fortnite.pronos.model.Team team) {
    return team.getGame();
  }

  static UUID gameId(com.fortnite.pronos.model.Team team) {
    return team.getGameId();
  }

  static UUID teamOwnerId(com.fortnite.pronos.model.Team team) {
    return team.getUserId();
  }

  static List<com.fortnite.pronos.model.GameRegionRule> regionRules(
      com.fortnite.pronos.model.Team team) {
    return team.getGameRegionRules();
  }

  static List<com.fortnite.pronos.model.Player> getActivePlayers(
      com.fortnite.pronos.model.Team team) {
    return team.getPlayers().stream()
        .filter(
            teamPlayer ->
                teamPlayer != null
                    && teamPlayer.getPlayer() != null
                    && (teamPlayer.getUntil() == null || teamPlayer.isActive()))
        .map(com.fortnite.pronos.model.TeamPlayer::getPlayer)
        .toList();
  }

  static boolean teamOwnsPlayer(
      com.fortnite.pronos.model.Team team, com.fortnite.pronos.model.Player player) {
    return team.getPlayers().stream()
        .anyMatch(teamPlayer -> isOwnedActivePlayer(teamPlayer, player));
  }

  private static boolean isOwnedActivePlayer(
      com.fortnite.pronos.model.TeamPlayer teamPlayer, com.fortnite.pronos.model.Player player) {
    if (teamPlayer == null || teamPlayer.getPlayer() == null) {
      return false;
    }
    if (!teamPlayer.getPlayer().equals(player)) {
      return false;
    }
    return teamPlayer.getUntil() == null || teamPlayer.isActive();
  }
}
