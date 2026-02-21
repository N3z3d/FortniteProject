package com.fortnite.pronos.service.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"java:S135"})
public final class TeamRosterSanitizer {
  private static final Logger log = LoggerFactory.getLogger(TeamRosterSanitizer.class);

  private TeamRosterSanitizer() {}

  public static List<com.fortnite.pronos.model.TeamPlayer> sanitize(
      com.fortnite.pronos.model.Team team, String source) {
    if (team == null) {
      return List.of();
    }
    return sanitize(team.getPlayers(), team.getId(), source);
  }

  public static List<com.fortnite.pronos.model.TeamPlayer> sanitize(
      List<com.fortnite.pronos.model.TeamPlayer> roster, UUID teamId, String source) {
    List<com.fortnite.pronos.model.TeamPlayer> safeRoster = roster == null ? List.of() : roster;
    Map<UUID, com.fortnite.pronos.model.TeamPlayer> uniquePlayers = new LinkedHashMap<>();
    int duplicates = 0;
    int missingPlayers = 0;

    for (com.fortnite.pronos.model.TeamPlayer teamPlayer : safeRoster) {
      if (teamPlayer == null || !teamPlayer.isActive()) {
        continue;
      }
      com.fortnite.pronos.model.Player player = teamPlayer.getPlayer();
      if (player == null || player.getId() == null) {
        missingPlayers++;
        continue;
      }
      UUID playerId = player.getId();
      if (uniquePlayers.containsKey(playerId)) {
        duplicates++;
        continue;
      }
      uniquePlayers.put(playerId, teamPlayer);
    }

    if (duplicates > 0 || missingPlayers > 0) {
      log.warn(
          "TeamRosterSanitizer: source={}, teamId={}, duplicates={}, missingPlayers={}",
          source == null ? "unknown" : source,
          teamId,
          duplicates,
          missingPlayers);
    }

    return new ArrayList<>(uniquePlayers.values());
  }
}
