package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.team.model.Team;

/**
 * Output port for Team domain persistence operations. Implemented by the persistence adapter.
 *
 * <p>This port coexists with the legacy {@link TeamRepositoryPort} during migration.
 */
public interface TeamDomainRepositoryPort {

  Optional<Team> findById(UUID id);

  Team save(Team team);

  List<Team> findBySeason(int season);

  List<Team> findBySeasonWithFetch(int season);

  Optional<Team> findByIdWithFetch(UUID teamId);

  Optional<Team> findByOwnerIdAndSeason(UUID ownerId, int season);

  Optional<Team> findTeamByPlayerAndSeason(UUID playerId, int season);

  List<Team> findIncompleteTeams(int season);

  List<Team> findActiveTeams(int currentSeason);

  List<Team> findByGameIdWithFetch(UUID gameId);

  long countBySeason(int season);

  long count();
}
