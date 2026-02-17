package com.fortnite.pronos.adapter.out.persistence.team;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.port.out.TeamDomainRepositoryPort;
import com.fortnite.pronos.domain.team.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

/**
 * Persistence adapter for the Team domain migration. Implements a dedicated domain port without
 * modifying the legacy {@code TeamRepositoryPort}.
 */
@Component
public class TeamRepositoryAdapter implements TeamDomainRepositoryPort {

  private final TeamRepository teamRepository;
  private final UserRepository userRepository;
  private final TeamEntityMapper mapper;

  public TeamRepositoryAdapter(
      TeamRepository teamRepository, UserRepository userRepository, TeamEntityMapper mapper) {
    this.teamRepository = teamRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Team> findById(UUID id) {
    return teamCrudRepository().findById(id).map(mapper::toDomain);
  }

  @Override
  public Team save(Team team) {
    Objects.requireNonNull(team, "Team cannot be null");
    User owner = findRequiredOwner(team.getOwnerId());
    com.fortnite.pronos.model.Team entity = mapper.toEntity(team, owner);
    com.fortnite.pronos.model.Team savedEntity = teamCrudRepository().save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public List<Team> findBySeason(int season) {
    return mapper.toDomainList(teamRepository.findBySeason(season));
  }

  @Override
  public List<Team> findBySeasonWithFetch(int season) {
    return mapper.toDomainList(teamRepository.findBySeasonWithFetch(season));
  }

  @Override
  public Optional<Team> findByIdWithFetch(UUID teamId) {
    if (teamId == null) {
      return Optional.empty();
    }
    return teamRepository.findByIdWithFetch(teamId).map(mapper::toDomain);
  }

  @Override
  public Optional<Team> findByOwnerIdAndSeason(UUID ownerId, int season) {
    if (ownerId == null) {
      return Optional.empty();
    }
    Optional<User> owner = userCrudRepository().findById(ownerId);
    if (owner.isEmpty()) {
      return Optional.empty();
    }
    return teamRepository.findByOwnerAndSeason(owner.orElseThrow(), season).map(mapper::toDomain);
  }

  @Override
  public Optional<Team> findTeamByPlayerAndSeason(UUID playerId, int season) {
    if (playerId == null) {
      return Optional.empty();
    }
    return teamRepository.findTeamByPlayerAndSeason(playerId, season).map(mapper::toDomain);
  }

  @Override
  public List<Team> findIncompleteTeams(int season) {
    return mapper.toDomainList(teamRepository.findIncompleteTeams(season));
  }

  @Override
  public List<Team> findActiveTeams(int currentSeason) {
    return mapper.toDomainList(teamRepository.findActiveTeams(currentSeason));
  }

  @Override
  public List<Team> findByGameIdWithFetch(UUID gameId) {
    if (gameId == null) {
      return Collections.emptyList();
    }
    return mapper.toDomainList(teamRepository.findByGameIdWithFetch(gameId));
  }

  @Override
  public long countBySeason(int season) {
    return teamRepository.countBySeason(season);
  }

  @Override
  public long count() {
    return teamRepository.count();
  }

  // ===============================
  // PRIVATE HELPERS
  // ===============================

  private User findRequiredOwner(UUID ownerId) {
    if (ownerId == null) {
      throw new IllegalArgumentException("Owner not found: null");
    }
    return userCrudRepository()
        .findById(ownerId)
        .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + ownerId));
  }

  private CrudRepository<com.fortnite.pronos.model.Team, UUID> teamCrudRepository() {
    return teamRepository;
  }

  private CrudRepository<User, UUID> userCrudRepository() {
    return userRepository;
  }
}
