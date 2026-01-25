package com.fortnite.pronos.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.DraftPick;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.Player;

/** Repository pour la gestion des picks de draft */
@Repository
public interface DraftPickRepository
    extends JpaRepository<DraftPick, UUID>, DraftPickRepositoryPort {

  /** Trouver les picks d'un draft */
  List<DraftPick> findByDraft(Draft draft);

  /** Trouver les picks d'un participant */
  List<DraftPick> findByParticipant(GameParticipant participant);

  /** Vérifier si un joueur a déjà été sélectionné dans un draft */
  boolean existsByDraftAndPlayer(Draft draft, Player player);

  /** Compter les joueurs sélectionnés par région dans un draft */
  @Query(
      "SELECT COUNT(dp) FROM DraftPick dp WHERE dp.draft = :draft AND dp.player.region = :region")
  long countByDraftAndPlayerRegion(
      @Param("draft") Draft draft, @Param("region") Player.Region region);

  /** Trouver les picks d'un round spécifique */
  List<DraftPick> findByDraftAndRound(Draft draft, Integer round);

  /** Trouver un pick spécifique */
  DraftPick findByDraftAndRoundAndPickNumber(Draft draft, Integer round, Integer pickNumber);

  /** Compter le nombre de picks d'un participant dans un draft */
  long countByDraftAndParticipant(Draft draft, GameParticipant participant);

  /** Trouver les picks d'un draft ordonnés par ordre de sélection */
  List<DraftPick> findByDraftOrderByPickNumber(Draft draft);
}
