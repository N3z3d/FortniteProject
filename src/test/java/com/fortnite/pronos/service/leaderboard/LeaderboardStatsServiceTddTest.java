package com.fortnite.pronos.service.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.LeaderboardStatsDTO;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardStatsService - TDD Tests")
class LeaderboardStatsServiceTddTest {

  @Mock private TeamRepository teamRepository;
  @Mock private ScoreRepository scoreRepository;
  @Mock private PlayerRepository playerRepository;

  @InjectMocks private LeaderboardStatsService leaderboardStatsService;

  @Test
  @DisplayName("getLeaderboardStats aggregates totals and averages")
  void getLeaderboardStatsAggregatesTotalsAndAverages() {
    int season = 2025;
    Team teamOne = buildTeam();
    Team teamTwo = buildTeam();
    Player playerOne = buildPlayer(Player.Region.EU, "1");
    Player playerTwo = buildPlayer(Player.Region.NAC, "2");
    Player playerThree = buildPlayer(Player.Region.EU, "3");

    Map<UUID, Integer> points = new HashMap<>();
    points.put(playerOne.getId(), 100);
    points.put(playerTwo.getId(), 200);
    points.put(playerThree.getId(), 50);

    when(teamRepository.findBySeason(season)).thenReturn(List.of(teamOne, teamTwo));
    when(playerRepository.findAll()).thenReturn(List.of(playerOne, playerTwo, playerThree));
    when(scoreRepository.findAllBySeasonGroupedByPlayer(season)).thenReturn(points);

    LeaderboardStatsDTO stats = leaderboardStatsService.getLeaderboardStats(season);

    assertThat(stats.getTotalTeams()).isEqualTo(2);
    assertThat(stats.getTotalPlayers()).isEqualTo(3);
    assertThat(stats.getTotalPoints()).isEqualTo(350);
    assertThat(stats.getAveragePoints()).isEqualTo(175.0);
    assertThat(stats.getRegionStats()).containsEntry("EU", 150L).containsEntry("NAC", 200L);
  }

  @Test
  @DisplayName("getLeaderboardStatsByGame returns zero stats when no teams")
  void getLeaderboardStatsByGameReturnsZeroWhenNoTeams() {
    UUID gameId = UUID.randomUUID();
    when(teamRepository.findByGameIdWithFetch(gameId)).thenReturn(List.of());

    LeaderboardStatsDTO stats = leaderboardStatsService.getLeaderboardStatsByGame(gameId);

    assertThat(stats.getTotalTeams()).isZero();
    assertThat(stats.getTotalPlayers()).isZero();
    assertThat(stats.getTotalPoints()).isZero();
    assertThat(stats.getRegionStats()).isEmpty();
  }

  @Test
  @DisplayName("getRegionDistribution counts players per region")
  void getRegionDistributionCountsPlayersPerRegion() {
    Player playerOne = buildPlayer(Player.Region.EU, "1");
    Player playerTwo = buildPlayer(Player.Region.EU, "2");
    Player playerThree = buildPlayer(Player.Region.NAC, "3");

    when(playerRepository.findAll()).thenReturn(List.of(playerOne, playerTwo, playerThree));

    Map<String, Integer> distribution = leaderboardStatsService.getRegionDistribution();

    assertThat(distribution).containsEntry("EU", 2).containsEntry("NAC", 1);
  }

  @Test
  @DisplayName("getTrancheDistribution counts players per tranche")
  void getTrancheDistributionCountsPlayersPerTranche() {
    Player playerOne = buildPlayer(Player.Region.EU, "1");
    playerOne.setTranche("1");
    Player playerTwo = buildPlayer(Player.Region.NAC, "2");
    playerTwo.setTranche("2");
    Player playerThree = buildPlayer(Player.Region.NAC, "3");
    playerThree.setTranche("2");

    when(playerRepository.findAll()).thenReturn(List.of(playerOne, playerTwo, playerThree));

    Map<String, Integer> distribution = leaderboardStatsService.getTrancheDistribution();

    assertThat(distribution).containsEntry("Tranche 1", 1).containsEntry("Tranche 2", 2);
  }

  private Team buildTeam() {
    Team team = new Team();
    team.setId(UUID.randomUUID());
    team.setName("Team");
    return team;
  }

  private Player buildPlayer(Player.Region region, String suffix) {
    Player player = new Player();
    player.setId(UUID.randomUUID());
    player.setUsername("player-" + suffix);
    player.setNickname("nick-" + suffix);
    player.setRegion(region);
    player.setTranche("1");
    return player;
  }
}
