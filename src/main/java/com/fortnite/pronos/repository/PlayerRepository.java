package com.fortnite.pronos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.model.Player;

/** Repository pour la gestion des joueurs Fortnite */
@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID>, PlayerRepositoryPort {

  /** Trouver un joueur par son nickname */
  Optional<Player> findByNickname(String nickname);

  /** Trouver les joueurs par région */
  List<Player> findByRegion(Player.Region region);

  /** Vérifier si un joueur existe par son nickname */
  boolean existsByNickname(String nickname);

  // Note: Pas de notion d'actif/inactif pour l'instant
  // List<Player> findByActiveTrue();

  /** Compter les joueurs par région */
  long countByRegion(Player.Region region);

  /** Trouver un joueur par username (compatibilité) */
  Optional<Player> findByUsername(String username);

  /** Trouver les joueurs par tranche */
  List<Player> findByTranche(String tranche);

  /** Rechercher des joueurs par nickname (avec pagination) */
  @Query("SELECT p FROM Player p WHERE LOWER(p.nickname) LIKE LOWER(CONCAT('%', :query, '%'))")
  Page<Player> searchByNickname(@Param("query") String query, Pageable pageable);

  /** Recherche pageable avec filtres optionnels */
  @Query(
      "SELECT p FROM Player p "
          + "WHERE (:query IS NULL OR LOWER(p.nickname) LIKE LOWER(CONCAT('%', :query, '%'))) "
          + "AND (:region IS NULL OR p.region = :region) "
          + "AND (:tranche IS NULL OR p.tranche = :tranche) "
          + "AND (:available = false OR COALESCE(p.locked, false) = false)")
  Page<Player> searchPlayers(
      @Param("query") String query,
      @Param("region") Player.Region region,
      @Param("tranche") String tranche,
      @Param("available") boolean available,
      Pageable pageable);

  /** Trouver les joueurs actifs (alias pour findByActiveTrue) */
  // Retourne tous les joueurs (pas de notion d'actif/inactif pour l'instant)
  @Query("SELECT p FROM Player p")
  List<Player> findActivePlayers();
}
