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

import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.TeamDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.domain.team.model.Team;
import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamQueryService - TDD Tests")
class TeamQueryServiceTddTest {

  @Mock private TeamDomainRepositoryPort teamDomainRepository;
  @Mock private PlayerDomainRepositoryPort playerDomainRepository;
  @Mock private UserRepositoryPort userRepository;
  @Mock private TeamRepository teamRepository;

  @InjectMocks private TeamQueryService teamQueryService;

  private User testUser;
  private Team testTeam;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("testuser");
    testUser.setEmail("testuser@example.com");

    testTeam =
        Team.restore(UUID.randomUUID(), "Test Team", testUser.getId(), 2025, null, 0, List.of());
  }

  @Test
  @DisplayName("getTeam returns team for user and season")
  void getTeamReturnsTeamForUserAndSeason() {
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser.getId(), 2025))
        .thenReturn(Optional.of(testTeam));

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
    when(teamDomainRepository.findByIdWithFetch(testTeam.getId()))
        .thenReturn(Optional.of(testTeam));

    TeamDto result = teamQueryService.getTeamById(testTeam.getId());

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Test Team");
  }

  @Test
  @DisplayName("getTeamById throws exception when not found")
  void getTeamByIdThrowsExceptionWhenNotFound() {
    UUID unknownTeamId = UUID.randomUUID();
    when(teamDomainRepository.findByIdWithFetch(unknownTeamId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> teamQueryService.getTeamById(unknownTeamId))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  @DisplayName("getAllTeams returns all teams for season")
  void getAllTeamsReturnsAllTeamsForSeason() {
    Team team2 =
        Team.restore(UUID.randomUUID(), "Team 2", testUser.getId(), 2025, null, 0, List.of());

    when(teamDomainRepository.findBySeasonWithFetch(2025)).thenReturn(List.of(testTeam, team2));

    List<TeamDto> result = teamQueryService.getAllTeams(2025);

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("getTeamsByGame returns teams for game")
  void getTeamsByGameReturnsTeamsForGame() {
    UUID gameId = UUID.randomUUID();
    when(teamDomainRepository.findByGameIdWithFetch(gameId)).thenReturn(List.of(testTeam));

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
    when(teamDomainRepository.findBySeasonWithFetch(2025)).thenReturn(List.of(testTeam));

    List<TeamDto> result = teamQueryService.getTeamsBySeason(2025);

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("getParticipantTeams returns participant teams")
  void getParticipantTeamsReturnsParticipantTeams() {
    when(teamDomainRepository.findBySeasonWithFetch(2025)).thenReturn(List.of(testTeam));
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    List<TeamDto> result = teamQueryService.getParticipantTeams(2025);

    assertThat(result).hasSize(1);
  }
}
