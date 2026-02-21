package com.fortnite.pronos.service.leaderboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.PronostiqueurLeaderboardEntryDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service responsible for predictor (user) leaderboard operations. */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PronostiqueurLeaderboardService {

  private final com.fortnite.pronos.repository.TeamRepository teamRepository;
  private final com.fortnite.pronos.repository.ScoreRepository scoreRepository;

  /** Returns the pronosticator leaderboard for the requested season. */
  @Cacheable(value = "leaderboard", key = "'pronostiqueurs_' + #season")
  public List<PronostiqueurLeaderboardEntryDTO> getPronostiqueurLeaderboard(int season) {
    log.info("Loading pronosticator leaderboard for season={}", season);

    List<com.fortnite.pronos.model.Team> teams = teamRepository.findBySeasonWithFetch(season);
    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
    Map<UUID, List<com.fortnite.pronos.model.Team>> teamsByUser = groupTeamsByUser(teams);

    List<PronostiqueurLeaderboardEntryDTO> entries = buildEntries(teamsByUser, playerPointsMap);
    sortAndAssignRanks(entries);

    log.info("Pronosticator leaderboard generated with {} users", entries.size());
    return entries;
  }

  private Map<UUID, List<com.fortnite.pronos.model.Team>> groupTeamsByUser(
      List<com.fortnite.pronos.model.Team> teams) {
    return teams.stream().collect(Collectors.groupingBy(team -> team.getOwner().getId()));
  }

  private List<PronostiqueurLeaderboardEntryDTO> buildEntries(
      Map<UUID, List<com.fortnite.pronos.model.Team>> teamsByUser,
      Map<UUID, Integer> playerPointsMap) {
    List<PronostiqueurLeaderboardEntryDTO> entries = new ArrayList<>();
    for (Map.Entry<UUID, List<com.fortnite.pronos.model.Team>> entry : teamsByUser.entrySet()) {
      entries.add(buildUserEntry(entry.getKey(), entry.getValue(), playerPointsMap));
    }
    return entries;
  }

  private PronostiqueurLeaderboardEntryDTO buildUserEntry(
      UUID userId,
      List<com.fortnite.pronos.model.Team> userTeams,
      Map<UUID, Integer> playerPointsMap) {
    com.fortnite.pronos.model.User user = userTeams.get(0).getOwner();
    TeamScoreSummary scoreSummary = calculateTeamScoreSummary(userTeams, playerPointsMap);
    int avgPointsPerTeam = userTeams.isEmpty() ? 0 : scoreSummary.totalPoints() / userTeams.size();

    return PronostiqueurLeaderboardEntryDTO.builder()
        .userId(userId)
        .username(user.getUsername())
        .email(user.getEmail())
        .totalPoints(scoreSummary.totalPoints())
        .totalTeams(userTeams.size())
        .avgPointsPerTeam(avgPointsPerTeam)
        .bestTeamPoints(scoreSummary.bestTeamPoints())
        .bestTeamName(scoreSummary.bestTeamName())
        .victories(0)
        .winRate(0.0)
        .build();
  }

  private TeamScoreSummary calculateTeamScoreSummary(
      List<com.fortnite.pronos.model.Team> userTeams, Map<UUID, Integer> playerPointsMap) {
    int totalPoints = 0;
    int bestTeamPoints = 0;
    String bestTeamName = "";

    for (com.fortnite.pronos.model.Team team : userTeams) {
      int teamPoints = calculateTeamPoints(team, playerPointsMap);
      totalPoints += teamPoints;
      if (teamPoints > bestTeamPoints) {
        bestTeamPoints = teamPoints;
        bestTeamName = team.getName();
      }
    }

    return new TeamScoreSummary(totalPoints, bestTeamPoints, bestTeamName);
  }

  private int calculateTeamPoints(
      com.fortnite.pronos.model.Team team, Map<UUID, Integer> playerPointsMap) {
    int teamPoints = 0;
    for (com.fortnite.pronos.model.TeamPlayer teamPlayer : team.getPlayers()) {
      if (!teamPlayer.isActive()) {
        continue;
      }
      teamPoints += playerPointsMap.getOrDefault(teamPlayer.getPlayer().getId(), 0);
    }
    return teamPoints;
  }

  private void sortAndAssignRanks(List<PronostiqueurLeaderboardEntryDTO> entries) {
    entries.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));
    for (int i = 0; i < entries.size(); i++) {
      entries.get(i).setRank(i + 1);
    }
  }

  private record TeamScoreSummary(int totalPoints, int bestTeamPoints, String bestTeamName) {}
}
