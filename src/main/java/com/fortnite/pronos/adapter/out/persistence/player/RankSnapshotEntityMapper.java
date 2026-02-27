package com.fortnite.pronos.adapter.out.persistence.player;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.player.model.RankSnapshot;

@Component
public class RankSnapshotEntityMapper {

  public RankSnapshot toDomain(RankSnapshotEntity entity) {
    return RankSnapshot.restore(
        entity.getId(),
        entity.getPlayerId(),
        entity.getRegion(),
        entity.getRank(),
        entity.getPrValue(),
        entity.getSnapshotDate());
  }

  public RankSnapshotEntity toEntity(RankSnapshot snapshot) {
    RankSnapshotEntity entity = new RankSnapshotEntity();
    entity.setId(snapshot.getId());
    entity.setPlayerId(snapshot.getPlayerId());
    entity.setRegion(snapshot.getRegion());
    entity.setRank(snapshot.getRank());
    entity.setPrValue(snapshot.getPrValue());
    entity.setSnapshotDate(snapshot.getSnapshotDate());
    return entity;
  }
}
