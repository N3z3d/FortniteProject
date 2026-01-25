package com.fortnite.pronos.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.Game;

/** Repository pour la gestion des drafts */
@Repository
public interface DraftRepository extends JpaRepository<Draft, UUID>, DraftRepositoryPort {

  /** Trouver un draft par game */
  Optional<Draft> findByGame(Game game);

  /** Vérifier si un draft existe pour une game */
  boolean existsByGame(Game game);

  /** Trouver un draft actif par game */
  Optional<Draft> findByGameAndStatus(Game game, Draft.Status status);
}
