package com.fortnite.pronos.adapter.out.persistence.player.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlayerIdentityRepositoryAdapter implements PlayerIdentityRepositoryPort {

  private final PlayerIdentityJpaRepository jpaRepository;
  private final PlayerIdentityEntityMapper mapper;

  @Override
  public List<PlayerIdentityEntry> findByStatus(IdentityStatus status) {
    return jpaRepository.findByStatus(status).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<PlayerIdentityEntry> findByStatusPaged(IdentityStatus status, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return jpaRepository.findByStatus(status, pageable).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Optional<PlayerIdentityEntry> findByPlayerId(UUID playerId) {
    return jpaRepository.findByPlayerId(playerId).map(mapper::toDomain);
  }

  @Override
  public long countByStatus(IdentityStatus status) {
    return jpaRepository.countByStatus(status);
  }

  @Override
  public PlayerIdentityEntry save(PlayerIdentityEntry entry) {
    PlayerIdentityEntity entity = mapper.toEntity(entry);
    PlayerIdentityEntity saved = jpaRepository.save(entity);
    return mapper.toDomain(saved);
  }
}
