package com.fortnite.pronos.adapter.out.persistence.snapshot;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.port.out.PrSnapshotQueryPort;
import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.model.PrSnapshot;
import com.fortnite.pronos.repository.PrSnapshotRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PrSnapshotQueryAdapter implements PrSnapshotQueryPort {

  private final PrSnapshotRepository snapshotRepository;

  @Override
  public Optional<String> findMainRegionNameForPlayer(UUID playerId, LocalDate since) {
    return snapshotRepository.findByPlayerIdSince(playerId, since).stream()
        .filter(s -> s.getRegion() != PrRegion.GLOBAL)
        .collect(Collectors.groupingBy(PrSnapshot::getRegion, Collectors.counting()))
        .entrySet()
        .stream()
        .max(Comparator.comparingLong(Map.Entry::getValue))
        .map(e -> e.getKey().name());
  }
}
