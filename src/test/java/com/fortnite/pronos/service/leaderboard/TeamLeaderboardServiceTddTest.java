package com.fortnite.pronos.service.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.LeaderboardEntryDTO;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamLeaderboardService - TDD Tests")
class TeamLeaderboardServiceTddTest {

  @Mock private TeamRepository teamRepository;
  @Mock private ScoreRepository scoreRepository;
  @Mock private PlayerRepository playerRepository;

  @InjectMocks private TeamLeaderboardService teamLeaderboardService;

  @Test
  @DisplayName("getLeaderboard ranks teams by total points")
  void getLeaderboardRanksTeamsByTotalPoints() {
    int season = 2025;
    Player lowPlayer = buildPlayer("low", Player.Region.EU);
    Player highPlayer = buildPlayer("high", Player.Region.NAC);

    Team lowTeam = buildTeam("LowTeam", buildUser("owner-low"), season, activePlayer(lowPlayer, 1));
    Team highTeam =
        buildTeam("HighTeam", buildUser("owner-high"), season, activePlayer(highPlayer, 1));

    Map<UUID, Integer> points = new HashMap<>();
    points.put(lowPlayer.getId(), 100);
    points.put(highPlayer.getId(), 200);

    when(teamRepository.findBySeasonWithFetch(season)).thenReturn(List.of(lowTeam, highTeam));
    when(scoreRepository.findAllBySeasonGroupedByPlayer(season)).thenReturn(points);

    List<LeaderboardEntryDTO> entries = teamLeaderboardService.getLeaderboard(season);

    assertThat(entries).hasSize(2);
    assertThat(entries.get(0).getTeamId()).isEqualTo(highTeam.getId());
    assertThat(entries.get(0).getRank()).isEqualTo(1);
    assertThat(entries.get(1).getTeamId()).isEqualTo(lowTeam.getId());
    assertThat(entries.get(1).getRank()).isEqualTo(2);
  }

  @Test
  @DisplayName("getLeaderboard ignores inactive players")
  void getLeaderboardIgnoresInactivePlayers() {
    int season = 2025;
    Player active = buildPlayer("active", Player.Region.EU);
    Player inactive = buildPlayer("inactive", Player.Region.NAW);
    Team team =
        buildTeam(
            "Team",
            buildUser("owner"),
            season,
            activePlayer(active, 1),
            inactivePlayer(inactive, 2));

    Map<UUID, Integer> points = new HashMap<>();
    points.put(active.getId(), 100);
    points.put(inactive.getId(), 999);

    when(teamRepository.findBySeasonWithFetch(season)).thenReturn(List.of(team));
    when(scoreRepository.findAllBySeasonGroupedByPlayer(season)).thenReturn(points);

    List<LeaderboardEntryDTO> entries = teamLeaderboardService.getLeaderboard(season);

    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).getTotalPoints()).isEqualTo(100);
    assertThat(entries.get(0).getPlayers()).hasSize(1);
  }

  @Test
  @DisplayName("getLeaderboardByGame returns empty list when no teams")
  void getLeaderboardByGameReturnsEmptyListWhenNoTeams() {
    UUID gameId = UUID.randomUUID();
    when(teamRepository.findByGameIdWithFetch(gameId)).thenReturn(List.of());

    List<LeaderboardEntryDTO> entries = teamLeaderboardService.getLeaderboardByGame(gameId);

    assertThat(entries).isEmpty();
  }

  @Test
  @DisplayName("getTeamRanking throws when team is missing")
  void getTeamRankingThrowsWhenTeamIsMissing() {
    UUID teamId = UUID.randomUUID();
    when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> teamLeaderboardService.getTeamRanking(teamId.toString()))
        .isInstanceOf(RuntimeException.class);
  }

  private User buildUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setEmail(username + "@example.com");
    user.setPassword("password");
    return user;
  }

  private Player buildPlayer(String name, Player.Region region) {
    Player player = new Player();
    player.setId(UUID.randomUUID());
    player.setUsername(name);
    player.setNickname(name);
    player.setRegion(region);
    player.setTranche("1");
    return player;
  }

  private Team buildTeam(String name, User owner, int season, TeamPlayer... players) {
    Team team = new Team();
    team.setId(UUID.randomUUID());
    team.setName(name);
    team.setOwner(owner);
    team.setSeason(season);
    team.setPlayers(Arrays.asList(players));
    return team;
  }

  private TeamPlayer activePlayer(Player player, int position) {
    return buildTeamPlayer(player, position, null);
  }

  private TeamPlayer inactivePlayer(Player player, int position) {
    return buildTeamPlayer(player, position, OffsetDateTime.now());
  }

  private TeamPlayer buildTeamPlayer(Player player, int position, OffsetDateTime until) {
    TeamPlayer teamPlayer = new TeamPlayer();
    teamPlayer.setPlayer(player);
    teamPlayer.setPosition(position);
    teamPlayer.setUntil(until);
    return teamPlayer;
  }
}
