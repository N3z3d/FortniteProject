package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.SwapPlayersRequest;
import com.fortnite.pronos.dto.SwapPlayersResponse;
import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.exception.InvalidSwapException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamPlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

/**
 * TDD Tests for TeamService - Business Critical Component (Commands Only)
 *
 * <p>This test suite validates team management, player operations, and complex business workflows
 * using RED-GREEN-REFACTOR TDD methodology. TeamService handles team creation, player management,
 * swapping mechanics, and ownership validation essential for the fantasy league team operations.
 *
 * <p>Note: Query/retrieval tests have been moved to TeamQueryServiceTddTest following the
 * extraction of TeamQueryService to respect SRP.
 *
 * <p>Business Logic Areas: - Team creation and ownership management - Player addition/removal and
 * position management - Complex player swapping between teams - Team validation and business rules
 * enforcement
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService - Business Critical TDD Tests (Commands)")
class TeamServiceTddTest {

  @Mock private TeamRepository teamRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private UserRepository userRepository;
  @Mock private TeamPlayerRepository teamPlayerRepository;

  @InjectMocks private TeamService teamService;

  private User testUser1;
  private User testUser2;
  private Team testTeam1;
  private Team testTeam2;
  private Player testPlayer1;
  private Player testPlayer2;
  private TeamPlayer testTeamPlayer1;
  private TeamPlayer testTeamPlayer2;
  private UUID userId1;
  private UUID userId2;
  private UUID teamId1;
  private UUID teamId2;
  private UUID playerId1;
  private UUID playerId2;
  private int testSeason = 2025;

  @BeforeEach
  void setUp() {
    // Users setup
    userId1 = UUID.randomUUID();
    userId2 = UUID.randomUUID();

    testUser1 = new User();
    testUser1.setId(userId1);
    testUser1.setUsername("champion_owner");
    testUser1.setEmail("champion@test.com");
    testUser1.setCurrentSeason(testSeason);

    testUser2 = new User();
    testUser2.setId(userId2);
    testUser2.setUsername("competitor_owner");
    testUser2.setEmail("competitor@test.com");
    testUser2.setCurrentSeason(testSeason);

    // Players setup
    playerId1 = UUID.randomUUID();
    playerId2 = UUID.randomUUID();

    testPlayer1 =
        Player.builder()
            .id(playerId1)
            .username("star_player")
            .nickname("Star Player")
            .region(Player.Region.EU)
            .tranche("1-3")
            .currentSeason(testSeason)
            .build();

    testPlayer2 =
        Player.builder()
            .id(playerId2)
            .username("backup_player")
            .nickname("Backup Player")
            .region(Player.Region.NAW)
            .tranche("4-7")
            .currentSeason(testSeason)
            .build();

    // Teams setup
    teamId1 = UUID.randomUUID();
    teamId2 = UUID.randomUUID();

    testTeam1 = new Team();
    testTeam1.setId(teamId1);
    testTeam1.setName("Champions Team");
    testTeam1.setOwner(testUser1);
    testTeam1.setSeason(testSeason);
    testTeam1.setPlayers(new ArrayList<>());

    testTeam2 = new Team();
    testTeam2.setId(teamId2);
    testTeam2.setName("Competitors Team");
    testTeam2.setOwner(testUser2);
    testTeam2.setSeason(testSeason);
    testTeam2.setPlayers(new ArrayList<>());

    // TeamPlayers setup
    testTeamPlayer1 = new TeamPlayer();
    testTeamPlayer1.setTeam(testTeam1);
    testTeamPlayer1.setPlayer(testPlayer1);
    testTeamPlayer1.setPosition(1);

    testTeamPlayer2 = new TeamPlayer();
    testTeamPlayer2.setTeam(testTeam2);
    testTeamPlayer2.setPlayer(testPlayer2);
    testTeamPlayer2.setPosition(1);

    // Add players to teams
    testTeam1.getPlayers().add(testTeamPlayer1);
    testTeam2.getPlayers().add(testTeamPlayer2);
  }

  @Nested
  @DisplayName("Team Creation and Management")
  class TeamCreationTests {

    @Test
    @DisplayName("Should create team successfully for valid user")
    void shouldCreateTeamSuccessfullyForValidUser() {
      // RED: Define expected behavior for team creation
      String teamName = "New Dream Team";

      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(teamRepository.findByOwnerAndSeason(testUser1, testSeason)).thenReturn(Optional.empty());
      when(teamRepository.save(any(Team.class)))
          .thenAnswer(
              invocation -> {
                Team team = invocation.getArgument(0);
                team.setId(UUID.randomUUID());
                return team;
              });

      TeamDto result = teamService.createTeam(userId1, teamName, testSeason);

      assertThat(result.getName()).isEqualTo(teamName);
      assertThat(result.getOwnerUsername()).isEqualTo("champion_owner");
      assertThat(result.getSeason()).isEqualTo(testSeason);

      verify(userRepository).findById(userId1);
      verify(teamRepository).findByOwnerAndSeason(testUser1, testSeason);
      verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("Should reject team creation for non-existent user")
    void shouldRejectTeamCreationForNonExistentUser() {
      // RED: Test user validation
      UUID nonExistentUserId = UUID.randomUUID();
      when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> teamService.createTeam(nonExistentUserId, "Test Team", testSeason))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Utilisateur non trouve");

      verify(userRepository).findById(nonExistentUserId);
      verifyNoInteractions(teamRepository);
    }

    @Test
    @DisplayName("Should reject duplicate team creation for same user and season")
    void shouldRejectDuplicateTeamCreationForSameUserAndSeason() {
      // RED: Test duplicate team validation
      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(teamRepository.findByOwnerAndSeason(testUser1, testSeason))
          .thenReturn(Optional.of(testTeam1));

      assertThatThrownBy(() -> teamService.createTeam(userId1, "Duplicate Team", testSeason))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("L'utilisateur a deja une equipe pour cette saison");

      verify(userRepository).findById(userId1);
      verify(teamRepository).findByOwnerAndSeason(testUser1, testSeason);
      verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("Should allow multiple teams for different seasons")
    void shouldAllowMultipleTeamsForDifferentSeasons() {
      // RED: Test different seasons validation
      int differentSeason = 2024;

      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(teamRepository.findByOwnerAndSeason(testUser1, differentSeason))
          .thenReturn(Optional.empty());
      when(teamRepository.save(any(Team.class)))
          .thenAnswer(
              invocation -> {
                Team team = invocation.getArgument(0);
                team.setId(UUID.randomUUID());
                return team;
              });

      TeamDto result = teamService.createTeam(userId1, "Season 2024 Team", differentSeason);

      assertThat(result.getName()).isEqualTo("Season 2024 Team");
      assertThat(result.getSeason()).isEqualTo(differentSeason);
    }
  }

  @Nested
  @DisplayName("Player Management Operations")
  class PlayerManagementTests {

    @Test
    @DisplayName("Should add player to team successfully")
    void shouldAddPlayerToTeamSuccessfully() {
      // RED: Test player addition to team
      Player newPlayer =
          Player.builder()
              .id(UUID.randomUUID())
              .username("new_recruit")
              .nickname("New Recruit")
              .region(Player.Region.BR)
              .tranche("5-8")
              .build();

      Team emptyTeam = new Team();
      emptyTeam.setId(teamId1);
      emptyTeam.setName("Empty Team");
      emptyTeam.setOwner(testUser1);
      emptyTeam.setSeason(testSeason);
      emptyTeam.setPlayers(new ArrayList<>());

      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(playerRepository.findById(newPlayer.getId())).thenReturn(Optional.of(newPlayer));
      when(teamRepository.findByOwnerAndSeason(testUser1, testSeason))
          .thenReturn(Optional.of(emptyTeam));
      when(teamRepository.save(any(Team.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      TeamDto result = teamService.addPlayerToTeam(userId1, newPlayer.getId(), 1, testSeason);

      assertThat(result.getId()).isEqualTo(teamId1);
      verify(userRepository).findById(userId1);
      verify(playerRepository).findById(newPlayer.getId());
      verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("Should reject adding duplicate player to team")
    void shouldRejectAddingDuplicatePlayerToTeam() {
      // RED: Test duplicate player validation
      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(teamRepository.findByOwnerAndSeason(testUser1, testSeason))
          .thenReturn(Optional.of(testTeam1));

      // Player is already in the team (configured in setup)
      // testTeam1.getPlayers() already contains testTeamPlayer1 with testPlayer1

      assertThatThrownBy(() -> teamService.addPlayerToTeam(userId1, playerId1, 2, testSeason))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Le joueur est deja dans l'equipe");

      verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("Should remove player from team successfully")
    void shouldRemovePlayerFromTeamSuccessfully() {
      // RED: Test player removal from team
      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(teamRepository.findByOwnerAndSeason(testUser1, testSeason))
          .thenReturn(Optional.of(testTeam1));
      when(teamRepository.save(any(Team.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      TeamDto result = teamService.removePlayerFromTeam(userId1, playerId1, testSeason);

      assertThat(result.getId()).isEqualTo(teamId1);
      verify(userRepository).findById(userId1);
      verify(playerRepository).findById(playerId1);
      verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("Should reject removing non-existent player from team")
    void shouldRejectRemovingNonExistentPlayerFromTeam() {
      // RED: Test removing player not in team
      Player notInTeamPlayer =
          Player.builder()
              .id(UUID.randomUUID())
              .username("outsider")
              .nickname("Outsider")
              .region(Player.Region.ASIA)
              .tranche("1-10")
              .build();

      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(playerRepository.findById(notInTeamPlayer.getId()))
          .thenReturn(Optional.of(notInTeamPlayer));
      when(teamRepository.findByOwnerAndSeason(testUser1, testSeason))
          .thenReturn(Optional.of(testTeam1));

      assertThatThrownBy(
              () -> teamService.removePlayerFromTeam(userId1, notInTeamPlayer.getId(), testSeason))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Le joueur n'est pas dans l'equipe");

      verify(teamRepository, never()).save(any(Team.class));
    }
  }

  @Nested
  @DisplayName("Player Swapping Operations")
  class PlayerSwappingTests {

    @Test
    @DisplayName("Should execute player swap successfully between teams")
    void shouldExecutePlayerSwapSuccessfullyBetweenTeams() {
      // RED: Test successful player swap
      SwapPlayersRequest request = new SwapPlayersRequest(teamId1, teamId2, playerId1, playerId2);

      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(teamRepository.findById(teamId1)).thenReturn(Optional.of(testTeam1));
      when(teamRepository.findById(teamId2)).thenReturn(Optional.of(testTeam2));
      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(playerRepository.findById(playerId2)).thenReturn(Optional.of(testPlayer2));
      when(teamPlayerRepository.findByTeamAndPlayer(testTeam1, testPlayer1))
          .thenReturn(Optional.of(testTeamPlayer1));
      when(teamPlayerRepository.findByTeamAndPlayer(testTeam2, testPlayer2))
          .thenReturn(Optional.of(testTeamPlayer2));

      SwapPlayersResponse result = teamService.swapPlayers(userId1, request);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getMessage()).contains("Echange effectue avec succes");
      assertThat(result.getSwapDetails()).isNotNull();

      verify(teamPlayerRepository).save(testTeamPlayer1);
      verify(teamPlayerRepository).save(testTeamPlayer2);
    }

    @Test
    @DisplayName("Should reject swap when user doesn't own either team")
    void shouldRejectSwapWhenUserDoesntOwnEitherTeam() {
      // RED: Test unauthorized swap attempt
      UUID unauthorizedUserId = UUID.randomUUID();
      User unauthorizedUser = new User();
      unauthorizedUser.setId(unauthorizedUserId);
      unauthorizedUser.setUsername("unauthorized");

      SwapPlayersRequest request = new SwapPlayersRequest(teamId1, teamId2, playerId1, playerId2);

      when(userRepository.findById(unauthorizedUserId)).thenReturn(Optional.of(unauthorizedUser));
      when(teamRepository.findById(teamId1)).thenReturn(Optional.of(testTeam1));
      when(teamRepository.findById(teamId2)).thenReturn(Optional.of(testTeam2));

      assertThatThrownBy(() -> teamService.swapPlayers(unauthorizedUserId, request))
          .isInstanceOf(UnauthorizedAccessException.class)
          .hasMessageContaining("Vous devez etre proprietaire d'au moins une des equipes");

      verifyNoInteractions(teamPlayerRepository);
    }

    @Test
    @DisplayName("Should reject swap between teams from different seasons")
    void shouldRejectSwapBetweenTeamsFromDifferentSeasons() {
      // RED: Test season validation for swaps
      Team differentSeasonTeam = new Team();
      differentSeasonTeam.setId(teamId2);
      differentSeasonTeam.setName("Different Season Team");
      differentSeasonTeam.setOwner(testUser2);
      differentSeasonTeam.setSeason(2024); // Different season

      SwapPlayersRequest request = new SwapPlayersRequest(teamId1, teamId2, playerId1, playerId2);

      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(teamRepository.findById(teamId1)).thenReturn(Optional.of(testTeam1));
      when(teamRepository.findById(teamId2)).thenReturn(Optional.of(differentSeasonTeam));

      assertThatThrownBy(() -> teamService.swapPlayers(userId1, request))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("Les equipes doivent etre dans la meme saison");

      verifyNoInteractions(teamPlayerRepository);
    }

    @Test
    @DisplayName("Should reject swap when player not in specified team")
    void shouldRejectSwapWhenPlayerNotInSpecifiedTeam() {
      // RED: Test player-team association validation
      SwapPlayersRequest request = new SwapPlayersRequest(teamId1, teamId2, playerId1, playerId2);

      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(teamRepository.findById(teamId1)).thenReturn(Optional.of(testTeam1));
      when(teamRepository.findById(teamId2)).thenReturn(Optional.of(testTeam2));
      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(playerRepository.findById(playerId2)).thenReturn(Optional.of(testPlayer2));
      when(teamPlayerRepository.findByTeamAndPlayer(testTeam1, testPlayer1))
          .thenReturn(Optional.empty()); // Player not in team

      assertThatThrownBy(() -> teamService.swapPlayers(userId1, request))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("Le joueur Star Player n'est pas dans l'equipe Champions Team");

      verify(teamPlayerRepository, never()).save(any(TeamPlayer.class));
    }
  }

  @Nested
  @DisplayName("Team Updates and Maintenance")
  class TeamMaintenanceTests {

    @Test
    @DisplayName("Should update team successfully")
    void shouldUpdateTeamSuccessfully() {
      // RED: Test team update
      TeamDto updateDto = new TeamDto();
      updateDto.setName("Updated Team Name");

      when(teamRepository.findById(teamId1)).thenReturn(Optional.of(testTeam1));
      when(teamRepository.save(any(Team.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      TeamDto result = teamService.updateTeam(teamId1, updateDto);

      assertThat(result.getId()).isEqualTo(teamId1);
      verify(teamRepository).findById(teamId1);
      verify(teamRepository).save(testTeam1);
    }

    @Test
    @DisplayName("Should delete team successfully")
    void shouldDeleteTeamSuccessfully() {
      // RED: Test team deletion
      when(teamRepository.existsById(teamId1)).thenReturn(true);

      teamService.deleteTeam(teamId1);

      verify(teamRepository).existsById(teamId1);
      verify(teamRepository).deleteById(teamId1);
    }

    @Test
    @DisplayName("Should reject deleting non-existent team")
    void shouldRejectDeletingNonExistentTeam() {
      // RED: Test deleting non-existent team
      UUID nonExistentTeamId = UUID.randomUUID();
      when(teamRepository.existsById(nonExistentTeamId)).thenReturn(false);

      assertThatThrownBy(() -> teamService.deleteTeam(nonExistentTeamId))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Equipe non trouvee");

      verify(teamRepository).existsById(nonExistentTeamId);
      verify(teamRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should process multiple player changes in batch")
    void shouldProcessMultiplePlayerChangesInBatch() {
      // RED: Test batch player changes
      Player newPlayer1 =
          Player.builder()
              .id(UUID.randomUUID())
              .username("new1")
              .nickname("New Player 1")
              .region(Player.Region.EU)
              .tranche("1-3")
              .build();
      Player newPlayer2 =
          Player.builder()
              .id(UUID.randomUUID())
              .username("new2")
              .nickname("New Player 2")
              .region(Player.Region.NAW)
              .tranche("4-7")
              .build();
      Player oldPlayer1 = testPlayer1;
      Player oldPlayer2 =
          Player.builder()
              .id(UUID.randomUUID())
              .username("old2")
              .nickname("Old Player 2")
              .region(Player.Region.BR)
              .tranche("8-10")
              .build();

      // Add oldPlayer2 to the team first
      TeamPlayer oldTeamPlayer2 = new TeamPlayer();
      oldTeamPlayer2.setTeam(testTeam1);
      oldTeamPlayer2.setPlayer(oldPlayer2);
      oldTeamPlayer2.setPosition(3);
      testTeam1.getPlayers().add(oldTeamPlayer2);

      Map<UUID, UUID> playerChanges =
          Map.of(
              oldPlayer1.getId(), newPlayer1.getId(),
              oldPlayer2.getId(), newPlayer2.getId());

      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(teamRepository.findByOwnerAndSeason(testUser1, testSeason))
          .thenReturn(Optional.of(testTeam1));
      when(playerRepository.findById(oldPlayer1.getId())).thenReturn(Optional.of(oldPlayer1));
      when(playerRepository.findById(newPlayer1.getId())).thenReturn(Optional.of(newPlayer1));
      when(playerRepository.findById(oldPlayer2.getId())).thenReturn(Optional.of(oldPlayer2));
      when(playerRepository.findById(newPlayer2.getId())).thenReturn(Optional.of(newPlayer2));
      when(teamRepository.findTeamByPlayerAndSeason(newPlayer1.getId(), testSeason))
          .thenReturn(Optional.empty());
      when(teamRepository.findTeamByPlayerAndSeason(newPlayer2.getId(), testSeason))
          .thenReturn(Optional.empty());
      when(teamRepository.save(any(Team.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      TeamDto result = teamService.makePlayerChanges(userId1, playerChanges, testSeason);

      assertThat(result.getId()).isEqualTo(teamId1);
      verify(teamRepository).save(testTeam1);
    }
  }

  @Nested
  @DisplayName("Error Handling and Edge Cases")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle team creation with invalid data")
    void shouldHandleTeamCreationWithInvalidData() {
      // RED: Test invalid team creation data
      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(teamRepository.findByOwnerAndSeason(testUser1, testSeason)).thenReturn(Optional.empty());
      when(teamRepository.save(any(Team.class)))
          .thenThrow(new RuntimeException("Constraint violation"));

      assertThatThrownBy(() -> teamService.createTeam(userId1, "Invalid Team", testSeason))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Constraint violation");
    }
  }
}
