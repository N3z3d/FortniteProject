package com.fortnite.pronos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.User;

/** Repository pour la gestion des participants des games */
@Repository
public interface GameParticipantRepository
    extends JpaRepository<GameParticipant, UUID>, GameParticipantRepositoryPort {

  /** Trouver les participants par game */
  List<GameParticipant> findByGame(Game game);

  /** Trouver les participants par utilisateur */
  List<GameParticipant> findByUser(User user);

  /** Trouver un participant par game et utilisateur */
  Optional<GameParticipant> findByGameAndUser(Game game, User user);

  /** Vérifier si un utilisateur est déjà participant d'une game */
  boolean existsByGameAndUser(Game game, User user);

  /** Compter le nombre de participants dans une game */
  long countByGame(Game game);

  /** Trouver les participants par ordre de draft */
  List<GameParticipant> findByDraftOrder(int draftOrder);

  /** Trouver les participants par game et ordre de draft */
  List<GameParticipant> findByGameAndDraftOrder(Game game, int draftOrder);

  /** Compter les participants par utilisateur */
  long countByUser(User user);

  /** Trouver le participant avec le plus petit ordre de draft */
  Optional<GameParticipant> findFirstByGameOrderByDraftOrderAsc(Game game);

  /** Trouver le participant avec le plus grand ordre de draft */
  Optional<GameParticipant> findFirstByGameOrderByDraftOrderDesc(Game game);

  /** Trouver les participants ordonnés par ordre de draft */
  List<GameParticipant> findByGameOrderByDraftOrderAsc(Game game);

  /** Trouver les participants avec un ordre de draft dans une plage */
  List<GameParticipant> findByDraftOrderBetween(int minOrder, int maxOrder);

  /** Supprimer les participants d'une game */
  void deleteByGame(Game game);

  /** Trouver les participants avec une sélection récente */
  List<GameParticipant> findByLastSelectionTimeAfter(LocalDateTime time);

  /** Trouver les participants avec une sélection ancienne */
  List<GameParticipant> findByLastSelectionTimeBefore(LocalDateTime time);

  /** Trouver un participant par game et utilisateur avec sélection récente */
  Optional<GameParticipant> findByGameAndUserAndLastSelectionTimeAfter(
      Game game, User user, LocalDateTime time);

  /** Trouver les participants ordonnés par nombre de joueurs sélectionnés */
  @Query(
      "SELECT gp FROM GameParticipant gp WHERE gp.game = :game ORDER BY SIZE(gp.selectedPlayers) DESC")
  List<GameParticipant> findByGameOrderBySelectedPlayersCountDesc(@Param("game") Game game);

  /** Trouver les participants avec un nombre minimum de joueurs sélectionnés */
  @Query(
      "SELECT gp FROM GameParticipant gp WHERE gp.game = :game AND SIZE(gp.selectedPlayers) >= :minCount")
  List<GameParticipant> findByGameAndSelectedPlayersCountGreaterThanEqual(
      @Param("game") Game game, @Param("minCount") int minCount);

  /** Trouver les participants avec un nombre maximum de joueurs sélectionnés */
  @Query(
      "SELECT gp FROM GameParticipant gp WHERE gp.game = :game AND SIZE(gp.selectedPlayers) <= :maxCount")
  List<GameParticipant> findByGameAndSelectedPlayersCountLessThanEqual(
      @Param("game") Game game, @Param("maxCount") int maxCount);

  /** Trouver les participants qui n'ont pas encore sélectionné de joueurs */
  @Query("SELECT gp FROM GameParticipant gp WHERE gp.game = :game AND SIZE(gp.selectedPlayers) = 0")
  List<GameParticipant> findByGameAndNoSelectedPlayers(@Param("game") Game game);

  /** Trouver les participants qui ont sélectionné des joueurs */
  @Query("SELECT gp FROM GameParticipant gp WHERE gp.game = :game AND SIZE(gp.selectedPlayers) > 0")
  List<GameParticipant> findByGameAndHasSelectedPlayers(@Param("game") Game game);

  /** Trouver les participants par game et région de joueurs sélectionnés */
  @Query(
      "SELECT DISTINCT gp FROM GameParticipant gp JOIN gp.selectedPlayers p WHERE gp.game = :game AND p.region = :region")
  List<GameParticipant> findByGameAndSelectedPlayersRegion(
      @Param("game") Game game, @Param("region") String region);

  /** Trouver les participants avec le plus grand nombre de joueurs d'une région */
  @Query(
      "SELECT gp FROM GameParticipant gp WHERE gp.game = :game ORDER BY (SELECT COUNT(p) FROM Player p WHERE p MEMBER OF gp.selectedPlayers AND p.region = :region) DESC")
  List<GameParticipant> findByGameOrderByRegionPlayerCountDesc(
      @Param("game") Game game, @Param("region") String region);

  /** Trouver les participants avec une sélection dans une période donnée */
  @Query(
      "SELECT gp FROM GameParticipant gp WHERE gp.game = :game AND gp.lastSelectionTime BETWEEN :startTime AND :endTime")
  List<GameParticipant> findByGameAndLastSelectionTimeBetween(
      @Param("game") Game game,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  /** Trouver les participants avec le plus grand nombre de joueurs sélectionnés par région */
  @Query(
      "SELECT gp FROM GameParticipant gp WHERE gp.game = :game ORDER BY (SELECT COUNT(p) FROM Player p WHERE p MEMBER OF gp.selectedPlayers) DESC")
  List<GameParticipant> findByGameOrderByTotalSelectedPlayersDesc(@Param("game") Game game);

  /** Trouver les participants qui ont sélectionné un joueur spécifique */
  @Query(
      "SELECT gp FROM GameParticipant gp WHERE gp.game = :game AND :player MEMBER OF gp.selectedPlayers")
  List<GameParticipant> findByGameAndSelectedPlayer(
      @Param("game") Game game, @Param("player") com.fortnite.pronos.model.Player player);

  /** Trouver les participants avec un nombre de joueurs sélectionnés dans une plage */
  @Query(
      "SELECT gp FROM GameParticipant gp WHERE gp.game = :game AND SIZE(gp.selectedPlayers) BETWEEN :minCount AND :maxCount")
  List<GameParticipant> findByGameAndSelectedPlayersCountBetween(
      @Param("game") Game game, @Param("minCount") int minCount, @Param("maxCount") int maxCount);

  /** Trouver les participants qui n'ont pas sélectionné depuis longtemps */
  @Query(
      "SELECT gp FROM GameParticipant gp WHERE gp.game = :game AND gp.lastSelectionTime < :timeout")
  List<GameParticipant> findByGameAndLastSelectionTimeBefore(
      @Param("game") Game game, @Param("timeout") LocalDateTime timeout);

  /** Trouver les participants avec le plus petit nombre de joueurs sélectionnés */
  @Query(
      "SELECT gp FROM GameParticipant gp WHERE gp.game = :game ORDER BY SIZE(gp.selectedPlayers) ASC")
  List<GameParticipant> findByGameOrderBySelectedPlayersCountAsc(@Param("game") Game game);

  /** Trouver les participants par créateur de game */
  @Query("SELECT gp FROM GameParticipant gp WHERE gp.game.creator = :creator")
  List<GameParticipant> findByGameCreator(@Param("creator") User creator);

  /** Trouver les participants par statut de game */
  @Query("SELECT gp FROM GameParticipant gp WHERE gp.game.status = :status")
  List<GameParticipant> findByGameStatus(
      @Param("status") com.fortnite.pronos.model.GameStatus status);

  // ============== MÉTHODES OPTIMISÉES POUR ÉVITER N+1 QUERIES ==============

  /** OPTIMISÉ: Récupère les participants d'un user avec FETCH JOIN sur la game */
  @Query(
      "SELECT DISTINCT gp FROM GameParticipant gp "
          + "JOIN FETCH gp.game g "
          + "JOIN FETCH g.creator "
          + "LEFT JOIN FETCH g.participants "
          + "LEFT JOIN FETCH g.regionRules "
          + "WHERE gp.user = :user "
          + "ORDER BY g.createdAt DESC")
  List<GameParticipant> findByUserWithGameFetch(@Param("user") User user);

  /** OPTIMISÉ: Récupère les participants d'une game avec FETCH JOIN sur l'utilisateur */
  @Query(
      "SELECT DISTINCT gp FROM GameParticipant gp "
          + "JOIN FETCH gp.user u "
          + "WHERE gp.game = :game "
          + "ORDER BY gp.draftOrder")
  List<GameParticipant> findByGameWithUserFetch(@Param("game") Game game);

  // ============== MÉTHODES MANQUANTES POUR PERFORMANCE ==============

  /** Performance methods using IDs to avoid entity loading */
  List<GameParticipant> findByGameIdOrderByJoinedAt(UUID gameId);

  boolean existsByUserIdAndGameId(UUID userId, UUID gameId);

  long countByGameId(UUID gameId);

  Optional<GameParticipant> findByUserIdAndGameId(UUID userId, UUID gameId);

  /** OPTIMISÉ: Trouve les participants d'une game par ID avec utilisateur */
  @Query(
      "SELECT DISTINCT gp FROM GameParticipant gp "
          + "JOIN FETCH gp.user u "
          + "WHERE gp.game.id = :gameId "
          + "ORDER BY gp.joinedAt")
  List<GameParticipant> findByGameIdWithUserFetch(@Param("gameId") UUID gameId);

  /** PERFORMANCE: Vérifie rapidement l'existence d'un participant */
  @Query(
      "SELECT COUNT(gp) > 0 FROM GameParticipant gp WHERE gp.game.id = :gameId AND gp.user.id = :userId")
  boolean existsByGameIdAndUserId(@Param("gameId") UUID gameId, @Param("userId") UUID userId);

  // PHASE 2A: CLEAN ARCHITECTURE - Methods for use cases

  /** Find participants by game ID ordered by draft order */
  @Query(
      "SELECT gp FROM GameParticipant gp "
          + "JOIN FETCH gp.user "
          + "WHERE gp.game.id = :gameId "
          + "ORDER BY gp.draftOrder ASC")
  List<GameParticipant> findByGameIdOrderByDraftOrderAsc(@Param("gameId") UUID gameId);
}
