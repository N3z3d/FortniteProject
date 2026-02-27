package com.fortnite.pronos.adapter.out.persistence.player;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.player.model.RankSnapshot;
import com.fortnite.pronos.domain.port.out.RankSnapshotRepositoryPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RankSnapshotRepositoryAdapter implements RankSnapshotRepositoryPort {

  private final RankSnapshotJpaRepository jpaRepository;
  private final RankSnapshotEntityMapper mapper;

  @Override
  public List<RankSnapshot> findByPlayerAndRegion(UUID playerId, String region, int days) {
    LocalDate since = LocalDate.now().minusDays(days);
    return jpaRepository.findByPlayerAndRegionSince(playerId, region, since).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<RankSnapshot> findByPlayerRecent(UUID playerId, int days) {
    LocalDate since = LocalDate.now().minusDays(days);
    return jpaRepository.findByPlayerSince(playerId, since).stream().map(mapper::toDomain).toList();
  }

  @Override
  public RankSnapshot save(RankSnapshot snapshot) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(snapshot)));
  }
}
