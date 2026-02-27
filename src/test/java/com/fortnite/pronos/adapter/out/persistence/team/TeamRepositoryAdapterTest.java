package com.fortnite.pronos.adapter.out.persistence.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;

import com.fortnite.pronos.domain.team.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TeamRepositoryAdapterTest {

  @Mock private TeamRepository teamRepository;
  @Mock private UserRepository userRepository;

  private TeamRepositoryAdapter adapter;
  private CrudRepository<com.fortnite.pronos.model.Team, UUID> teamCrudRepository;
  private CrudRepository<User, UUID> userCrudRepository;

  @BeforeEach
  void setUp() {
    adapter = new TeamRepositoryAdapter(teamRepository, userRepository, new TeamEntityMapper());
    teamCrudRepository = teamRepository;
    userCrudRepository = userRepository;
  }

  @Test
  void findByIdReturnsEmptyWhenMissing() {
    UUID id = UUID.randomUUID();
    when(teamCrudRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(adapter.findById(id)).isEmpty();
  }

  @Test
  void findByIdReturnsMappedTeam() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    com.fortnite.pronos.model.Team entity = buildEntity(id, "Team", ownerId, 2025);
    when(teamCrudRepository.findById(id)).thenReturn(Optional.of(entity));

    Optional<Team> result = adapter.findById(id);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getId()).isEqualTo(id);
    assertThat(result.orElseThrow().getName()).isEqualTo("Team");
  }

  @Test
  void saveMapsAndReturns() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    User owner = buildUser(ownerId);
    when(userCrudRepository.findById(ownerId)).thenReturn(Optional.of(owner));

    Team domainTeam = Team.restore(id, "Team", ownerId, 2025, null, 0, null);
    com.fortnite.pronos.model.Team savedEntity = buildEntity(id, "Team", ownerId, 2025);
    when(teamCrudRepository.save(any(com.fortnite.pronos.model.Team.class)))
        .thenReturn(savedEntity);

    Team result = adapter.save(domainTeam);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(id);
    verify(teamCrudRepository).save(any(com.fortnite.pronos.model.Team.class));
  }

  @Test
  void saveRejectsNull() {
    assertThatNullPointerException().isThrownBy(() -> adapter.save(null));
  }

  @Test
  void findBySeasonReturnsMappedList() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    com.fortnite.pronos.model.Team entity = buildEntity(id, "Team", ownerId, 2025);
    when(teamRepository.findBySeason(2025)).thenReturn(List.of(entity));

    List<Team> result = adapter.findBySeason(2025);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSeason()).isEqualTo(2025);
  }

  @Test
  void findBySeasonWithFetchReturnsMappedList() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    com.fortnite.pronos.model.Team entity = buildEntity(id, "Team", ownerId, 2025);
    when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(List.of(entity));

    List<Team> result = adapter.findBySeasonWithFetch(2025);

    assertThat(result).hasSize(1);
  }

  @Test
  void findByIdWithFetchReturnsMapped() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    com.fortnite.pronos.model.Team entity = buildEntity(id, "Team", ownerId, 2025);
    when(teamRepository.findByIdWithFetch(id)).thenReturn(Optional.of(entity));

    assertThat(adapter.findByIdWithFetch(id)).isPresent();
  }

  @Test
  void findByIdWithFetchReturnsEmptyForNull() {
    assertThat(adapter.findByIdWithFetch(null)).isEmpty();
  }

  @Test
  void findByOwnerIdAndSeasonReturnsMapped() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    User owner = buildUser(ownerId);
    com.fortnite.pronos.model.Team entity = buildEntity(id, "Team", ownerId, 2025);
    when(userCrudRepository.findById(ownerId)).thenReturn(Optional.of(owner));
    when(teamRepository.findByOwnerAndSeason(owner, 2025)).thenReturn(Optional.of(entity));

    Optional<Team> result = adapter.findByOwnerIdAndSeason(ownerId, 2025);

    assertThat(result).isPresent();
  }

  @Test
  void findByOwnerIdAndSeasonReturnsEmptyForNullOwner() {
    assertThat(adapter.findByOwnerIdAndSeason(null, 2025)).isEmpty();
  }

  @Test
  void findByOwnerIdAndSeasonReturnsEmptyForUnknownOwner() {
    UUID ownerId = UUID.randomUUID();
    when(userCrudRepository.findById(ownerId)).thenReturn(Optional.empty());

    assertThat(adapter.findByOwnerIdAndSeason(ownerId, 2025)).isEmpty();
  }

  @Test
  void findTeamByPlayerAndSeasonReturnsMapped() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID playerId = UUID.randomUUID();
    com.fortnite.pronos.model.Team entity = buildEntity(id, "Team", ownerId, 2025);
    when(teamRepository.findTeamByPlayerAndSeason(playerId, 2025)).thenReturn(Optional.of(entity));

    assertThat(adapter.findTeamByPlayerAndSeason(playerId, 2025)).isPresent();
  }

  @Test
  void findTeamByPlayerAndSeasonReturnsEmptyForNull() {
    assertThat(adapter.findTeamByPlayerAndSeason(null, 2025)).isEmpty();
  }

  @Test
  void findIncompleteTeamsReturnsMappedList() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    com.fortnite.pronos.model.Team entity = buildEntity(id, "Team", ownerId, 2025);
    when(teamRepository.findIncompleteTeams(2025)).thenReturn(List.of(entity));

    assertThat(adapter.findIncompleteTeams(2025)).hasSize(1);
  }

  @Test
  void findActiveTeamsReturnsMappedList() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    com.fortnite.pronos.model.Team entity = buildEntity(id, "Team", ownerId, 2025);
    when(teamRepository.findActiveTeams(2025)).thenReturn(List.of(entity));

    assertThat(adapter.findActiveTeams(2025)).hasSize(1);
  }

  @Test
  void findByGameIdWithFetchReturnsMappedList() {
    UUID gameId = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    com.fortnite.pronos.model.Team entity = buildEntity(id, "Team", ownerId, 2025);
    when(teamRepository.findByGameIdWithFetch(gameId)).thenReturn(List.of(entity));

    assertThat(adapter.findByGameIdWithFetch(gameId)).hasSize(1);
  }

  @Test
  void findByGameIdWithFetchReturnsEmptyForNull() {
    assertThat(adapter.findByGameIdWithFetch(null)).isEmpty();
  }

  @Test
  void countBySeasonDelegates() {
    when(teamRepository.countBySeason(2025)).thenReturn(10L);
    assertThat(adapter.countBySeason(2025)).isEqualTo(10L);
  }

  @Test
  void countDelegates() {
    when(teamRepository.count()).thenReturn(42L);
    assertThat(adapter.count()).isEqualTo(42L);
  }

  // ===============================
  // HELPERS
  // ===============================

  private com.fortnite.pronos.model.Team buildEntity(
      UUID id, String name, UUID ownerId, int season) {
    com.fortnite.pronos.model.Team entity = new com.fortnite.pronos.model.Team();
    entity.setId(id);
    entity.setName(name);
    entity.setSeason(season);
    entity.setCompletedTradesCount(0);

    User owner = buildUser(ownerId);
    entity.setOwner(owner);

    return entity;
  }

  private User buildUser(UUID id) {
    User user = new User();
    user.setId(id);
    return user;
  }
}
