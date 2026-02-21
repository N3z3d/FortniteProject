package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.repository.TeamPlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService - Domain Port Migration Tests")
@SuppressWarnings({"java:S5778"})
class TeamServiceTest {

  @Mock private TeamDomainRepositoryPort teamDomainRepository;
  @Mock private PlayerDomainRepositoryPort playerDomainRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private UserRepositoryPort userRepository;
  @Mock private TeamPlayerRepository teamPlayerRepository;

  @InjectMocks private TeamService teamService;

  private com.fortnite.pronos.model.User testUser;
  private com.fortnite.pronos.model.Player testPlayer;
  private com.fortnite.pronos.model.Player testPlayer2;
  private com.fortnite.pronos.model.Team testTeam;
  private TeamDto testTeamDto;

  @BeforeEach
  void setUp() {
    testUser = new com.fortnite.pronos.model.User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword("password123");
    testUser.setCurrentSeason(2025);

    testPlayer = new com.fortnite.pronos.model.Player();
    testPlayer.setId(UUID.randomUUID());
    testPlayer.setUsername("TestPlayer1");
    testPlayer.setNickname("TP1");
    testPlayer.setRegion(com.fortnite.pronos.model.Player.Region.EU);
    testPlayer.setTranche("S");

    testPlayer2 = new com.fortnite.pronos.model.Player();
    testPlayer2.setId(UUID.randomUUID());
    testPlayer2.setUsername("TestPlayer2");
    testPlayer2.setNickname("TP2");
    testPlayer2.setRegion(com.fortnite.pronos.model.Player.Region.NAW);
    testPlayer2.setTranche("A");

    testTeam = new com.fortnite.pronos.model.Team();
    testTeam.setId(UUID.randomUUID());
    testTeam.setName("Test Team");
    testTeam.setOwner(testUser);
    testTeam.setSeason(2025);

    testTeamDto = new TeamDto();
    testTeamDto.setId(testTeam.getId());
    testTeamDto.setName(testTeam.getName());
    testTeamDto.setUserId(testUser.getId());
    testTeamDto.setSeason(2025);
  }

  @Test
  void shouldCreateTeamForUser() {
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser.getId(), 2025))
        .thenReturn(Optional.empty());
    when(teamDomainRepository.save(any(Team.class))).thenReturn(toDomainTeam(testTeam));

    TeamDto result = teamService.createTeam(testUser.getId(), "New Team", 2025);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(teamDomainRepository).findByOwnerIdAndSeason(testUser.getId(), 2025);
    verify(teamDomainRepository).save(any(Team.class));
  }

  @Test
  void shouldThrowExceptionWhenUserAlreadyHasTeam() {
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser.getId(), 2025))
        .thenReturn(Optional.of(toDomainTeam(testTeam)));

    assertThatThrownBy(() -> teamService.createTeam(testUser.getId(), "New Team", 2025))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("L'utilisateur a deja une equipe pour cette saison");
  }

  @Test
  void shouldAddPlayerToTeam() {
    Team domainTeam = toDomainTeam(testTeam);

    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(playerDomainRepository.findById(testPlayer.getId()))
        .thenReturn(Optional.of(toDomainPlayer(testPlayer)));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser.getId(), 2025))
        .thenReturn(Optional.of(domainTeam));
    when(teamDomainRepository.save(any(Team.class))).thenReturn(domainTeam);

    TeamDto result = teamService.addPlayerToTeam(testUser.getId(), testPlayer.getId(), 1, 2025);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(playerDomainRepository, atLeastOnce()).findById(testPlayer.getId());
    verify(teamDomainRepository).save(any(Team.class));
  }

  @Test
  void shouldThrowExceptionWhenPlayerNotFound() {
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(playerDomainRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> teamService.addPlayerToTeam(testUser.getId(), UUID.randomUUID(), 1, 2025))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Joueur non trouve");
  }

  @Test
  void shouldRemovePlayerFromTeam() {
    testTeam.addPlayer(testPlayer, 1);
    Team domainTeam = toDomainTeam(testTeam);

    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(playerDomainRepository.findById(testPlayer.getId()))
        .thenReturn(Optional.of(toDomainPlayer(testPlayer)));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser.getId(), 2025))
        .thenReturn(Optional.of(domainTeam));
    when(teamDomainRepository.save(any(Team.class))).thenReturn(domainTeam);

    TeamDto result = teamService.removePlayerFromTeam(testUser.getId(), testPlayer.getId(), 2025);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(teamDomainRepository).save(any(Team.class));
  }

  @Test
  void shouldMakePlayerChanges() {
    testTeam.addPlayer(testPlayer, 1);
    Team domainTeam = toDomainTeam(testTeam);
    Map<UUID, UUID> playerChanges = Map.of(testPlayer.getId(), testPlayer2.getId());

    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamDomainRepository.findByOwnerIdAndSeason(testUser.getId(), 2025))
        .thenReturn(Optional.of(domainTeam));
    when(playerDomainRepository.findById(testPlayer.getId()))
        .thenReturn(Optional.of(toDomainPlayer(testPlayer)));
    when(playerDomainRepository.findById(testPlayer2.getId()))
        .thenReturn(Optional.of(toDomainPlayer(testPlayer2)));
    when(teamDomainRepository.findTeamByPlayerAndSeason(testPlayer2.getId(), 2025))
        .thenReturn(Optional.empty());
    when(teamDomainRepository.save(any(Team.class))).thenReturn(domainTeam);

    TeamDto result = teamService.makePlayerChanges(testUser.getId(), playerChanges, 2025);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(teamDomainRepository).save(any(Team.class));
  }

  @Test
  void shouldUpdateTeam() {
    when(teamDomainRepository.findById(testTeam.getId()))
        .thenReturn(Optional.of(toDomainTeam(testTeam)));
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(teamDomainRepository.save(any(Team.class))).thenReturn(toDomainTeam(testTeam));

    TeamDto result = teamService.updateTeam(testTeam.getId(), testTeamDto);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    verify(teamDomainRepository).findById(testTeam.getId());
    verify(teamDomainRepository).save(any(Team.class));
  }

  @Test
  void shouldThrowExceptionWhenTeamNotFoundForUpdateTeam() {
    when(teamDomainRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> teamService.updateTeam(UUID.randomUUID(), testTeamDto))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Equipe non trouvee");
  }

  @Test
  void shouldDeleteTeam() {
    when(teamDomainRepository.findById(testTeam.getId()))
        .thenReturn(Optional.of(toDomainTeam(testTeam)));

    teamService.deleteTeam(testTeam.getId());

    verify(teamRepository).deleteById(testTeam.getId());
  }

  @Test
  void shouldThrowExceptionWhenTeamNotFoundForDeleteTeam() {
    when(teamDomainRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> teamService.deleteTeam(UUID.randomUUID()))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Equipe non trouvee");
  }

  private Team toDomainTeam(com.fortnite.pronos.model.Team team) {
    List<TeamMember> members =
        team.getPlayers() == null
            ? List.of()
            : team.getPlayers().stream()
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
        player.getCurrentSeason() == null ? 2025 : player.getCurrentSeason(),
        player.isLocked());
  }
}
