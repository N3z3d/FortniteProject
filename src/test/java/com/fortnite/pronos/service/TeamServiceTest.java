package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

/** Tests TDD pour TeamService */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests TDD - TeamService")
class TeamServiceTest {

  @Mock private TeamRepository teamRepository;

  @Mock private PlayerRepository playerRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private TeamService teamService;

  private User testUser;
  private Player testPlayer;
  private Player testPlayer2;
  private Team testTeam;
  private TeamDto testTeamDto;

  @BeforeEach
  void setUp() {
    // Créer un utilisateur de test
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword("password123"); // Ajout du mot de passe requis
    testUser.setCurrentSeason(2025);

    // Créer des joueurs de test
    testPlayer = new Player();
    testPlayer.setId(UUID.randomUUID());
    testPlayer.setUsername("TestPlayer1");
    testPlayer.setNickname("TP1");
    testPlayer.setRegion(Player.Region.EU);
    testPlayer.setTranche("S");

    testPlayer2 = new Player();
    testPlayer2.setId(UUID.randomUUID());
    testPlayer2.setUsername("TestPlayer2");
    testPlayer2.setNickname("TP2");
    testPlayer2.setRegion(Player.Region.NAW);
    testPlayer2.setTranche("A");

    // Créer une équipe de test
    testTeam = new Team();
    testTeam.setId(UUID.randomUUID());
    testTeam.setName("Test Team");
    testTeam.setOwner(testUser);
    testTeam.setSeason(2025);

    // Créer un DTO de test
    testTeamDto = new TeamDto();
    testTeamDto.setId(testTeam.getId());
    testTeamDto.setName(testTeam.getName());
    testTeamDto.setUserId(testUser.getId());
    testTeamDto.setSeason(2025);
  }

  @Test
  @DisplayName("Devrait récupérer l'équipe d'un utilisateur pour une saison")
  void shouldGetTeamByUserAndSeason() {
    // Given
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamRepository.findByOwnerAndSeason(testUser, 2025)).thenReturn(Optional.of(testTeam));

