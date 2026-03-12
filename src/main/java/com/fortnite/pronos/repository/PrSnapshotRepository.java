package com.fortnite.pronos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.model.PrSnapshot;

@Repository
public interface PrSnapshotRepository
    extends JpaRepository<PrSnapshot, PrSnapshot.PrSnapshotId>, PrSnapshotRepositoryCustom {

  @Query("SELECT s FROM PrSnapshot s WHERE s.player.id = :playerId AND s.snapshotDate >= :since")
  List<PrSnapshot> findByPlayerIdSince(
      @Param("playerId") UUID playerId, @Param("since") LocalDate since);

  @Query(
      value =
          """
          SELECT *
          FROM pr_snapshots
          WHERE player_id = :playerId
            AND region = CAST(:region AS pr_region)
            AND snapshot_date = :snapshotDate
          """,
      nativeQuery = true)
  Optional<PrSnapshot> findForUpsert(
      @Param("playerId") UUID playerId,
      @Param("region") String region,
      @Param("snapshotDate") LocalDate snapshotDate);
}
