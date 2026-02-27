package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class TeamInitializationServiceTest {

  @Mock private CsvDataLoaderService csvDataLoaderService;
  @Mock private Environment environment;
  @Mock private UserRepositoryPort userRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private PlayerRepository playerRepository;

  @InjectMocks private TeamInitializationService teamInitializationService;

  @Test
  void createTeamsFromCsvDataCreatesTeamAndSanitizesUsernameWhenUserMissing() {
    Player firstPlayer = buildPlayer("first-player");
    Player secondPlayer = buildPlayer("second-player");
    String pronostiqueur = "J\u00f6hn.Doe!";
    Map<String, List<Player>> assignments =
        Map.of(pronostiqueur, List.of(firstPlayer, secondPlayer));

    when(csvDataLoaderService.getAllPlayerAssignments()).thenReturn(assignments);
    when(teamRepository.findBySeason(2025)).thenReturn(List.of());
    when(userRepository.findByUsernameIgnoreCase(pronostiqueur)).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(playerRepository.getReferenceById(firstPlayer.getId())).thenReturn(firstPlayer);
    when(playerRepository.getReferenceById(secondPlayer.getId())).thenReturn(secondPlayer);

    teamInitializationService.createTeamsFromCsvData();

    ArgumentCaptor<User> createdUser = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(createdUser.capture());
    assertThat(createdUser.getValue().getUsername()).isEqualTo("jhndoe");

    ArgumentCaptor<Team> savedTeam = ArgumentCaptor.forClass(Team.class);
    verify(teamRepository).save(savedTeam.capture());
    assertThat(savedTeam.getValue().getPlayers()).hasSize(2);
    assertThat(savedTeam.getValue().getPlayers())
        .extracting(TeamPlayer::getPosition)
        .containsExactly(1, 2);
  }

  @Test
  void createTeamsFromCsvDataSkipsCreationWhenOwnerAlreadyExistsIgnoringCase() {
    Player player = buildPlayer("player");
    Map<String, List<Player>> assignments = Map.of("alice", List.of(player));

    Team existingTeam = new Team();
    User existingOwner = new User();
    existingOwner.setUsername("ALICE");
    existingTeam.setOwner(existingOwner);

    when(csvDataLoaderService.getAllPlayerAssignments()).thenReturn(assignments);
    when(teamRepository.findBySeason(2025)).thenReturn(List.of(existingTeam));

    teamInitializationService.createTeamsFromCsvData();

    verify(teamRepository, never()).save(any(Team.class));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void createTeamsFromCsvDataReloadsCsvWhenAssignmentsInitiallyEmpty() {
    Player player = buildPlayer("player");
    Map<String, List<Player>> reloadedAssignments = Map.of("bob", List.of(player));

    User existingUser = new User();
    existingUser.setId(UUID.randomUUID());
    existingUser.setUsername("bob");

    when(csvDataLoaderService.getAllPlayerAssignments()).thenReturn(Map.of(), reloadedAssignments);
    when(teamRepository.findBySeason(2025)).thenReturn(List.of());
    when(userRepository.findByUsernameIgnoreCase("bob")).thenReturn(Optional.of(existingUser));
    when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(playerRepository.getReferenceById(player.getId())).thenReturn(player);

    teamInitializationService.createTeamsFromCsvData();

    verify(csvDataLoaderService).loadAllCsvData();
    verify(teamRepository).save(any(Team.class));
  }

  private Player buildPlayer(String username) {
    Player player = new Player();
    player.setId(UUID.randomUUID());
    player.setUsername(username);
    player.setNickname(username);
    return player;
  }
}
