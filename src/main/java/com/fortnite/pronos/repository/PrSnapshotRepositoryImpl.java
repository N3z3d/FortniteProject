package com.fortnite.pronos.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.fortnite.pronos.model.PrSnapshot;

@Repository
class PrSnapshotRepositoryImpl implements PrSnapshotRepositoryCustom {

  @PersistenceContext private EntityManager entityManager;

  @Override
  public void persist(PrSnapshot snapshot) {
    if (snapshot.getId() == null) {
      snapshot.setId(UUID.randomUUID());
    }
    if (snapshot.getPrValue() == null) {
      snapshot.setPrValue(0);
    }
    if (snapshot.getCollectedAt() == null) {
      snapshot.setCollectedAt(OffsetDateTime.now());
    }
    entityManager
        .createNativeQuery(
            """
            INSERT INTO pr_snapshots (
              player_id,
              region,
              snapshot_date,
              id,
              points,
              pr_value,
              rank,
              collected_at,
              run_id
            ) VALUES (
              :playerId,
              CAST(:region AS pr_region),
              :snapshotDate,
              :id,
              :points,
              :prValue,
              :rank,
              :collectedAt,
              :runId
            )
            """)
        .setParameter("playerId", snapshot.getPlayer().getId())
        .setParameter("region", snapshot.getRegion().name())
        .setParameter("snapshotDate", snapshot.getSnapshotDate())
        .setParameter("id", snapshot.getId())
        .setParameter("points", snapshot.getPoints())
        .setParameter("prValue", snapshot.getPrValue())
        .setParameter("rank", snapshot.getRank())
        .setParameter("collectedAt", snapshot.getCollectedAt())
        .setParameter("runId", snapshot.getRun() != null ? snapshot.getRun().getId() : null)
        .executeUpdate();
  }
}
