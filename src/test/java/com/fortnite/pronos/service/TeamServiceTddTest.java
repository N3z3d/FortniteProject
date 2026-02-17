package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.TeamDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.domain.team.model.Team;
import com.fortnite.pronos.domain.team.model.TeamMember;
import com.fortnite.pronos.dto.SwapPlayersRequest;
import com.fortnite.pronos.dto.SwapPlayersResponse;
import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.exception.InvalidSwapException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.repository.TeamPlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService - TDD Domain Migration")
class TeamServiceTddTest {

  @Mock private TeamDomainRepositoryPort teamDomainRepository;
  @Mock private PlayerDomainRepositoryPort playerDomainRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private UserRepositoryPort userRepository;
  @Mock private TeamPlayerRepository teamPlayerRepository;

  @InjectMocks private TeamService teamService;

  private com.fortnite.pronos.model.User testUser1;
  private com.fortnite.pronos.model.User testUser2;
  private com.fortnite.pronos.model.Team testTeam1;
  private com.fortnite.pronos.model.Team testTeam2;
  private com.fortnite.pronos.model.Player testPlayer1;
  private com.fortnite.pronos.model.Player testPlayer2;
  private TeamPlayer teamPlayer1;
  private TeamPlayer teamPlayer2;
  private int season;

  @BeforeEach
  void setUp() {
    season = 2025;

    testUser1 = new com.fortnite.pronos.model.User();
    testUser1.setId(UUID.randomUUID());
    testUser1.setUsername("owner_one");
    testUser1.setCurrentSeason(season);

    testUser2 = new com.fortnite.pronos.model.User();
    testUser2.setId(UUID.randomUUID());
    testUser2.setUsername("owner_two");
    testUser2.setCurrentSeason(season);

    testPlayer1 =
        com.fortnite.pronos.model.Player.builder()
            .id(UUID.randomUUID())
            .username("player_one")
            .nickname("Player One")
            .region(com.fortnite.pronos.model.Player.Region.EU)
            .tranche("1-3")
            .currentSeason(season)
            .build();

    testPlayer2 =
        com.fortnite.pronos.model.Player.builder()
            .id(UUID.randomUUID())
            .username("player_two")
            .nickname("Player Two")
            .region(com.fortnite.pronos.model.Player.Region.NAW)
            .tranche("4-7")
            .currentSeason(season)
            .build();

    testTeam1 = new com.fortnite.pronos.model.Team();
    testTeam1.setId(UUID.randomUUID());
    testTeam1.setName("Team One");
    testTeam1.setOwner(testUser1);
    testTeam1.setSeason(season);
    testTeam1.setPlayers(new ArrayList<>());

    testTeam2 = new com.fortnite.pronos.model.Team();
    testTeam2.setId(UUID.randomUUID());
    testTeam2.setName("Team Two");
    testTeam2.setOwner(testUser2);
    testTeam2.setSeason(season);
    testTeam2.setPlayers(new ArrayList<>());

    teamPlayer1 = new TeamPlayer();
    teamPlayer1.setTeam(testTeam1);
    teamPlayer1.setPlayer(testPlayer1);
    teamPlayer1.setPosition(1);
    testTeam1.getPlayers().add(teamPlayer1);

    teamPlayer2 = new TeamPlayer();
    teamPlayer2.setTeam(testTeam2);
    teamPlayer2.setPlayer(testPlayer2);
    teamPlayer2.setPosition(1);
    testTeam2.getPlayers().add(teamPlayer2);
  }

