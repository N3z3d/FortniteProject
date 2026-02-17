package com.fortnite.pronos.domain.port.out;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;

/**
 * Output port for Score persistence operations. Implemented by the persistence adapter
 * (ScoreRepository). Framework-agnostic - no Spring types (Page, Pageable).
 */
public interface ScoreRepositoryPort {

  Optional<Score> findByPlayerAndSeason(Player player, int season);

  List<Score> findByPlayer(Player player);

  List<Score> findBySeason(int season);

  List<Score> findByPlayerIdAndSeason(UUID playerId, int season);

  Integer sumPointsByPlayerAndSeason(UUID playerId, int season);

  Double averagePointsBySeason(int season);

  Integer maxPointsBySeason(int season);

  List<Score> findAboveAverageScores(int season);

  List<Score> findScoresByTeamAndSeason(UUID teamId, int season);

  Integer sumTeamPoints(UUID teamId, int season);

  Optional<Score> findLatestScoreByPlayer(UUID playerId, OffsetDateTime timestamp);

  List<Score> findByPlayerIdAndTimestampBetween(
      UUID playerId, OffsetDateTime start, OffsetDateTime end);

  List<Score> findByPlayerIdOrderByTimestampDesc(UUID playerId);

  Map<UUID, Integer> findAllBySeasonGroupedByPlayer(int season);

  Optional<Score> findTopByPlayerIdAndTimestampLessThanEqualOrderByTimestampDesc(
      UUID playerId, OffsetDateTime timestamp);

  Integer findTotalPointsByTeam(UUID teamId, int season);

  List<Score> findByPlayerAndDateBetween(Player player, LocalDate startDate, LocalDate endDate);
}
