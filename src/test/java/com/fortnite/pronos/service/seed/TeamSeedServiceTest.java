package com.fortnite.pronos.service.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.service.MockDataGeneratorService;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamSeedService")
class TeamSeedServiceTest {

  @Mock private TeamRepository teamRepository;
  @Mock private PlayerRepository playerRepository;

  @InjectMocks private TeamSeedService teamSeedService;

  @Test
  void createFallbackTeamsReturnsEmptyWhenNotEnoughPlayers() {
    when(playerRepository.findAll()).thenReturn(List.of(buildPlayer("p1"), buildPlayer("p2")));

    List<Team> result = teamSeedService.createFallbackTeams(buildParticipants("u1", "u2", "u3"));

    assertThat(result).isEmpty();
    verify(teamRepository, never()).saveAll(any());
  }

  @Test
  void createFallbackTeamsReturnsEmptyWhenNotEnoughParticipantUsers() {
    when(playerRepository.findAll())
        .thenReturn(List.of(buildPlayer("p1"), buildPlayer("p2"), buildPlayer("p3")));

    List<User> users =
        List.of(
            buildUser("admin", User.UserRole.ADMIN),
            buildUser("u1", User.UserRole.USER),
            buildUser("u2", User.UserRole.USER));

    List<Team> result = teamSeedService.createFallbackTeams(users);

    assertThat(result).isEmpty();
    verify(teamRepository, never()).saveAll(any());
  }

  @Test
  void createFallbackTeamsCreatesAtMostThreeTeams() {
    when(teamRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(playerRepository.findAll())
        .thenReturn(
            List.of(
                buildPlayer("p1"),
                buildPlayer("p2"),
                buildPlayer("p3"),
                buildPlayer("p4"),
                buildPlayer("p5"),
                buildPlayer("p6"),
                buildPlayer("p7"),
                buildPlayer("p8"),
                buildPlayer("p9"),
                buildPlayer("p10"),
                buildPlayer("p11"),
                buildPlayer("p12")));

    List<Team> result =
        teamSeedService.createFallbackTeams(buildParticipants("u1", "u2", "u3", "u4"));

    assertThat(result)
        .hasSize(3)
        .allMatch(team -> team.getSeason() == 2025)
        .allMatch(team -> !team.getPlayers().isEmpty());
  }

  @Test
  void createTeamsFromCsvAssignmentsUsesFallbackWhenMockDataEmpty() {
    when(teamRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(playerRepository.findAll())
        .thenReturn(
            List.of(
                buildPlayer("p1"),
                buildPlayer("p2"),
                buildPlayer("p3"),
                buildPlayer("p4"),
                buildPlayer("p5"),
                buildPlayer("p6")));

    List<Team> result =
        teamSeedService.createTeamsFromCsvAssignments(
            buildParticipants("u1", "u2", "u3"), MockDataGeneratorService.MockDataSet.empty());

    assertThat(result).hasSize(3);
  }

  @Test
  void createTeamsFromCsvAssignmentsCreatesTeamFromCsvData() {
    when(teamRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    Player persistedPlayer = buildPlayerWithId("alice-player");
    when(playerRepository.findAll()).thenReturn(List.of(persistedPlayer));
    when(((com.fortnite.pronos.domain.port.out.PlayerRepositoryPort) playerRepository)
            .findById(persistedPlayer.getId()))
        .thenReturn(java.util.Optional.of(persistedPlayer));

    MockDataGeneratorService.PlayerWithScore playerWithScore =
        new MockDataGeneratorService.PlayerWithScore(
            "alice", buildPlayerWithId("alice-player"), null, 1);
    Map<String, List<MockDataGeneratorService.PlayerWithScore>> assignments = new HashMap<>();
    assignments.put("alice", List.of(playerWithScore));

    MockDataGeneratorService.MockDataSet mockDataSet =
        new MockDataGeneratorService.MockDataSet(assignments, 1);

    List<Team> result =
        teamSeedService.createTeamsFromCsvAssignments(
            List.of(buildUser("alice", User.UserRole.USER)), mockDataSet);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getName()).isEqualTo("Equipe alice");
    assertThat(result.getFirst().getPlayers()).hasSize(1);
  }

  private List<User> buildParticipants(String... usernames) {
    return java.util.Arrays.stream(usernames)
        .map(username -> buildUser(username, User.UserRole.USER))
        .toList();
  }

  private User buildUser(String username, User.UserRole role) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setRole(role);
    return user;
  }

  private Player buildPlayer(String username) {
    Player player = new Player();
    player.setUsername(username);
    player.setNickname(username);
    return player;
  }

  private Player buildPlayerWithId(String username) {
    Player player = buildPlayer(username);
    player.setId(UUID.randomUUID());
    return player;
  }
}
