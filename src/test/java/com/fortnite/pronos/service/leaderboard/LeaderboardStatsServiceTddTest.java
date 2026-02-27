package com.fortnite.pronos.service.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import com.fortnite.pronos.model.TeamPlayer;
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

  @Test
  @DisplayName("getLeaderboardStats default delegates to season 2025")
  void getLeaderboardStatsDefaultDelegatesToSeason2025() {
    when(teamRepository.findBySeason(2025)).thenReturn(List.of());
    when(playerRepository.findAll()).thenReturn(List.of());
    when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(Map.of());

    LeaderboardStatsDTO stats = leaderboardStatsService.getLeaderboardStats();

    assertThat(stats.getTotalTeams()).isZero();
    assertThat(stats.getTotalPlayers()).isZero();
    assertThat(stats.getTotalPoints()).isZero();
    assertThat(stats.getAveragePoints()).isZero();
  }

  @Test
  @DisplayName("defines a default season constant")
  void definesDefaultSeasonConstant() {
    assertThatCode(() -> LeaderboardStatsService.class.getDeclaredField("DEFAULT_SEASON"))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("getLeaderboardStats with zero teams returns zero average")
  void getLeaderboardStatsWithZeroTeamsReturnsZeroAverage() {
    int season = 2025;
    Player player = buildPlayer(Player.Region.EU, "solo");

    Map<UUID, Integer> points = new HashMap<>();
    points.put(player.getId(), 300);

    when(teamRepository.findBySeason(season)).thenReturn(List.of());
    when(playerRepository.findAll()).thenReturn(List.of(player));
    when(scoreRepository.findAllBySeasonGroupedByPlayer(season)).thenReturn(points);

    LeaderboardStatsDTO stats = leaderboardStatsService.getLeaderboardStats(season);

    assertThat(stats.getTotalTeams()).isZero();
    assertThat(stats.getTotalPlayers()).isEqualTo(1);
    assertThat(stats.getTotalPoints()).isEqualTo(300);
    assertThat(stats.getAveragePoints()).isZero();
  }

  @Test
  @DisplayName("getLeaderboardStats handles player with no score")
  void getLeaderboardStatsHandlesPlayerWithNoScore() {
    int season = 2025;
    Player player = buildPlayer(Player.Region.EU, "noscore");

    when(teamRepository.findBySeason(season)).thenReturn(List.of(buildTeam()));
    when(playerRepository.findAll()).thenReturn(List.of(player));
    when(scoreRepository.findAllBySeasonGroupedByPlayer(season)).thenReturn(Map.of());

    LeaderboardStatsDTO stats = leaderboardStatsService.getLeaderboardStats(season);

    assertThat(stats.getTotalPoints()).isZero();
    assertThat(stats.getRegionStats()).containsEntry("EU", 0L);
  }

  @Test
  @DisplayName("getLeaderboardStatsByGame aggregates stats from team players")
  void getLeaderboardStatsByGameAggregatesFromTeamPlayers() {
    UUID gameId = UUID.randomUUID();
    Player playerEU = buildPlayer(Player.Region.EU, "eu1");
    Player playerNA = buildPlayer(Player.Region.NAW, "na1");

    Team team = buildTeamWithPlayers(playerEU, playerNA);
    when(teamRepository.findByGameIdWithFetch(gameId)).thenReturn(List.of(team));
    when(playerRepository.findAllById(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(playerEU, playerNA));

    Map<UUID, Integer> points = new HashMap<>();
    points.put(playerEU.getId(), 50);
    points.put(playerNA.getId(), 80);
    when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(points);

    LeaderboardStatsDTO stats = leaderboardStatsService.getLeaderboardStatsByGame(gameId);

    assertThat(stats.getTotalTeams()).isEqualTo(1);
    assertThat(stats.getTotalPlayers()).isEqualTo(2);
    assertThat(stats.getTotalPoints()).isEqualTo(130);
    assertThat(stats.getAveragePoints()).isEqualTo(130.0);
    assertThat(stats.getRegionStats()).containsEntry("EU", 50L).containsEntry("NAW", 80L);
  }

  @Test
  @DisplayName("getRegionDistributionByGame counts players from teams")
  void getRegionDistributionByGameCountsPlayersFromTeams() {
    UUID gameId = UUID.randomUUID();
    Player p1 = buildPlayer(Player.Region.EU, "g1");
    Player p2 = buildPlayer(Player.Region.EU, "g2");
    Player p3 = buildPlayer(Player.Region.NAW, "g3");

    Team team = buildTeamWithPlayers(p1, p2, p3);
    when(teamRepository.findByGameIdWithFetch(gameId)).thenReturn(List.of(team));
    when(playerRepository.findAllById(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(p1, p2, p3));

    Map<String, Integer> distribution = leaderboardStatsService.getRegionDistributionByGame(gameId);

    assertThat(distribution).containsEntry("EU", 2).containsEntry("NAW", 1);
  }

  @Test
  @DisplayName("getRegionDistributionByGame with no teams returns empty map")
  void getRegionDistributionByGameWithNoTeamsReturnsEmptyMap() {
    UUID gameId = UUID.randomUUID();
    when(teamRepository.findByGameIdWithFetch(gameId)).thenReturn(List.of());
    when(playerRepository.findAllById(org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of());

    Map<String, Integer> distribution = leaderboardStatsService.getRegionDistributionByGame(gameId);

    assertThat(distribution).isEmpty();
  }

  private Team buildTeamWithPlayers(Player... players) {
    Team team = buildTeam();
    List<TeamPlayer> teamPlayers = new ArrayList<>();
    for (Player player : players) {
      TeamPlayer tp = new TeamPlayer();
      tp.setPlayer(player);
      tp.setPosition(1);
      teamPlayers.add(tp);
    }
    team.setPlayers(teamPlayers);
    return team;
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