    // When
    TeamDto result = teamService.getTeam(testUser.getId(), 2025);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    assertThat(result.getName()).isEqualTo(testTeam.getName());
    verify(userRepository).findById(testUser.getId());
    verify(teamRepository).findByOwnerAndSeason(testUser, 2025);
  }

  @Test
  @DisplayName("Devrait lever une exception quand l'utilisateur n'existe pas")
  void shouldThrowExceptionWhenUserNotFound() {
    // Given
    when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> teamService.getTeam(UUID.randomUUID(), 2025))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Utilisateur non trouvé");
  }

  @Test
  @DisplayName("Devrait lever une exception quand l'équipe n'existe pas")
  void shouldThrowExceptionWhenTeamNotFound() {
    // Given
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamRepository.findByOwnerAndSeason(testUser, 2025)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> teamService.getTeam(testUser.getId(), 2025))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Équipe non trouvée");
  }

  @Test
  @DisplayName("Devrait récupérer une équipe par son ID")
  void shouldGetTeamById() {
    // Given
    when(teamRepository.findByIdWithFetch(testTeam.getId())).thenReturn(Optional.of(testTeam));

    // When
    TeamDto result = teamService.getTeamById(testTeam.getId());

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(teamRepository).findByIdWithFetch(testTeam.getId());
  }

  @Test
  @DisplayName("Devrait lever une exception quand l'équipe n'existe pas par ID")
  void shouldThrowExceptionWhenTeamNotFoundById() {
    // Given
    when(teamRepository.findByIdWithFetch(any(UUID.class))).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> teamService.getTeamById(UUID.randomUUID()))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Équipe non trouvée");
  }

  @Test
  @DisplayName("Devrait récupérer toutes les équipes d'une saison")
  void shouldGetAllTeamsBySeason() {
    // Given
    List<Team> teams = Arrays.asList(testTeam);
    when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(teams);

    // When
    List<TeamDto> result = teamService.getAllTeams(2025);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(testTeam.getId());
    verify(teamRepository).findBySeasonWithFetch(2025);
  }

  @Test
  @DisplayName("Devrait créer une nouvelle équipe pour un utilisateur")
  void shouldCreateTeamForUser() {
    // Given
    String teamName = "New Team";
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamRepository.findByOwnerAndSeason(testUser, 2025)).thenReturn(Optional.empty());
    when(teamRepository.save(any(Team.class))).thenReturn(testTeam);

    // When
    TeamDto result = teamService.createTeam(testUser.getId(), teamName, 2025);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(userRepository).findById(testUser.getId());
    verify(teamRepository).findByOwnerAndSeason(testUser, 2025);
    verify(teamRepository).save(any(Team.class));
  }

  @Test
  @DisplayName("Devrait lever une exception quand l'utilisateur a déjà une équipe")
  void shouldThrowExceptionWhenUserAlreadyHasTeam() {
    // Given
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamRepository.findByOwnerAndSeason(testUser, 2025)).thenReturn(Optional.of(testTeam));

    // When & Then
    assertThatThrownBy(() -> teamService.createTeam(testUser.getId(), "New Team", 2025))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("L'utilisateur a déjà une équipe pour cette saison");
  }

  @Test
  @DisplayName("Devrait ajouter un joueur à une équipe")
  void shouldAddPlayerToTeam() {
    // Given
    int position = 1;
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(playerRepository.findById(testPlayer.getId())).thenReturn(Optional.of(testPlayer));
    when(teamRepository.findByOwnerAndSeason(testUser, 2025)).thenReturn(Optional.of(testTeam));
    when(teamRepository.save(any(Team.class))).thenReturn(testTeam);

    // When
    TeamDto result =
        teamService.addPlayerToTeam(testUser.getId(), testPlayer.getId(), position, 2025);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(userRepository).findById(testUser.getId());
    verify(playerRepository).findById(testPlayer.getId());
    verify(teamRepository).findByOwnerAndSeason(testUser, 2025);
    verify(teamRepository).save(testTeam);
  }

  @Test
  @DisplayName("Devrait lever une exception quand le joueur n'existe pas")
  void shouldThrowExceptionWhenPlayerNotFound() {
    // Given
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(playerRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(
            () -> teamService.addPlayerToTeam(testUser.getId(), UUID.randomUUID(), 1, 2025))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Joueur non trouvé");
  }

  @Test
  @DisplayName("Devrait retirer un joueur d'une équipe")
  void shouldRemovePlayerFromTeam() {
    // Given
    testTeam.addPlayer(testPlayer, 1); // Add player to team first so it can be removed
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(playerRepository.findById(testPlayer.getId())).thenReturn(Optional.of(testPlayer));
    when(teamRepository.findByOwnerAndSeason(testUser, 2025)).thenReturn(Optional.of(testTeam));
    when(teamRepository.save(any(Team.class))).thenReturn(testTeam);

    // When
    TeamDto result = teamService.removePlayerFromTeam(testUser.getId(), testPlayer.getId(), 2025);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(userRepository).findById(testUser.getId());
    verify(playerRepository).findById(testPlayer.getId());
    verify(teamRepository).findByOwnerAndSeason(testUser, 2025);
    verify(teamRepository).save(testTeam);
  }

  @Test
  @DisplayName("Devrait effectuer des changements de joueurs en lot")
  void shouldMakePlayerChanges() {
    // Given
    testTeam.addPlayer(testPlayer, 1); // Add player to team first so it can be replaced
    Map<UUID, UUID> playerChanges = new HashMap<>();
    playerChanges.put(testPlayer.getId(), testPlayer2.getId());

    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(playerRepository.findById(testPlayer.getId())).thenReturn(Optional.of(testPlayer));
    when(playerRepository.findById(testPlayer2.getId())).thenReturn(Optional.of(testPlayer2));
    when(teamRepository.findByOwnerAndSeason(testUser, 2025)).thenReturn(Optional.of(testTeam));
    when(teamRepository.save(any(Team.class))).thenReturn(testTeam);

    // When
    TeamDto result = teamService.makePlayerChanges(testUser.getId(), playerChanges, 2025);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(userRepository).findById(testUser.getId());
    verify(teamRepository).findByOwnerAndSeason(testUser, 2025);
    verify(teamRepository).save(testTeam);
  }

  @Test
  @DisplayName("Devrait récupérer l'équipe d'un joueur")
  void shouldGetTeamByPlayer() {
    // Given
    when(playerRepository.findById(testPlayer.getId())).thenReturn(Optional.of(testPlayer));
    when(teamRepository.findTeamByPlayerAndSeason(testPlayer.getId(), 2025))
        .thenReturn(Optional.of(testTeam));

    // When
    TeamDto result = teamService.getTeamByPlayer(testPlayer.getId(), 2025);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(playerRepository).findById(testPlayer.getId());
    verify(teamRepository).findTeamByPlayerAndSeason(testPlayer.getId(), 2025);
  }

  @Test
  @DisplayName("Devrait lever une exception quand le joueur n'existe pas pour getTeamByPlayer")
  void shouldThrowExceptionWhenPlayerNotFoundForGetTeamByPlayer() {
    // Given
    when(playerRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> teamService.getTeamByPlayer(UUID.randomUUID(), 2025))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Joueur non trouvé");
  }

  @Test
  @DisplayName("Devrait lever une exception quand l'équipe n'existe pas pour getTeamByPlayer")
  void shouldThrowExceptionWhenTeamNotFoundForGetTeamByPlayer() {
    // Given
    when(playerRepository.findById(testPlayer.getId())).thenReturn(Optional.of(testPlayer));
    when(teamRepository.findTeamByPlayerAndSeason(testPlayer.getId(), 2025))
        .thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> teamService.getTeamByPlayer(testPlayer.getId(), 2025))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Équipe non trouvée");
  }

  @Test
  @DisplayName("Devrait récupérer les équipes par joueurs")
  void shouldGetTeamsByPlayers() {
    // Given
    List<UUID> playerIds = Arrays.asList(testPlayer.getId(), testPlayer2.getId());
    when(playerRepository.findById(testPlayer.getId())).thenReturn(Optional.of(testPlayer));
    when(playerRepository.findById(testPlayer2.getId())).thenReturn(Optional.of(testPlayer2));
    when(teamRepository.findTeamByPlayerAndSeason(testPlayer.getId(), 2025))
        .thenReturn(Optional.of(testTeam));
    when(teamRepository.findTeamByPlayerAndSeason(testPlayer2.getId(), 2025))
        .thenReturn(Optional.of(testTeam));

    // When
    Map<UUID, TeamDto> result = teamService.getTeamsByPlayers(playerIds, 2025);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result.get(testPlayer.getId())).isNotNull();
    assertThat(result.get(testPlayer2.getId())).isNotNull();
    verify(playerRepository, times(2)).findById(any(UUID.class));
    verify(teamRepository, times(2)).findTeamByPlayerAndSeason(any(UUID.class), eq(2025));
  }

  @Test
  @DisplayName("Devrait récupérer les équipes par saison")
  void shouldGetTeamsBySeason() {
    // Given
    List<Team> teams = Arrays.asList(testTeam);
    when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(teams);

    // When
    List<TeamDto> result = teamService.getTeamsBySeason(2025);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(testTeam.getId());
    verify(teamRepository).findBySeasonWithFetch(2025);
  }

  @Test
  @DisplayName("Devrait récupérer les équipes de participants")
  void shouldGetParticipantTeams() {
    // Given
    List<Team> teams = Arrays.asList(testTeam);
    when(teamRepository.findParticipantTeamsWithFetch(2025)).thenReturn(teams);

    // When
    List<TeamDto> result = teamService.getParticipantTeams(2025);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(testTeam.getId());
    verify(teamRepository).findParticipantTeamsWithFetch(2025);
  }

  @Test
  @DisplayName("Devrait retourner une liste vide pour les équipes de Marcel")
  void shouldReturnEmptyListForMarcelTeams() {
    // When
    List<TeamDto> result = teamService.getMarcelTeams(2025);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Devrait récupérer l'équipe d'un utilisateur et saison (nouvelle signature)")
  void shouldGetTeamByUserAndSeasonNewSignature() {
    // Given
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamRepository.findByOwnerAndSeason(testUser, 2025)).thenReturn(Optional.of(testTeam));

    // When
    TeamDto result = teamService.getTeamByUserAndSeason(testUser.getId(), 2025);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(userRepository).findById(testUser.getId());
    verify(teamRepository).findByOwnerAndSeason(testUser, 2025);
  }

  @Test
  @DisplayName("Devrait créer une équipe avec TeamDto")
  void shouldCreateTeamWithTeamDto() {
    // Given
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamRepository.save(any(Team.class))).thenReturn(testTeam);

    // When
    TeamDto result = teamService.createTeam(testTeamDto);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(userRepository).findById(testUser.getId());
    verify(teamRepository).save(any(Team.class));
  }

  @Test
  @DisplayName(
      "Devrait lever une exception quand l'utilisateur n'existe pas pour createTeam avec TeamDto")
  void shouldThrowExceptionWhenUserNotFoundForCreateTeamWithTeamDto() {
    // Given
    when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> teamService.createTeam(testTeamDto))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Utilisateur non trouvé");
  }

  @Test
  @DisplayName("Devrait mettre à jour une équipe")
  void shouldUpdateTeam() {
    // Given
    when(teamRepository.findById(testTeam.getId())).thenReturn(Optional.of(testTeam));
    when(teamRepository.save(any(Team.class))).thenReturn(testTeam);

    // When
    TeamDto result = teamService.updateTeam(testTeam.getId(), testTeamDto);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(teamRepository).findById(testTeam.getId());
    verify(teamRepository).save(testTeam);
  }

  @Test
  @DisplayName("Devrait lever une exception quand l'équipe n'existe pas pour updateTeam")
  void shouldThrowExceptionWhenTeamNotFoundForUpdateTeam() {
    // Given
    when(teamRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> teamService.updateTeam(UUID.randomUUID(), testTeamDto))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Équipe non trouvée");
  }

  @Test
  @DisplayName("Devrait supprimer une équipe")
  void shouldDeleteTeam() {
    // Given
    when(teamRepository.existsById(testTeam.getId())).thenReturn(true);

    // When
    teamService.deleteTeam(testTeam.getId());

    // Then
    verify(teamRepository).existsById(testTeam.getId());
    verify(teamRepository).deleteById(testTeam.getId());
  }

  @Test
  @DisplayName("Devrait lever une exception quand l'équipe n'existe pas pour deleteTeam")
  void shouldThrowExceptionWhenTeamNotFoundForDeleteTeam() {
    // Given
    when(teamRepository.existsById(any(UUID.class))).thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> teamService.deleteTeam(UUID.randomUUID()))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Équipe non trouvée");
  }
}
