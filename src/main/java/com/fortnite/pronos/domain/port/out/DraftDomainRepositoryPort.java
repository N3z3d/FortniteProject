package com.fortnite.pronos.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.draft.model.Draft;

/**
 * Output port for Draft persistence migration using pure domain models.
 *
 * <p>Coexists with legacy {@link DraftRepositoryPort} during ARCH-014 transition.
 */
public interface DraftDomainRepositoryPort {

  Optional<Draft> findById(UUID id);

  Optional<Draft> findByGameId(UUID gameId);

  Optional<Draft> findActiveByGameId(UUID gameId);

  boolean existsByGameId(UUID gameId);

  Draft save(Draft draft);
}
