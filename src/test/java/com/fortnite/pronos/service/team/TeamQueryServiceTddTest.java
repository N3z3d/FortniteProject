package com.fortnite.pronos.service.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamQueryService - TDD Tests")
class TeamQueryServiceTddTest {

  @Mock private TeamRepository teamRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private TeamQueryService teamQueryService;

  private User testUser;
  private Team testTeam;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("testuser");

    testTeam = new Team();
    testTeam.setId(UUID.randomUUID());
    testTeam.setName("Test Team");
    testTeam.setOwner(testUser);
    testTeam.setSeason(2025);
  }

  @Test
  @DisplayName("getTeam returns team for user and season")
  void getTeamReturnsTeamForUserAndSeason() {
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamRepository.findByOwnerAndSeason(testUser, 2025)).thenReturn(Optional.of(testTeam));

    TeamDto result = teamQueryService.getTeam(testUser.getId(), 2025);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Test Team");
  }

  @Test
  @DisplayName("getTeam throws exception when user not found")
  void getTeamThrowsExceptionWhenUserNotFound() {
    UUID unknownUserId = UUID.randomUUID();
    when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> teamQueryService.getTeam(unknownUserId, 2025))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  @DisplayName("getTeamById returns team when found")
  void getTeamByIdReturnsTeamWhenFound() {
    when(teamRepository.findByIdWithFetch(testTeam.getId())).thenReturn(Optional.of(testTeam));

    TeamDto result = teamQueryService.getTeamById(testTeam.getId());

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Test Team");
  }

  @Test
  @DisplayName("getTeamById throws exception when not found")
  void getTeamByIdThrowsExceptionWhenNotFound() {
    UUID unknownTeamId = UUID.randomUUID();
    when(teamRepository.findByIdWithFetch(unknownTeamId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> teamQueryService.getTeamById(unknownTeamId))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  @DisplayName("getAllTeams returns all teams for season")
  void getAllTeamsReturnsAllTeamsForSeason() {
    Team team2 = new Team();
    team2.setId(UUID.randomUUID());
    team2.setName("Team 2");
    team2.setOwner(testUser);

    when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(List.of(testTeam, team2));

    List<TeamDto> result = teamQueryService.getAllTeams(2025);

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("getTeamsByGame returns teams for game")
  void getTeamsByGameReturnsTeamsForGame() {
    UUID gameId = UUID.randomUUID();
    when(teamRepository.findByGameIdWithFetch(gameId)).thenReturn(List.of(testTeam));

    List<TeamDto> result = teamQueryService.getTeamsByGame(gameId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Test Team");
  }

  @Test
  @DisplayName("getMarcelTeams returns empty list (obsolete)")
  void getMarcelTeamsReturnsEmptyList() {
    List<TeamDto> result = teamQueryService.getMarcelTeams(2025);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("getTeamsBySeason returns teams for season")
  void getTeamsBySeasonReturnsTeamsForSeason() {
    when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(List.of(testTeam));

    List<TeamDto> result = teamQueryService.getTeamsBySeason(2025);

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("getParticipantTeams returns participant teams")
  void getParticipantTeamsReturnsParticipantTeams() {
    when(teamRepository.findParticipantTeamsWithFetch(2025)).thenReturn(List.of(testTeam));

    List<TeamDto> result = teamQueryService.getParticipantTeams(2025);

    assertThat(result).hasSize(1);
  }
}
