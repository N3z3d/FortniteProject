package com.fortnite.pronos.adapter.out.persistence.draft;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.draft.model.Draft;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.model.Game;

/** Persistence adapter for Draft domain migration (ARCH-014). */
@Component
public class DraftRepositoryAdapter implements DraftDomainRepositoryPort {

  private final DraftRepositoryPort draftRepository;
  private final GameRepositoryPort gameRepository;
  private final DraftEntityMapper mapper;

  public DraftRepositoryAdapter(
      DraftRepositoryPort draftRepository,
      GameRepositoryPort gameRepository,
      DraftEntityMapper mapper) {
    this.draftRepository = draftRepository;
    this.gameRepository = gameRepository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Draft> findById(UUID id) {
    return draftCrudRepository().findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Draft> findByGameId(UUID gameId) {
    return findGame(gameId).flatMap(draftRepository::findByGame).map(mapper::toDomain);
  }

  @Override
  public Optional<Draft> findActiveByGameId(UUID gameId) {
    return findGame(gameId)
        .flatMap(
            game ->
                draftRepository.findByGameAndStatus(
                    game, com.fortnite.pronos.model.Draft.Status.ACTIVE))
        .map(mapper::toDomain);
  }

  @Override
  public boolean existsByGameId(UUID gameId) {
    return findGame(gameId).map(draftRepository::existsByGame).orElse(false);
  }

  @Override
  public Draft save(Draft draft) {
    Objects.requireNonNull(draft, "Draft cannot be null");
    Game game = findRequiredGame(draft.getGameId());
    com.fortnite.pronos.model.Draft entity = mapper.toEntity(draft, game);
    com.fortnite.pronos.model.Draft saved = draftCrudRepository().save(entity);
    return mapper.toDomain(saved);
  }

  private Optional<Game> findGame(UUID gameId) {
    if (gameId == null) {
      return Optional.empty();
    }
    return gameRepository.findById(gameId);
  }

  private Game findRequiredGame(UUID gameId) {
    if (gameId == null) {
      throw new IllegalArgumentException("Game ID cannot be null");
    }
    return gameRepository
        .findById(gameId)
        .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
  }

  private CrudRepository<com.fortnite.pronos.model.Draft, UUID> draftCrudRepository() {
    return (CrudRepository<com.fortnite.pronos.model.Draft, UUID>) draftRepository;
  }
}
