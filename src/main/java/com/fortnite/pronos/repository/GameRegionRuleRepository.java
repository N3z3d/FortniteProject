package com.fortnite.pronos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.domain.port.out.GameRegionRuleRepositoryPort;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.Player;

/** Repository pour la gestion des règles de région des games */
@Repository
public interface GameRegionRuleRepository
    extends JpaRepository<GameRegionRule, UUID>, GameRegionRuleRepositoryPort {

  /** Trouver les règles de région par game */
  List<GameRegionRule> findByGame(Game game);

  /** Trouver les règles de région par région */
  List<GameRegionRule> findByRegion(Player.Region region);

  /** Trouver une règle de région par game et région */
  Optional<GameRegionRule> findByGameAndRegion(Game game, Player.Region region);

  /** Vérifier l'existence d'une règle par game et région */
  boolean existsByGameAndRegion(Game game, Player.Region region);

  /** Trouver les règles avec un nombre maximum de joueurs */
  List<GameRegionRule> findByMaxPlayers(int maxPlayers);

  /** Trouver les règles avec un nombre de joueurs dans une plage donnée */
  List<GameRegionRule> findByMaxPlayersBetween(int minPlayers, int maxPlayers);

  /** Compter les règles par game */
  long countByGame(Game game);

  /** Compter les règles par région */
  long countByRegion(Player.Region region);

  /** Trouver les règles avec un nombre minimum de joueurs */
  List<GameRegionRule> findByMaxPlayersGreaterThanEqual(int minPlayers);

  /** Supprimer les règles d'une game */
  void deleteByGame(Game game);

  /** Trouver les règles par game et nombre de joueurs */
  List<GameRegionRule> findByGameAndMaxPlayers(Game game, int maxPlayers);

  /** Trouver toutes les régions utilisées dans une game */
  @Query("SELECT DISTINCT gr.region FROM GameRegionRule gr WHERE gr.game = :game")
  List<Player.Region> findRegionsByGame(@Param("game") Game game);

  /** Trouver les games par région */
  @Query("SELECT DISTINCT gr.game FROM GameRegionRule gr WHERE gr.region = :region")
  List<Game> findGamesByRegion(@Param("region") Player.Region region);

  /** Trouver les règles avec un nombre de joueurs supérieur à une valeur */
  @Query("SELECT gr FROM GameRegionRule gr WHERE gr.maxPlayers > :minPlayers")
  List<GameRegionRule> findByMaxPlayersGreaterThan(@Param("minPlayers") int minPlayers);

  /** Trouver les règles avec un nombre de joueurs inférieur à une valeur */
  @Query("SELECT gr FROM GameRegionRule gr WHERE gr.maxPlayers < :maxPlayers")
  List<GameRegionRule> findByMaxPlayersLessThan(@Param("maxPlayers") int maxPlayers);

  /** Trouver les règles par game et région avec un nombre de joueurs spécifique */
  @Query(
      "SELECT gr FROM GameRegionRule gr WHERE gr.game = :game AND gr.region = :region AND gr.maxPlayers = :maxPlayers")
  Optional<GameRegionRule> findByGameAndRegionAndMaxPlayers(
      @Param("game") Game game,
      @Param("region") Player.Region region,
      @Param("maxPlayers") int maxPlayers);

  /** Trouver les règles avec le plus grand nombre de joueurs par game */
  @Query(
      "SELECT gr FROM GameRegionRule gr WHERE gr.game = :game AND gr.maxPlayers = (SELECT MAX(gr2.maxPlayers) FROM GameRegionRule gr2 WHERE gr2.game = :game)")
  List<GameRegionRule> findMaxPlayerRulesByGame(@Param("game") Game game);

  /** Trouver les règles avec le plus petit nombre de joueurs par game */
  @Query(
      "SELECT gr FROM GameRegionRule gr WHERE gr.game = :game AND gr.maxPlayers = (SELECT MIN(gr2.maxPlayers) FROM GameRegionRule gr2 WHERE gr2.game = :game)")
  List<GameRegionRule> findMinPlayerRulesByGame(@Param("game") Game game);

  /** Trouver les games qui ont des règles pour toutes les régions spécifiées */
  @Query(
      "SELECT gr.game FROM GameRegionRule gr WHERE gr.region IN :regions GROUP BY gr.game HAVING COUNT(gr.region) = :regionCount")
  List<Game> findGamesWithAllRegions(
      @Param("regions") List<Player.Region> regions, @Param("regionCount") long regionCount);

  /** Trouver les règles par game avec un nombre de joueurs dans une plage */
  @Query(
      "SELECT gr FROM GameRegionRule gr WHERE gr.game = :game AND gr.maxPlayers BETWEEN :minPlayers AND :maxPlayers")
  List<GameRegionRule> findByGameAndMaxPlayersBetween(
      @Param("game") Game game,
      @Param("minPlayers") int minPlayers,
      @Param("maxPlayers") int maxPlayers);

  /** Trouver les règles avec un nombre total de joueurs par game */
  @Query(
      "SELECT gr.game, SUM(gr.maxPlayers) as totalPlayers FROM GameRegionRule gr GROUP BY gr.game")
  List<Object[]> findTotalPlayersByGame();

  /** Trouver les games avec un nombre total de joueurs supérieur à une valeur */
  @Query(
      "SELECT gr.game FROM GameRegionRule gr GROUP BY gr.game HAVING SUM(gr.maxPlayers) > :minTotalPlayers")
  List<Game> findGamesWithTotalPlayersGreaterThan(@Param("minTotalPlayers") int minTotalPlayers);

  /** Trouver les règles par région avec un nombre de joueurs spécifique */
  @Query(
      "SELECT gr FROM GameRegionRule gr WHERE gr.region = :region AND gr.maxPlayers = :maxPlayers")
  List<GameRegionRule> findByRegionAndMaxPlayers(
      @Param("region") Player.Region region, @Param("maxPlayers") int maxPlayers);

  /** Trouver les règles avec le plus grand nombre de joueurs par région */
  @Query(
      "SELECT gr FROM GameRegionRule gr WHERE gr.region = :region AND gr.maxPlayers = (SELECT MAX(gr2.maxPlayers) FROM GameRegionRule gr2 WHERE gr2.region = :region)")
  List<GameRegionRule> findMaxPlayerRulesByRegion(@Param("region") Player.Region region);

  /** Trouver les règles créées récemment */
  @Query("SELECT gr FROM GameRegionRule gr WHERE gr.game.createdAt >= :since")
  List<GameRegionRule> findRecentRules(@Param("since") java.time.LocalDateTime since);

  /** Trouver les règles par créateur de game */
  @Query("SELECT gr FROM GameRegionRule gr WHERE gr.game.creator = :creator")
  List<GameRegionRule> findByGameCreator(@Param("creator") com.fortnite.pronos.model.User creator);

  /** Trouver les règles par statut de game */
  @Query("SELECT gr FROM GameRegionRule gr WHERE gr.game.status = :status")
  List<GameRegionRule> findByGameStatus(
      @Param("status") com.fortnite.pronos.model.GameStatus status);
}
