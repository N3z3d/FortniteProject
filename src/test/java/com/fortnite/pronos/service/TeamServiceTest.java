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
import com.fortnite.pronos.repository.TeamPlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

/**
 * Tests TDD pour TeamService (commandes uniquement).
 *
 * <p>Note: Les tests pour les methodes de lecture (queries) ont ete deplaces vers
 * TeamQueryServiceTddTest suite a l'extraction de TeamQueryService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests TDD - TeamService (Commands)")
class TeamServiceTest {

  @Mock private TeamRepository teamRepository;

  @Mock private PlayerRepository playerRepository;

  @Mock private UserRepository userRepository;

  @Mock private TeamPlayerRepository teamPlayerRepository;

  @InjectMocks private TeamService teamService;

  private User testUser;
  private Player testPlayer;
  private Player testPlayer2;
  private Team testTeam;
  private TeamDto testTeamDto;

  @BeforeEach
  void setUp() {
    // Creer un utilisateur de test
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword("password123");
    testUser.setCurrentSeason(2025);

    // Creer des joueurs de test
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

    // Creer une equipe de test
    testTeam = new Team();
    testTeam.setId(UUID.randomUUID());
    testTeam.setName("Test Team");
    testTeam.setOwner(testUser);
    testTeam.setSeason(2025);

    // Creer un DTO de test
    testTeamDto = new TeamDto();
    testTeamDto.setId(testTeam.getId());
    testTeamDto.setName(testTeam.getName());
    testTeamDto.setUserId(testUser.getId());
    testTeamDto.setSeason(2025);
  }

  @Test
  @DisplayName("Devrait creer une nouvelle equipe pour un utilisateur")
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
  @DisplayName("Devrait lever une exception quand l'utilisateur a deja une equipe")
  void shouldThrowExceptionWhenUserAlreadyHasTeam() {
    // Given
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamRepository.findByOwnerAndSeason(testUser, 2025)).thenReturn(Optional.of(testTeam));

    // When & Then
    assertThatThrownBy(() -> teamService.createTeam(testUser.getId(), "New Team", 2025))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("L'utilisateur a deja une equipe pour cette saison");
  }

  @Test
  @DisplayName("Devrait ajouter un joueur a une equipe")
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
        .hasMessage("Joueur non trouve");
  }

  @Test
  @DisplayName("Devrait retirer un joueur d'une equipe")
  void shouldRemovePlayerFromTeam() {
    // Given
    testTeam.addPlayer(testPlayer, 1);
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
    testTeam.addPlayer(testPlayer, 1);
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
  @DisplayName("Devrait creer une equipe avec TeamDto")
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
        .hasMessage("Utilisateur non trouve");
  }

  @Test
  @DisplayName("Devrait mettre a jour une equipe")
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
  @DisplayName("Devrait lever une exception quand l'equipe n'existe pas pour updateTeam")
  void shouldThrowExceptionWhenTeamNotFoundForUpdateTeam() {
    // Given
    when(teamRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> teamService.updateTeam(UUID.randomUUID(), testTeamDto))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Equipe non trouvee");
  }

  @Test
  @DisplayName("Devrait supprimer une equipe")
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
  @DisplayName("Devrait lever une exception quand l'equipe n'existe pas pour deleteTeam")
  void shouldThrowExceptionWhenTeamNotFoundForDeleteTeam() {
    // Given
    when(teamRepository.existsById(any(UUID.class))).thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> teamService.deleteTeam(UUID.randomUUID()))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Equipe non trouvee");
  }
}
