package com.fortnite.pronos.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.Game;

/**
 * Output port for Draft persistence operations. Implemented by the persistence adapter
 * (DraftRepository).
 */
public interface DraftRepositoryPort {

  Optional<Draft> findById(UUID id);

  Optional<Draft> findByGame(Game game);

  boolean existsByGame(Game game);

  Optional<Draft> findByGameAndStatus(Game game, Draft.Status status);

  Draft save(Draft draft);
}
