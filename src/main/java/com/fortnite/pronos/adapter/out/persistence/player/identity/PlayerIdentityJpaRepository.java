package com.fortnite.pronos.adapter.out.persistence.player.identity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;

public interface PlayerIdentityJpaRepository extends JpaRepository<PlayerIdentityEntity, UUID> {

  List<PlayerIdentityEntity> findByStatus(IdentityStatus status);

  Page<PlayerIdentityEntity> findByStatus(IdentityStatus status, Pageable pageable);

  Optional<PlayerIdentityEntity> findByPlayerId(UUID playerId);

  long countByStatus(IdentityStatus status);

  @Query(
      "SELECT e.playerRegion, e.status, COUNT(e) FROM PlayerIdentityEntity e"
          + " GROUP BY e.playerRegion, e.status")
  List<Object[]> countByRegionAndStatus();

  @Query(
      "SELECT e.playerRegion, MAX(e.createdAt) FROM PlayerIdentityEntity e"
          + " GROUP BY e.playerRegion")
  List<Object[]> findLastIngestedAtByRegion();

  @Query("SELECT MIN(e.createdAt) FROM PlayerIdentityEntity e WHERE e.status = :status")
  Optional<LocalDateTime> findOldestCreatedAtByStatus(@Param("status") IdentityStatus status);
}
