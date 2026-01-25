package com.fortnite.pronos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.domain.port.out.TeamPlayerRepositoryPort;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;

/** Repository pour gérer les relations entre équipes et joueurs */
@Repository
public interface TeamPlayerRepository
    extends JpaRepository<TeamPlayer, TeamPlayer.TeamPlayerId>, TeamPlayerRepositoryPort {

  /** Trouve une relation TeamPlayer par équipe et joueur */
  Optional<TeamPlayer> findByTeamAndPlayer(Team team, Player player);

  /** Trouve tous les joueurs d'une équipe */
  List<TeamPlayer> findByTeam(Team team);

  /** Trouve toutes les équipes d'un joueur */
  List<TeamPlayer> findByPlayer(Player player);

  /** Trouve les joueurs actifs d'une équipe (until = null) */
  List<TeamPlayer> findByTeamAndUntilIsNull(Team team);

  /** Trouve les joueurs d'une équipe ordonnés par position */
  @Query("SELECT tp FROM TeamPlayer tp WHERE tp.team = :team ORDER BY tp.position ASC")
  List<TeamPlayer> findByTeamOrderByPosition(@Param("team") Team team);

  /** Compte le nombre de joueurs dans une équipe */
  long countByTeam(Team team);

  /** Supprime toutes les relations d'une équipe */
  void deleteByTeam(Team team);

  /** Supprime toutes les relations d'un joueur */
  void deleteByPlayer(Player player);

  /** Compte les joueurs actifs dans une saison donnée */
  @Query(
      "SELECT COUNT(DISTINCT tp.player.id) FROM TeamPlayer tp "
          + "JOIN tp.team t WHERE t.season = :season AND tp.until IS NULL")
  long countActivePlayersInSeason(@Param("season") Integer season);

  /** Vérifie si un joueur appartient à une équipe */
  @Query(
      "SELECT CASE WHEN COUNT(tp) > 0 THEN true ELSE false END FROM TeamPlayer tp "
          + "WHERE tp.team.id = :teamId AND tp.player.id = :playerId AND tp.until IS NULL")
  boolean existsByTeamIdAndPlayerId(@Param("teamId") UUID teamId, @Param("playerId") UUID playerId);

  /** Supprime une relation équipe-joueur par ID */
  @Modifying
  @Query("DELETE FROM TeamPlayer tp WHERE tp.team.id = :teamId AND tp.player.id = :playerId")
  void deleteByTeamIdAndPlayerId(@Param("teamId") UUID teamId, @Param("playerId") UUID playerId);

  /** Compte les joueurs d'une équipe par région */
  @Query(
      "SELECT COUNT(tp) FROM TeamPlayer tp WHERE tp.team = :team AND tp.player.region = :region AND tp.until IS NULL")
  long countByTeamAndPlayerRegion(@Param("team") Team team, @Param("region") Player.Region region);
}
