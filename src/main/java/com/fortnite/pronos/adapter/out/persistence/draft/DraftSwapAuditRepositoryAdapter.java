package com.fortnite.pronos.adapter.out.persistence.draft;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.draft.model.DraftSwapAuditEntry;
import com.fortnite.pronos.domain.port.out.DraftSwapAuditRepositoryPort;
import com.fortnite.pronos.model.DraftSwapAuditEntity;
import com.fortnite.pronos.repository.DraftSwapAuditJpaRepository;

import lombok.RequiredArgsConstructor;

/** JPA adapter for DraftSwapAuditRepositoryPort. */
@Component
@RequiredArgsConstructor
public class DraftSwapAuditRepositoryAdapter implements DraftSwapAuditRepositoryPort {

  private final DraftSwapAuditJpaRepository jpaRepository;

  @Override
  public DraftSwapAuditEntry save(DraftSwapAuditEntry entry) {
    DraftSwapAuditEntity entity = new DraftSwapAuditEntity();
    entity.setDraftId(entry.getDraftId());
    entity.setParticipantId(entry.getParticipantId());
    entity.setPlayerOutId(entry.getPlayerOutId());
    entity.setPlayerInId(entry.getPlayerInId());
    entity.setOccurredAt(entry.getOccurredAt());
    DraftSwapAuditEntity saved = jpaRepository.save(entity);
    return toDomain(saved);
  }

  @Override
  public List<DraftSwapAuditEntry> findByDraftId(UUID draftId) {
    return jpaRepository.findByDraftIdOrderByOccurredAtDesc(draftId).stream()
        .map(this::toDomain)
        .toList();
  }

  private DraftSwapAuditEntry toDomain(DraftSwapAuditEntity e) {
    return DraftSwapAuditEntry.restore(
        e.getId(),
        e.getDraftId(),
        e.getParticipantId(),
        e.getPlayerOutId(),
        e.getPlayerInId(),
        e.getOccurredAt());
  }
}
