package com.fortnite.pronos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.domain.port.out.TeamRepositoryPort;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID>, TeamRepositoryPort {
  Optional<Team> findByOwnerAndSeason(User owner, int season);

  List<Team> findBySeason(int season);

  @Query(
      "SELECT t FROM Team t WHERE t.season = :season AND "
          + "(SELECT COUNT(tp) FROM TeamPlayer tp WHERE tp.team = t AND tp.until IS NULL) < 7")
  List<Team> findIncompleteTeams(@Param("season") int season);

  @Query(
      "SELECT t FROM Team t WHERE t.season = :season AND t.owner.role = com.fortnite.pronos.model.User$UserRole.USER")
  List<Team> findParticipantTeams(@Param("season") int season);

  // Méthode obsolète - Marcel n'est plus un rôle spécial
  // @Query("SELECT t FROM Team t WHERE t.season = :season AND t.owner.role = 'MARCEL'")
  // List<Team> findMarcelTeams(@Param("season") int season);

  @Query("SELECT t FROM Team t WHERE t.season = :season AND t.owner.username = 'Marcel'")
  List<Team> findMarcelTeams(@Param("season") int season);

  @Query(
      "SELECT t FROM Team t WHERE t.season = :season AND "
          + "EXISTS (SELECT tp FROM TeamPlayer tp WHERE tp.team = t AND tp.player.id = :playerId AND tp.until IS NULL)")
  Optional<Team> findTeamByPlayerAndSeason(
      @Param("playerId") UUID playerId, @Param("season") int season);

  @Query("SELECT COUNT(t) FROM Team t WHERE t.season = :season")
  long countTeamsBySeason(@Param("season") int season);

  @Query(
      "SELECT t FROM Team t WHERE t.season = :season AND "
          + "(SELECT COUNT(tp2) FROM TeamPlayer tp2 WHERE tp2.team = t) = "
          + "(SELECT COUNT(tp) FROM TeamPlayer tp WHERE tp.team = t AND tp.until IS NULL)")
  List<Team> findTeamsWithNoChanges(@Param("season") int season);

  @Query("SELECT t FROM Team t WHERE t.season >= :currentSeason")
  List<Team> findActiveTeams(@Param("currentSeason") int currentSeason);

  @Query(
      "SELECT t FROM Team t WHERE t.owner = :owner AND t.season = :season AND EXISTS (SELECT tp FROM TeamPlayer tp WHERE tp.team = t AND tp.player = :player AND tp.until IS NULL)")
  Optional<Team> findByOwnerAndPlayerAndSeason(
      @Param("owner") User owner, @Param("player") Player player, @Param("season") Integer season);

  @Query(
      "SELECT DISTINCT t FROM Team t JOIN t.players tp WHERE tp.player.id = :playerId AND tp.until IS NULL")
  List<Team> findTeamsWithActivePlayer(@Param("playerId") UUID playerId);

  /**
   * MÉTHODE OPTIMISÉE - Récupère toutes les équipes avec leurs relations en UNE SEULE requête Évite
   * le problème N+1 queries en utilisant JOIN FETCH
   */
  @Query(
      "SELECT DISTINCT t FROM Team t "
          + "LEFT JOIN FETCH t.players tp "
          + "LEFT JOIN FETCH tp.player p "
          + "LEFT JOIN FETCH t.owner o "
          + "WHERE t.season = :season "
          + "ORDER BY t.name")
  List<Team> findBySeasonWithFetch(@Param("season") int season);

  /**
   * MÉTHODE OPTIMISÉE - Récupère une équipe par ID avec toutes ses relations Évite le problème N+1
   * queries en utilisant JOIN FETCH
   */
  @Query(
      "SELECT DISTINCT t FROM Team t "
          + "LEFT JOIN FETCH t.players tp "
          + "LEFT JOIN FETCH tp.player p "
          + "LEFT JOIN FETCH t.owner o "
          + "WHERE t.id = :teamId")
  Optional<Team> findByIdWithFetch(@Param("teamId") UUID teamId);

  /** Compte le nombre d'équipes dans une saison */
  long countBySeason(int season);

  /** OPTIMISÉ: Récupère les équipes participantes avec FETCH JOIN */
  @Query(
      "SELECT DISTINCT t FROM Team t "
          + "LEFT JOIN FETCH t.players tp "
          + "LEFT JOIN FETCH tp.player p "
          + "LEFT JOIN FETCH t.owner o "
          + "WHERE t.season = :season AND t.owner.role = com.fortnite.pronos.model.User$UserRole.USER "
          + "ORDER BY t.name")
  List<Team> findParticipantTeamsWithFetch(@Param("season") int season);

  /** OPTIMISÉ: Récupère les équipes d'un jeu spécifique avec FETCH JOIN */
  @Query(
      "SELECT DISTINCT t FROM Team t "
          + "LEFT JOIN FETCH t.players tp "
          + "LEFT JOIN FETCH tp.player p "
          + "LEFT JOIN FETCH t.owner o "
          + "WHERE t.game.id = :gameId "
          + "ORDER BY t.name")
  List<Team> findByGameIdWithFetch(@Param("gameId") UUID gameId);
}
