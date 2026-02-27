package com.fortnite.pronos.adapter.out.persistence.player.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;

public interface PlayerIdentityJpaRepository extends JpaRepository<PlayerIdentityEntity, UUID> {

  List<PlayerIdentityEntity> findByStatus(IdentityStatus status);

  Page<PlayerIdentityEntity> findByStatus(IdentityStatus status, Pageable pageable);

  Optional<PlayerIdentityEntity> findByPlayerId(UUID playerId);

  long countByStatus(IdentityStatus status);
}
