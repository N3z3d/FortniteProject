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
    SanitizationStats stats = new SanitizationStats();

    for (com.fortnite.pronos.model.TeamPlayer teamPlayer : safeRoster) {
      UUID playerId = extractActivePlayerId(teamPlayer, stats);
      if (playerId == null) {
        continue;
      }
      if (uniquePlayers.putIfAbsent(playerId, teamPlayer) != null) {
        stats.incrementDuplicates();
      }
    }

    logIfSanitized(source, teamId, stats);
    return new ArrayList<>(uniquePlayers.values());
  }

  private static UUID extractActivePlayerId(
      com.fortnite.pronos.model.TeamPlayer teamPlayer, SanitizationStats stats) {
    if (teamPlayer == null || !teamPlayer.isActive()) {
      return null;
    }
    com.fortnite.pronos.model.Player player = teamPlayer.getPlayer();
    if (player == null || player.getId() == null) {
      stats.incrementMissingPlayers();
      return null;
    }
    return player.getId();
  }

  private static void logIfSanitized(String source, UUID teamId, SanitizationStats stats) {
    if (!stats.hasIssues()) {
      return;
    }
    log.warn(
        "TeamRosterSanitizer: source={}, teamId={}, duplicates={}, missingPlayers={}",
        source == null ? "unknown" : source,
        teamId,
        stats.getDuplicates(),
        stats.getMissingPlayers());
  }

  private static final class SanitizationStats {
    private int duplicates;
    private int missingPlayers;

    void incrementDuplicates() {
      duplicates++;
    }

    void incrementMissingPlayers() {
      missingPlayers++;
    }

    int getDuplicates() {
      return duplicates;
    }

    int getMissingPlayers() {
      return missingPlayers;
    }

    boolean hasIssues() {
      return duplicates > 0 || missingPlayers > 0;
    }
  }
}
