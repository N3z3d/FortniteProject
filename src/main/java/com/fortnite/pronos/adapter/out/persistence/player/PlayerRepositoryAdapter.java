package com.fortnite.pronos.adapter.out.persistence.player;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.repository.PlayerRepository;

/**
 * Persistence adapter for the Player domain migration. Implements a dedicated domain port without
 * modifying the legacy {@code PlayerRepositoryPort}.
 */
@Component
public class PlayerRepositoryAdapter implements PlayerDomainRepositoryPort {

  private final PlayerRepository playerRepository;
  private final PlayerEntityMapper mapper;

  public PlayerRepositoryAdapter(PlayerRepository playerRepository, PlayerEntityMapper mapper) {
    this.playerRepository = playerRepository;
    this.mapper = mapper;
  }

  @Override
  public Optional<com.fortnite.pronos.domain.player.model.Player> findById(UUID id) {
    return playerCrudRepository().findById(id).map(mapper::toDomain);
  }

  @Override
  public com.fortnite.pronos.domain.player.model.Player save(
      com.fortnite.pronos.domain.player.model.Player player) {
    Objects.requireNonNull(player, "Player cannot be null");
    com.fortnite.pronos.model.Player entity = mapper.toEntity(player);
    com.fortnite.pronos.model.Player savedEntity = playerCrudRepository().save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public List<com.fortnite.pronos.domain.player.model.Player> findByRegion(PlayerRegion region) {
    if (region == null) {
      return Collections.emptyList();
    }
    com.fortnite.pronos.model.Player.Region entityRegion = mapper.toEntityRegion(region);
    return mapper.toDomainList(playerRepository.findByRegion(entityRegion));
  }

  @Override
  public List<com.fortnite.pronos.domain.player.model.Player> findByTranche(String tranche) {
    if (tranche == null) {
      return Collections.emptyList();
    }
    return mapper.toDomainList(playerRepository.findByTranche(tranche));
  }

  @Override
  public List<com.fortnite.pronos.domain.player.model.Player> findActivePlayers() {
    return mapper.toDomainList(playerRepository.findActivePlayers());
  }

  @Override
  public Optional<com.fortnite.pronos.domain.player.model.Player> findByNickname(String nickname) {
    if (nickname == null) {
      return Optional.empty();
    }
    return playerRepository.findByNickname(nickname).map(mapper::toDomain);
  }

  @Override
  public Optional<com.fortnite.pronos.domain.player.model.Player> findByUsername(String username) {
    if (username == null) {
      return Optional.empty();
    }
    return playerRepository.findByUsername(username).map(mapper::toDomain);
  }

  @Override
  public boolean existsByNickname(String nickname) {
    if (nickname == null) {
      return false;
    }
    return playerRepository.existsByNickname(nickname);
  }

  @Override
  public long countByRegion(PlayerRegion region) {
    if (region == null) {
      return 0L;
    }
    return playerRepository.countByRegion(mapper.toEntityRegion(region));
  }

  @Override
  public List<com.fortnite.pronos.domain.player.model.Player> findAll() {
    return mapper.toDomainList(playerRepository.findAll());
  }

  @Override
  public long count() {
    return playerRepository.count();
  }

  private CrudRepository<com.fortnite.pronos.model.Player, UUID> playerCrudRepository() {
    return playerRepository;
  }
}
