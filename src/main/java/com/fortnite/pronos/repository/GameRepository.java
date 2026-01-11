package com.fortnite.pronos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;

/** Repository pour la gestion des games */
@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {

  /** Trouver les games par créateur */
  List<Game> findByCreator(User creator);

  /** Trouver les games par statut */
  List<Game> findByStatus(GameStatus status);

  /** Trouver les games actives */
  @Query("SELECT g FROM Game g WHERE g.status IN ('DRAFTING', 'ACTIVE')")
  List<Game> findActiveGames();

  /** Compter les games par statut */
  long countByStatus(GameStatus status);

  /** Vérifier si une game existe avec un nom spécifique pour un créateur */
  boolean existsByNameAndCreator(String name, User creator);

  /** Trouver les games créées après une date */
  List<Game> findByCreatedAtAfter(LocalDateTime date);

  /** Trouver les games avec des places disponibles */
  @Query("SELECT g FROM Game g WHERE SIZE(g.participants) < g.maxParticipants")
  List<Game> findGamesWithAvailableSlots();

  /** Trouver une game par son code d'invitation */
  Optional<Game> findByInvitationCode(String invitationCode);

  /** Vérifier si un code d'invitation existe */
  boolean existsByInvitationCode(String invitationCode);

  /** Trouver les games d'une saison spécifique */
  @Query("SELECT g FROM Game g WHERE g.createdAt >= :seasonStart " + "AND g.createdAt < :seasonEnd")
  List<Game> findGamesBySeason(
      @Param("seasonStart") LocalDateTime seasonStart, @Param("seasonEnd") LocalDateTime seasonEnd);

  /** Trouver les games avec un nom contenant une chaîne */
  List<Game> findByNameContainingIgnoreCase(String namePattern);

  /** Trouver les games créées entre deux dates */
  List<Game> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

  // ============== MÉTHODES OPTIMISÉES POUR ÉVITER N+1 QUERIES ==============

  /** OPTIMISÉ: Récupère toutes les games avec FETCH JOIN pour éviter N+1 */
  @Query(
      "SELECT DISTINCT g FROM Game g "
          + "LEFT JOIN FETCH g.participants p "
          + "LEFT JOIN FETCH p.user "
          + "LEFT JOIN FETCH g.creator "
          + "LEFT JOIN FETCH g.regionRules "
          + "ORDER BY g.createdAt DESC")
  List<Game> findAllWithFetch();

  /** OPTIMISÉ: Récupère les games d'un créateur avec FETCH JOIN */
  @Query(
      "SELECT DISTINCT g FROM Game g "
          + "LEFT JOIN FETCH g.participants p "
          + "LEFT JOIN FETCH p.user "
          + "LEFT JOIN FETCH g.regionRules "
          + "WHERE g.creator = :creator "
          + "ORDER BY g.createdAt DESC")
  List<Game> findByCreatorWithFetch(@Param("creator") User creator);

  /** OPTIMISÉ: Récupère une game par ID avec toutes les relations */
  @Query(
      "SELECT DISTINCT g FROM Game g "
          + "LEFT JOIN FETCH g.participants p "
          + "LEFT JOIN FETCH p.user "
          + "LEFT JOIN FETCH g.creator "
          + "LEFT JOIN FETCH g.regionRules "
          + "WHERE g.id = :gameId")
  Optional<Game> findByIdWithFetch(@Param("gameId") UUID gameId);

  /** OPTIMISÉ: Trouve les games d'un utilisateur (créateur ou participant) */
  @Query(
      "SELECT DISTINCT g FROM Game g "
          + "LEFT JOIN FETCH g.participants p "
          + "LEFT JOIN FETCH p.user "
          + "LEFT JOIN FETCH g.creator "
          + "WHERE g.creator.id = :userId OR p.user.id = :userId "
          + "ORDER BY g.createdAt DESC")
  List<Game> findGamesByUserId(@Param("userId") UUID userId);

  /** OPTIMISÉ: Trouve les games par statut avec fetch */
  @Query(
      "SELECT DISTINCT g FROM Game g "
          + "LEFT JOIN FETCH g.participants p "
          + "LEFT JOIN FETCH p.user "
          + "LEFT JOIN FETCH g.creator "
          + "WHERE g.status = :status "
          + "ORDER BY g.createdAt DESC")
  List<Game> findByStatusWithFetch(@Param("status") GameStatus status);

  /** OPTIMISÉ: Trouve les games par créateur ID */
  @Query(
      "SELECT DISTINCT g FROM Game g "
          + "LEFT JOIN FETCH g.participants p "
          + "LEFT JOIN FETCH p.user "
          + "LEFT JOIN FETCH g.regionRules "
          + "WHERE g.creator.id = :creatorId "
          + "ORDER BY g.createdAt DESC")
  List<Game> findByCreatorId(@Param("creatorId") UUID creatorId);

  /** Trouve les games qui ne sont pas dans un statut donné */
  List<Game> findByStatusNot(GameStatus status);

  /** Trouve une game par code d'invitation avec relations */
  @Query(
      "SELECT DISTINCT g FROM Game g "
          + "LEFT JOIN FETCH g.participants p "
          + "LEFT JOIN FETCH p.user "
          + "LEFT JOIN FETCH g.creator "
          + "LEFT JOIN FETCH g.regionRules "
          + "WHERE g.invitationCode = :invitationCode")
  Optional<Game> findByInvitationCodeWithFetch(@Param("invitationCode") String invitationCode);

  /** PERFORMANCE: Compte rapide des participants d'une game */
  @Query("SELECT COUNT(p) FROM GameParticipant p WHERE p.game.id = :gameId")
  long countParticipantsByGameId(@Param("gameId") UUID gameId);

  /** PERFORMANCE: Vérifie si un utilisateur participe à une game */
  @Query(
      "SELECT COUNT(p) > 0 FROM GameParticipant p WHERE p.game.id = :gameId AND p.user.id = :userId")
  boolean isUserParticipant(@Param("gameId") UUID gameId, @Param("userId") UUID userId);

  // ============== MÉTHODES PAGINÉES POUR GRANDES DATASETS ==============

  /** PAGINATION: Récupère toutes les games avec pagination et fetch joins */
  @Query(
      value =
          "SELECT DISTINCT g FROM Game g "
              + "LEFT JOIN FETCH g.participants p "
              + "LEFT JOIN FETCH p.user "
              + "LEFT JOIN FETCH g.creator "
              + "LEFT JOIN FETCH g.regionRules "
              + "ORDER BY g.createdAt DESC",
      countQuery = "SELECT COUNT(DISTINCT g) FROM Game g")
  Page<Game> findAllWithFetchPaginated(Pageable pageable);

  // PHASE 1B: CRITICAL N+1 OPTIMIZATION - EntityGraph methods for performance
  @EntityGraph("Game.withBasicDetails")
  List<Game> findAllByOrderByCreatedAtDesc();

  @EntityGraph("Game.withFullDetails")
  Optional<Game> findWithFullDetailsById(UUID id);

  // PHASE 2A: CLEAN ARCHITECTURE - Repository methods for use cases
  long countByCreatorAndStatusIn(User creator, java.util.List<GameStatus> statuses);

  /** PAGINATION: Récupère les games par statut avec pagination */
  @Query(
      value =
          "SELECT DISTINCT g FROM Game g "
              + "LEFT JOIN FETCH g.participants p "
              + "LEFT JOIN FETCH p.user "
              + "LEFT JOIN FETCH g.creator "
              + "WHERE g.status = :status "
              + "ORDER BY g.createdAt DESC",
      countQuery = "SELECT COUNT(DISTINCT g) FROM Game g WHERE g.status = :status")
  Page<Game> findByStatusWithFetchPaginated(@Param("status") GameStatus status, Pageable pageable);

  /** PAGINATION: Récupère les games d'un utilisateur avec pagination */
  @Query(
      value =
          "SELECT DISTINCT g FROM Game g "
              + "LEFT JOIN FETCH g.participants p "
              + "LEFT JOIN FETCH p.user "
              + "LEFT JOIN FETCH g.creator "
              + "WHERE g.creator.id = :userId OR p.user.id = :userId "
              + "ORDER BY g.createdAt DESC",
      countQuery =
          "SELECT COUNT(DISTINCT g) FROM Game g "
              + "LEFT JOIN g.participants p "
              + "WHERE g.creator.id = :userId OR p.user.id = :userId")
  Page<Game> findGamesByUserIdPaginated(@Param("userId") UUID userId, Pageable pageable);

  /** PAGINATION: Recherche games par nom avec pagination */
  @Query(
      value =
          "SELECT DISTINCT g FROM Game g "
              + "LEFT JOIN FETCH g.participants p "
              + "LEFT JOIN FETCH p.user "
              + "LEFT JOIN FETCH g.creator "
              + "WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :namePattern, '%')) "
              + "ORDER BY g.createdAt DESC",
      countQuery =
          "SELECT COUNT(DISTINCT g) FROM Game g "
              + "WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
  Page<Game> findByNameContainingIgnoreCasePaginated(
      @Param("namePattern") String namePattern, Pageable pageable);
}
