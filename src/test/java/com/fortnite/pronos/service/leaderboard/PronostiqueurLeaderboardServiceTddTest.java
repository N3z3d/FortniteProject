package com.fortnite.pronos.service.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.PronostiqueurLeaderboardEntryDTO;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PronostiqueurLeaderboardService - TDD Tests")
class PronostiqueurLeaderboardServiceTddTest {

  @Mock private TeamRepository teamRepository;
  @Mock private ScoreRepository scoreRepository;

  @InjectMocks private PronostiqueurLeaderboardService pronostiqueurLeaderboardService;

  @Test
  @DisplayName("getPronostiqueurLeaderboard aggregates teams per user")
  void getPronostiqueurLeaderboardAggregatesTeamsPerUser() {
    int season = 2025;
    User owner = buildUser("owner");
    Player playerA = buildPlayer("a", Player.Region.EU);
    Player playerB = buildPlayer("b", Player.Region.NAC);

    Team teamOne = buildTeam("TeamOne", owner, season, activePlayer(playerA, 1));
    Team teamTwo = buildTeam("TeamTwo", owner, season, activePlayer(playerB, 1));

    when(teamRepository.findBySeasonWithFetch(season)).thenReturn(List.of(teamOne, teamTwo));
    when(scoreRepository.findAllBySeasonGroupedByPlayer(season))
        .thenReturn(Map.of(playerA.getId(), 100, playerB.getId(), 200));

    List<PronostiqueurLeaderboardEntryDTO> entries =
        pronostiqueurLeaderboardService.getPronostiqueurLeaderboard(season);

    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).getTotalPoints()).isEqualTo(300);
    assertThat(entries.get(0).getTotalTeams()).isEqualTo(2);
    assertThat(entries.get(0).getBestTeamPoints()).isEqualTo(200);
  }

  @Test
  @DisplayName("getPronostiqueurLeaderboard returns empty list when no teams")
  void getPronostiqueurLeaderboardReturnsEmptyWhenNoTeams() {
    int season = 2025;
    when(teamRepository.findBySeasonWithFetch(season)).thenReturn(List.of());

    List<PronostiqueurLeaderboardEntryDTO> entries =
        pronostiqueurLeaderboardService.getPronostiqueurLeaderboard(season);

    assertThat(entries).isEmpty();
  }

  @Test
  @DisplayName("getPronostiqueurLeaderboard handles missing points")
  void getPronostiqueurLeaderboardHandlesMissingPoints() {
    int season = 2025;
    User owner = buildUser("owner");
    Player player = buildPlayer("player", Player.Region.EU);
    Team team = buildTeam("Team", owner, season, activePlayer(player, 1));

    when(teamRepository.findBySeasonWithFetch(season)).thenReturn(List.of(team));
    when(scoreRepository.findAllBySeasonGroupedByPlayer(season)).thenReturn(Map.of());

    List<PronostiqueurLeaderboardEntryDTO> entries =
        pronostiqueurLeaderboardService.getPronostiqueurLeaderboard(season);

    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).getTotalPoints()).isZero();
    assertThat(entries.get(0).getBestTeamPoints()).isZero();
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
    TeamPlayer teamPlayer = new TeamPlayer();
    teamPlayer.setPlayer(player);
    teamPlayer.setPosition(position);
    return teamPlayer;
  }
}
