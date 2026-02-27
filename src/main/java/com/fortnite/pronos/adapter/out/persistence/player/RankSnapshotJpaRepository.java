package com.fortnite.pronos.adapter.out.persistence.player;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RankSnapshotJpaRepository extends JpaRepository<RankSnapshotEntity, UUID> {

  @Query(
      "SELECT r FROM RankSnapshotEntity r "
          + "WHERE r.playerId = :playerId "
          + "AND r.region = :region "
          + "AND r.snapshotDate >= :since "
          + "ORDER BY r.snapshotDate ASC")
  List<RankSnapshotEntity> findByPlayerAndRegionSince(
      @Param("playerId") UUID playerId,
      @Param("region") String region,
      @Param("since") LocalDate since);

  @Query(
      "SELECT r FROM RankSnapshotEntity r "
          + "WHERE r.playerId = :playerId "
          + "AND r.snapshotDate >= :since "
          + "ORDER BY r.snapshotDate ASC")
  List<RankSnapshotEntity> findByPlayerSince(
      @Param("playerId") UUID playerId, @Param("since") LocalDate since);
}
