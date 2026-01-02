package com.fortnite.pronos.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;

public final class TeamRosterSanitizer {
  private static final Logger log = LoggerFactory.getLogger(TeamRosterSanitizer.class);

  private TeamRosterSanitizer() {}

  public static List<TeamPlayer> sanitize(Team team, String source) {
    if (team == null) {
      return List.of();
    }
    return sanitize(team.getPlayers(), team.getId(), source);
  }

  public static List<TeamPlayer> sanitize(List<TeamPlayer> roster, UUID teamId, String source) {
    List<TeamPlayer> safeRoster = roster == null ? List.of() : roster;
    Map<UUID, TeamPlayer> uniquePlayers = new LinkedHashMap<>();
    int duplicates = 0;
    int missingPlayers = 0;

    for (TeamPlayer teamPlayer : safeRoster) {
      if (teamPlayer == null || !teamPlayer.isActive()) {
        continue;
      }
      Player player = teamPlayer.getPlayer();
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