  @Test
  void shouldCreateTeamSuccessfullyForValidUser() {
    when(userRepository.findById(testUser1.getId())).thenReturn(Optional.of(testUser1));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser1.getId(), season))
        .thenReturn(Optional.empty());
    when(teamDomainRepository.save(any(Team.class))).thenReturn(toDomainTeam(testTeam1));
    when(teamRepository.findByIdWithFetch(testTeam1.getId())).thenReturn(Optional.of(testTeam1));

    TeamDto result = teamService.createTeam(testUser1.getId(), "New Team", season);

    assertThat(result.getId()).isEqualTo(testTeam1.getId());
    verify(teamDomainRepository).save(any(Team.class));
  }

  @Test
  void shouldRejectDuplicateTeamCreationForSameUserAndSeason() {
    when(userRepository.findById(testUser1.getId())).thenReturn(Optional.of(testUser1));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser1.getId(), season))
        .thenReturn(Optional.of(toDomainTeam(testTeam1)));

    assertThatThrownBy(() -> teamService.createTeam(testUser1.getId(), "Duplicate", season))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("deja une equipe");
  }

  @Test
  void shouldAddPlayerToTeamSuccessfully() {
    Team emptyDomainTeam =
        Team.restore(
            testTeam1.getId(), testTeam1.getName(), testUser1.getId(), season, null, 0, List.of());

    when(userRepository.findById(testUser1.getId())).thenReturn(Optional.of(testUser1));
    when(playerDomainRepository.findById(testPlayer1.getId()))
        .thenReturn(Optional.of(toDomainPlayer(testPlayer1)));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser1.getId(), season))
        .thenReturn(Optional.of(emptyDomainTeam));
    when(teamDomainRepository.save(any(Team.class))).thenReturn(toDomainTeam(testTeam1));
    when(teamRepository.findByIdWithFetch(testTeam1.getId())).thenReturn(Optional.of(testTeam1));

    TeamDto result = teamService.addPlayerToTeam(testUser1.getId(), testPlayer1.getId(), 1, season);

    assertThat(result.getId()).isEqualTo(testTeam1.getId());
    verify(teamDomainRepository).save(any(Team.class));
  }

  @Test
  void shouldRejectAddingDuplicatePlayerToTeam() {
    when(userRepository.findById(testUser1.getId())).thenReturn(Optional.of(testUser1));
    when(playerDomainRepository.findById(testPlayer1.getId()))
        .thenReturn(Optional.of(toDomainPlayer(testPlayer1)));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser1.getId(), season))
        .thenReturn(Optional.of(toDomainTeam(testTeam1)));

    assertThatThrownBy(
            () -> teamService.addPlayerToTeam(testUser1.getId(), testPlayer1.getId(), 2, season))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("deja dans l'equipe");
    verify(teamDomainRepository, never()).save(any(Team.class));
  }

  @Test
  void shouldRemovePlayerFromTeamSuccessfully() {
    when(userRepository.findById(testUser1.getId())).thenReturn(Optional.of(testUser1));
    when(playerDomainRepository.findById(testPlayer1.getId()))
        .thenReturn(Optional.of(toDomainPlayer(testPlayer1)));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser1.getId(), season))
        .thenReturn(Optional.of(toDomainTeam(testTeam1)));
    when(teamDomainRepository.save(any(Team.class))).thenReturn(toDomainTeam(testTeam1));
    when(teamRepository.findByIdWithFetch(testTeam1.getId())).thenReturn(Optional.of(testTeam1));

    TeamDto result =
        teamService.removePlayerFromTeam(testUser1.getId(), testPlayer1.getId(), season);

    assertThat(result.getId()).isEqualTo(testTeam1.getId());
    verify(teamDomainRepository).save(any(Team.class));
  }

  @Test
  void shouldRejectRemovingNonExistentPlayerFromTeam() {
    com.fortnite.pronos.model.Player outsider =
        com.fortnite.pronos.model.Player.builder()
            .id(UUID.randomUUID())
            .username("outsider")
            .nickname("Outsider")
            .region(com.fortnite.pronos.model.Player.Region.BR)
            .tranche("9-10")
            .build();

    when(userRepository.findById(testUser1.getId())).thenReturn(Optional.of(testUser1));
    when(playerDomainRepository.findById(outsider.getId()))
        .thenReturn(Optional.of(toDomainPlayer(outsider)));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser1.getId(), season))
        .thenReturn(Optional.of(toDomainTeam(testTeam1)));

    assertThatThrownBy(
            () -> teamService.removePlayerFromTeam(testUser1.getId(), outsider.getId(), season))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("n'est pas dans l'equipe");
  }

  @Test
  void shouldProcessMultiplePlayerChangesInBatch() {
    com.fortnite.pronos.model.Player newPlayer =
        com.fortnite.pronos.model.Player.builder()
            .id(UUID.randomUUID())
            .username("new_player")
            .nickname("New Player")
            .region(com.fortnite.pronos.model.Player.Region.BR)
            .tranche("4-7")
            .build();
    Map<UUID, UUID> changes = Map.of(testPlayer1.getId(), newPlayer.getId());

    when(userRepository.findById(testUser1.getId())).thenReturn(Optional.of(testUser1));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser1.getId(), season))
        .thenReturn(Optional.of(toDomainTeam(testTeam1)));
    when(playerDomainRepository.findById(testPlayer1.getId()))
        .thenReturn(Optional.of(toDomainPlayer(testPlayer1)));
    when(playerDomainRepository.findById(newPlayer.getId()))
        .thenReturn(Optional.of(toDomainPlayer(newPlayer)));
    when(teamDomainRepository.findTeamByPlayerAndSeason(newPlayer.getId(), season))
        .thenReturn(Optional.empty());
    when(teamDomainRepository.save(any(Team.class))).thenReturn(toDomainTeam(testTeam1));
    when(teamRepository.findByIdWithFetch(testTeam1.getId())).thenReturn(Optional.of(testTeam1));

    TeamDto result = teamService.makePlayerChanges(testUser1.getId(), changes, season);

    assertThat(result.getId()).isEqualTo(testTeam1.getId());
    verify(teamDomainRepository).save(any(Team.class));
  }

  @Test
  void shouldExecutePlayerSwapSuccessfullyBetweenTeams() {
    SwapPlayersRequest request =
        new SwapPlayersRequest(
            testTeam1.getId(), testTeam2.getId(), testPlayer1.getId(), testPlayer2.getId());

    when(userRepository.findById(testUser1.getId())).thenReturn(Optional.of(testUser1));
    when(teamDomainRepository.findById(testTeam1.getId()))
        .thenReturn(Optional.of(toDomainTeam(testTeam1)));
    when(teamDomainRepository.findById(testTeam2.getId()))
        .thenReturn(Optional.of(toDomainTeam(testTeam2)));
    when(playerDomainRepository.findById(testPlayer1.getId()))
        .thenReturn(Optional.of(toDomainPlayer(testPlayer1)));
    when(playerDomainRepository.findById(testPlayer2.getId()))
        .thenReturn(Optional.of(toDomainPlayer(testPlayer2)));
    when(teamPlayerRepository.findByTeamAndPlayer(
            any(com.fortnite.pronos.model.Team.class), any(com.fortnite.pronos.model.Player.class)))
        .thenReturn(Optional.of(teamPlayer1), Optional.of(teamPlayer2));

    SwapPlayersResponse result = teamService.swapPlayers(testUser1.getId(), request);

    assertThat(result.isSuccess()).isTrue();
    verify(teamPlayerRepository).save(teamPlayer1);
    verify(teamPlayerRepository).save(teamPlayer2);
  }

  @Test
  void shouldRejectSwapWhenUserDoesNotOwnEitherTeam() {
    com.fortnite.pronos.model.User outsider = new com.fortnite.pronos.model.User();
    outsider.setId(UUID.randomUUID());
    outsider.setUsername("outsider");

    SwapPlayersRequest request =
        new SwapPlayersRequest(
            testTeam1.getId(), testTeam2.getId(), testPlayer1.getId(), testPlayer2.getId());

    when(userRepository.findById(outsider.getId())).thenReturn(Optional.of(outsider));
    when(teamDomainRepository.findById(testTeam1.getId()))
        .thenReturn(Optional.of(toDomainTeam(testTeam1)));
    when(teamDomainRepository.findById(testTeam2.getId()))
        .thenReturn(Optional.of(toDomainTeam(testTeam2)));

    assertThatThrownBy(() -> teamService.swapPlayers(outsider.getId(), request))
        .isInstanceOf(UnauthorizedAccessException.class)
        .hasMessageContaining("proprietaire d'au moins une des equipes");
  }

  @Test
  void shouldRejectSwapBetweenTeamsFromDifferentSeasons() {
    com.fortnite.pronos.model.Team otherSeasonTeam = new com.fortnite.pronos.model.Team();
    otherSeasonTeam.setId(testTeam2.getId());
    otherSeasonTeam.setName("Other Season");
    otherSeasonTeam.setOwner(testUser2);
    otherSeasonTeam.setSeason(2024);

    SwapPlayersRequest request =
        new SwapPlayersRequest(
            testTeam1.getId(), testTeam2.getId(), testPlayer1.getId(), testPlayer2.getId());

    when(userRepository.findById(testUser1.getId())).thenReturn(Optional.of(testUser1));
    when(teamDomainRepository.findById(testTeam1.getId()))
        .thenReturn(Optional.of(toDomainTeam(testTeam1)));
    when(teamDomainRepository.findById(testTeam2.getId()))
        .thenReturn(Optional.of(toDomainTeam(otherSeasonTeam)));

    assertThatThrownBy(() -> teamService.swapPlayers(testUser1.getId(), request))
        .isInstanceOf(InvalidSwapException.class)
        .hasMessageContaining("meme saison");
  }

  @Test
  void shouldRejectSwapWhenPlayerIsNotInSpecifiedTeam() {
    SwapPlayersRequest request =
        new SwapPlayersRequest(
            testTeam1.getId(), testTeam2.getId(), testPlayer1.getId(), testPlayer2.getId());

    when(userRepository.findById(testUser1.getId())).thenReturn(Optional.of(testUser1));
    when(teamDomainRepository.findById(testTeam1.getId()))
        .thenReturn(Optional.of(toDomainTeam(testTeam1)));
    when(teamDomainRepository.findById(testTeam2.getId()))
        .thenReturn(Optional.of(toDomainTeam(testTeam2)));
    when(playerDomainRepository.findById(testPlayer1.getId()))
        .thenReturn(Optional.of(toDomainPlayer(testPlayer1)));
    when(playerDomainRepository.findById(testPlayer2.getId()))
        .thenReturn(Optional.of(toDomainPlayer(testPlayer2)));
    when(teamPlayerRepository.findByTeamAndPlayer(
            any(com.fortnite.pronos.model.Team.class), any(com.fortnite.pronos.model.Player.class)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> teamService.swapPlayers(testUser1.getId(), request))
        .isInstanceOf(InvalidSwapException.class)
        .hasMessageContaining("n'est pas dans l'equipe");
  }

  @Test
  void shouldRejectTeamCreationForUnknownUser() {
    UUID unknownUserId = UUID.randomUUID();
    when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> teamService.createTeam(unknownUserId, "Team", season))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Utilisateur non trouve");
  }

  private Team toDomainTeam(com.fortnite.pronos.model.Team team) {
    List<TeamMember> members =
        team.getPlayers().stream()
            .filter(tp -> tp.getPlayer() != null)
            .map(
                tp ->
                    TeamMember.restore(
                        tp.getPlayer().getId(),
                        tp.getPosition() == null ? 1 : tp.getPosition(),
                        tp.getUntil()))
            .toList();

    return Team.restore(
        team.getId(),
        team.getName(),
        team.getOwner().getId(),
        team.getSeason(),
        team.getGame() != null ? team.getGame().getId() : null,
        team.getCompletedTradesCount() == null ? 0 : team.getCompletedTradesCount(),
        members);
  }

  private Player toDomainPlayer(com.fortnite.pronos.model.Player player) {
    return Player.restore(
        player.getId(),
        player.getFortniteId(),
        player.getUsername(),
        player.getNickname(),
        player.getRegion() == null ? null : PlayerRegion.valueOf(player.getRegion().name()),
        player.getTranche(),
        player.getCurrentSeason() == null ? season : player.getCurrentSeason(),
        player.isLocked());
  }
}
