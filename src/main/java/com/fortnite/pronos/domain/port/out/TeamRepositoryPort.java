package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;

/**
 * Output port for Team persistence operations. Implemented by the persistence adapter
 * (TeamRepository).
 */
public interface TeamRepositoryPort {

  Optional<Team> findById(UUID id);

  Team save(Team team);

  Optional<Team> findByOwnerAndSeason(User owner, int season);

  List<Team> findBySeason(int season);

  List<Team> findIncompleteTeams(int season);

  List<Team> findParticipantTeams(int season);

  Optional<Team> findTeamByPlayerAndSeason(UUID playerId, int season);

  long countTeamsBySeason(int season);

  List<Team> findActiveTeams(int currentSeason);

  Optional<Team> findByOwnerAndPlayerAndSeason(User owner, Player player, Integer season);

  List<Team> findTeamsWithActivePlayer(UUID playerId);

  List<Team> findBySeasonWithFetch(int season);

  Optional<Team> findByIdWithFetch(UUID teamId);

  List<Team> findParticipantTeamsWithFetch(int season);

  List<Team> findByGameIdWithFetch(UUID gameId);
}
