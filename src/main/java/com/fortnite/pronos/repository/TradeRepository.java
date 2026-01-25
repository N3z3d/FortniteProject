package com.fortnite.pronos.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.domain.port.out.TradeRepositoryPort;
import com.fortnite.pronos.model.Trade;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID>, TradeRepositoryPort {

  /**
   * Find all trades involving a specific team (as either from or to team)
   *
   * @param teamId The team ID
   * @return List of trades involving the team
   */
  @Query(
      "SELECT t FROM Trade t WHERE t.fromTeam.id = :teamId OR t.toTeam.id = :teamId ORDER BY t.proposedAt DESC")
  List<Trade> findByTeamId(@Param("teamId") UUID teamId);

  /**
   * Find all pending trades for a specific team (where the team is the recipient)
   *
   * @param teamId The team ID
   * @return List of pending trades for the team
   */
  @Query(
      "SELECT t FROM Trade t WHERE t.toTeam.id = :teamId AND t.status = 'PENDING' ORDER BY t.proposedAt ASC")
  List<Trade> findPendingTradesForTeam(@Param("teamId") UUID teamId);

  /**
   * Count trades by game ID and status
   *
   * @param gameId The game ID
   * @param status The trade status
   * @return Count of trades with the specified status in the game
   */
  @Query("SELECT COUNT(t) FROM Trade t WHERE t.fromTeam.game.id = :gameId AND t.status = :status")
  Long countByGameIdAndStatus(@Param("gameId") UUID gameId, @Param("status") Trade.Status status);

  /**
   * Find all trades in a specific game
   *
   * @param gameId The game ID
   * @return List of all trades in the game
   */
  @Query(
      "SELECT DISTINCT t FROM Trade t "
          + "LEFT JOIN FETCH t.fromTeam ft "
          + "LEFT JOIN FETCH t.toTeam tt "
          + "WHERE ft.game.id = :gameId OR tt.game.id = :gameId "
          + "ORDER BY t.proposedAt DESC")
  List<Trade> findByGameId(@Param("gameId") UUID gameId);

  /**
   * Find all trades in a specific game with a specific status
   *
   * @param gameId The game ID
   * @param status The trade status
   * @return List of trades with the specified status in the game
   */
  @Query(
      "SELECT DISTINCT t FROM Trade t "
          + "LEFT JOIN FETCH t.fromTeam ft "
          + "LEFT JOIN FETCH t.toTeam tt "
          + "WHERE (ft.game.id = :gameId OR tt.game.id = :gameId) "
          + "AND t.status = :status "
          + "ORDER BY t.proposedAt DESC")
  List<Trade> findByGameIdAndStatus(
      @Param("gameId") UUID gameId, @Param("status") Trade.Status status);

  /**
   * Find all trades between two specific teams
   *
   * @param teamId1 First team ID
   * @param teamId2 Second team ID
   * @return List of trades between the teams
   */
  @Query(
      "SELECT t FROM Trade t WHERE (t.fromTeam.id = :teamId1 AND t.toTeam.id = :teamId2) OR (t.fromTeam.id = :teamId2 AND t.toTeam.id = :teamId1) ORDER BY t.proposedAt DESC")
  List<Trade> findTradesBetweenTeams(
      @Param("teamId1") UUID teamId1, @Param("teamId2") UUID teamId2);
}
