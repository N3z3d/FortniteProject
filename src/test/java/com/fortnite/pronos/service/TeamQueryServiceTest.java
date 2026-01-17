package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests - TeamQueryService")
class TeamQueryServiceTest {

  @Mock private TeamRepository teamRepository;

  @InjectMocks private TeamQueryService teamQueryService;

  private User testUser;
  private Team testTeam;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("tester");

    testTeam = new Team();
    testTeam.setId(UUID.randomUUID());
    testTeam.setName("Team Test");
    testTeam.setOwner(testUser);
    testTeam.setSeason(2025);
  }

  @Test
  @DisplayName("Devrait retourner les equipes pour une saison")
  void shouldReturnTeamsBySeasonWithFetch() {
    // Given
    when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(List.of(testTeam));

    // When
    List<Team> result = teamQueryService.findTeamsBySeasonWithFetch(2025);

    // Then
    assertThat(result).hasSize(1);
    verify(teamRepository).findBySeasonWithFetch(2025);
  }

  @Test
  @DisplayName("Devrait retourner une liste vide quand aucune equipe")
  void shouldReturnEmptyListWhenNoTeams() {
    // Given
    when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(Collections.emptyList());

    // When
    List<Team> result = teamQueryService.findTeamsBySeasonWithFetch(2025);

    // Then
    assertThat(result).isEmpty();
    verify(teamRepository).findBySeasonWithFetch(2025);
  }

  @Test
  @DisplayName("Devrait retourner une equipe par ID")
  void shouldReturnTeamByIdWithFetch() {
    // Given
    when(teamRepository.findByIdWithFetch(testTeam.getId())).thenReturn(Optional.of(testTeam));

    // When
    Optional<Team> result = teamQueryService.findTeamByIdWithFetch(testTeam.getId());

    // Then
    assertThat(result).contains(testTeam);
    verify(teamRepository).findByIdWithFetch(testTeam.getId());
  }

  @Test
  @DisplayName("Devrait retourner vide quand l'equipe est introuvable")
  void shouldReturnEmptyWhenTeamByIdMissing() {
    // Given
    when(teamRepository.findByIdWithFetch(testTeam.getId())).thenReturn(Optional.empty());

    // When
    Optional<Team> result = teamQueryService.findTeamByIdWithFetch(testTeam.getId());

    // Then
    assertThat(result).isEmpty();
    verify(teamRepository).findByIdWithFetch(testTeam.getId());
  }

  @Test
  @DisplayName("Devrait retourner l'equipe d'un owner pour une saison")
  void shouldReturnTeamByOwnerAndSeason() {
    // Given
    when(teamRepository.findByOwnerAndSeason(testUser, 2025)).thenReturn(Optional.of(testTeam));

    // When
    Optional<Team> result = teamQueryService.findTeamByOwnerAndSeason(testUser, 2025);

    // Then
    assertThat(result).contains(testTeam);
    verify(teamRepository).findByOwnerAndSeason(testUser, 2025);
  }

  @Test
  @DisplayName("Devrait retourner vide quand l'equipe owner/saison est introuvable")
  void shouldReturnEmptyWhenTeamByOwnerAndSeasonMissing() {
    // Given
    when(teamRepository.findByOwnerAndSeason(testUser, 2025)).thenReturn(Optional.empty());

    // When
    Optional<Team> result = teamQueryService.findTeamByOwnerAndSeason(testUser, 2025);

    // Then
    assertThat(result).isEmpty();
    verify(teamRepository).findByOwnerAndSeason(testUser, 2025);
  }
}
