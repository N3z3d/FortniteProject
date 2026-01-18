package com.fortnite.pronos.service.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.PlayerLeaderboardEntryDTO;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerLeaderboardService - TDD Tests")
class PlayerLeaderboardServiceTddTest {

  @Mock private PlayerRepository playerRepository;
  @Mock private ScoreRepository scoreRepository;
  @Mock private TeamRepository teamRepository;

  @InjectMocks private PlayerLeaderboardService playerLeaderboardService;

  @Test
  @DisplayName("getPlayerLeaderboard ranks players by total points")
  void getPlayerLeaderboardRanksPlayersByTotalPoints() {
    int season = 2025;
    Player low = buildPlayer("low", Player.Region.EU);
    Player high = buildPlayer("high", Player.Region.NAC);

    Team lowTeam = buildTeam("LowTeam", buildUser("owner-low"), season, activePlayer(low, 1));
    Team highTeam =
        buildTeam("HighTeam", buildUser("owner-high"), season, activePlayer(high, 1));

    Map<UUID, Integer> points = new HashMap<>();
    points.put(low.getId(), 10);
    points.put(high.getId(), 200);

    when(playerRepository.findAll()).thenReturn(List.of(low, high));
    when(teamRepository.findBySeasonWithFetch(season)).thenReturn(List.of(lowTeam, highTeam));
    when(scoreRepository.findAllBySeasonGroupedByPlayer(season)).thenReturn(points);

    List<PlayerLeaderboardEntryDTO> entries =
        playerLeaderboardService.getPlayerLeaderboard(season);

    assertThat(entries).hasSize(2);
    assertThat(entries.get(0).getPlayerId()).isEqualTo(high.getId().toString());
    assertThat(entries.get(0).getRank()).isEqualTo(1);
    assertThat(entries.get(1).getPlayerId()).isEqualTo(low.getId().toString());
    assertThat(entries.get(1).getRank()).isEqualTo(2);
  }

  @Test
  @DisplayName("getPlayerLeaderboardByGame returns empty list when no teams")
  void getPlayerLeaderboardByGameReturnsEmptyListWhenNoTeams() {
    UUID gameId = UUID.randomUUID();
    when(teamRepository.findByGameIdWithFetch(gameId)).thenReturn(List.of());

    List<PlayerLeaderboardEntryDTO> entries =
        playerLeaderboardService.getPlayerLeaderboardByGame(gameId);

    assertThat(entries).isEmpty();
  }

  @Test
  @DisplayName("getPlayerLeaderboardByGame ignores inactive players")
  void getPlayerLeaderboardByGameIgnoresInactivePlayers() {
    UUID gameId = UUID.randomUUID();
    Player inactive = buildPlayer("inactive", Player.Region.EU);
    Team team =
        buildTeam("InactiveTeam", buildUser("owner"), 2025, inactivePlayer(inactive, 1));

    when(teamRepository.findByGameIdWithFetch(gameId)).thenReturn(List.of(team));
    when(playerRepository.findAllById(Set.of())).thenReturn(List.of());
    when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(Map.of());

    List<PlayerLeaderboardEntryDTO> entries =
        playerLeaderboardService.getPlayerLeaderboardByGame(gameId);

    assertThat(entries).isEmpty();
  }

  @Test
  @DisplayName("getPlayerLeaderboard defaults missing points to zero")
  void getPlayerLeaderboardDefaultsMissingPointsToZero() {
    int season = 2025;
    Player player = buildPlayer("zero", Player.Region.NAW);
    Team team = buildTeam("Team", buildUser("owner"), season, activePlayer(player, 1));

    when(playerRepository.findAll()).thenReturn(List.of(player));
    when(teamRepository.findBySeasonWithFetch(season)).thenReturn(List.of(team));
    when(scoreRepository.findAllBySeasonGroupedByPlayer(season)).thenReturn(Map.of());

    List<PlayerLeaderboardEntryDTO> entries =
        playerLeaderboardService.getPlayerLeaderboard(season);

    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).getTotalPoints()).isZero();
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
