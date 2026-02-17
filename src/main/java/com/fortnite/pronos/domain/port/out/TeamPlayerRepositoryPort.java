package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;

/**
 * Output port for TeamPlayer persistence operations. Implemented by the persistence adapter
 * (TeamPlayerRepository).
 */
public interface TeamPlayerRepositoryPort {

  Optional<TeamPlayer> findByTeamAndPlayer(Team team, Player player);

  List<TeamPlayer> findByTeam(Team team);

  List<TeamPlayer> findByPlayer(Player player);

  List<TeamPlayer> findByTeamAndUntilIsNull(Team team);

  List<TeamPlayer> findByTeamOrderByPosition(Team team);

  long countByTeam(Team team);

  long countActivePlayersInSeason(Integer season);

  boolean existsByTeamIdAndPlayerId(UUID teamId, UUID playerId);

  long countByTeamAndPlayerRegion(Team team, Player.Region region);
}
