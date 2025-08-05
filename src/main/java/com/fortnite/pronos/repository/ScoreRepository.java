package com.fortnite.pronos.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Score.ScoreId> {

  Optional<Score> findByPlayerAndSeason(Player player, int season);

  List<Score> findByPlayer(Player player);

  List<Score> findBySeason(int season);

  List<Score> findByPlayerIdAndSeason(UUID playerId, int season);

  @Query("SELECT s FROM Score s WHERE s.season = :season ORDER BY s.points DESC")
  Page<Score> findTopScoresBySeason(@Param("season") int season, Pageable pageable);

  @Query(
      "SELECT s FROM Score s WHERE s.player.region = :region AND s.season = :season ORDER BY s.points DESC")
  Page<Score> findTopScoresByRegionAndSeason(
      @Param("region") Player.Region region, @Param("season") int season, Pageable pageable);

  @Query("SELECT SUM(s.points) FROM Score s WHERE s.player.id = :playerId AND s.season = :season")
  Integer sumPointsByPlayerAndSeason(@Param("playerId") UUID playerId, @Param("season") int season);

  @Query("SELECT AVG(s.points) FROM Score s WHERE s.season = :season")
  Double averagePointsBySeason(@Param("season") int season);

  @Query("SELECT MAX(s.points) FROM Score s WHERE s.season = :season")
  Integer maxPointsBySeason(@Param("season") int season);

  @Query(
      "SELECT s FROM Score s WHERE s.season = :season AND s.points > "
          + "(SELECT AVG(s2.points) FROM Score s2 WHERE s2.season = :season)")
  List<Score> findAboveAverageScores(@Param("season") int season);

  @Query(
      "SELECT s FROM Score s WHERE s.season = :season AND s.player.id IN "
          + "(SELECT tp.player.id FROM TeamPlayer tp WHERE tp.team.id = :teamId AND tp.until IS NULL)")
  List<Score> findScoresByTeamAndSeason(@Param("teamId") UUID teamId, @Param("season") int season);

  @Query(
      "SELECT SUM(s.points) FROM Score s WHERE s.season = :season AND s.player.id IN "
          + "(SELECT tp.player.id FROM TeamPlayer tp WHERE tp.team.id = :teamId AND tp.until IS NULL)")
  Integer sumTeamPoints(@Param("teamId") UUID teamId, @Param("season") int season);

  @Query(
      "SELECT s FROM Score s WHERE s.player.id = :playerId "
          + "AND s.timestamp <= :timestamp "
          + "ORDER BY s.timestamp DESC")
  Optional<Score> findLatestScoreByPlayer(
      @Param("playerId") UUID playerId, @Param("timestamp") OffsetDateTime timestamp);

  @Query(
      "SELECT s FROM Score s WHERE s.player.id = :playerId "
          + "AND s.timestamp BETWEEN :start AND :end "
          + "ORDER BY s.timestamp ASC")
  List<Score> findByPlayerIdAndTimestampBetween(
      @Param("playerId") UUID playerId,
      @Param("start") OffsetDateTime start,
      @Param("end") OffsetDateTime end);

  @Query("SELECT s FROM Score s WHERE s.player.id = :playerId " + "ORDER BY s.timestamp DESC")
  List<Score> findByPlayerIdOrderByTimestampDesc(@Param("playerId") UUID playerId);

  @Query(
      "SELECT s.player.id, SUM(s.points) FROM Score s WHERE s.season = :season GROUP BY s.player.id")
  List<Object[]> findAllBySeasonGroupedByPlayerRaw(@Param("season") int season);

  /**
   * Récupère tous les scores groupés par joueur pour une saison donnée - OPTIMISÉ Évite le problème
   * N+1 queries en récupérant tout en une seule requête
   */
  default Map<UUID, Integer> findAllBySeasonGroupedByPlayer(int season) {
    List<Object[]> results = findAllBySeasonGroupedByPlayerRaw(season);
    return results.stream()
        .collect(
            java.util.stream.Collectors.toMap(
                row -> (UUID) row[0], row -> row[1] != null ? ((Number) row[1]).intValue() : 0));
  }

  Optional<Score> findTopByPlayerIdAndTimestampLessThanEqualOrderByTimestampDesc(
      UUID playerId, OffsetDateTime timestamp);

  /** Calcule le total des points d'une équipe pour une saison */
  @Query(
      "SELECT SUM(s.points) FROM Score s WHERE s.season = :season AND s.player.id IN "
          + "(SELECT tp.player.id FROM TeamPlayer tp WHERE tp.team.id = :teamId AND tp.until IS NULL)")
  Integer findTotalPointsByTeam(@Param("teamId") UUID teamId, @Param("season") int season);

  /** Trouve les scores d'un joueur entre deux dates */
  List<Score> findByPlayerAndDateBetween(Player player, LocalDate startDate, LocalDate endDate);
}
