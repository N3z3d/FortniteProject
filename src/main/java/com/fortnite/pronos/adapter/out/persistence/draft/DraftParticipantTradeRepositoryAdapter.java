package com.fortnite.pronos.adapter.out.persistence.draft;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.draft.model.DraftParticipantTrade;
import com.fortnite.pronos.domain.draft.model.DraftParticipantTradeStatus;
import com.fortnite.pronos.domain.port.out.DraftParticipantTradeRepositoryPort;
import com.fortnite.pronos.model.DraftParticipantTradeEntity;
import com.fortnite.pronos.repository.DraftParticipantTradeJpaRepository;

import lombok.RequiredArgsConstructor;

/** Hexagonal adapter — bridges DraftParticipantTradeRepositoryPort and JPA. */
@Component
@RequiredArgsConstructor
public class DraftParticipantTradeRepositoryAdapter implements DraftParticipantTradeRepositoryPort {

  private final DraftParticipantTradeJpaRepository jpaRepository;

  @Override
  public DraftParticipantTrade save(DraftParticipantTrade trade) {
    DraftParticipantTradeEntity entity =
        jpaRepository.findById(trade.getId()).orElseGet(DraftParticipantTradeEntity::new);
    entity.setId(trade.getId());
    entity.setDraftId(trade.getDraftId());
    entity.setProposerParticipantId(trade.getProposerParticipantId());
    entity.setTargetParticipantId(trade.getTargetParticipantId());
    entity.setPlayerFromProposerId(trade.getPlayerFromProposerId());
    entity.setPlayerFromTargetId(trade.getPlayerFromTargetId());
    entity.setStatus(trade.getStatus().name());
    entity.setProposedAt(trade.getProposedAt());
    entity.setResolvedAt(trade.getResolvedAt());
    return toDomain(jpaRepository.save(entity));
  }

  @Override
  public Optional<DraftParticipantTrade> findById(UUID tradeId) {
    return jpaRepository.findById(tradeId).map(this::toDomain);
  }

  @Override
  public List<DraftParticipantTrade> findPendingByDraftId(UUID draftId) {
    return jpaRepository.findByDraftIdAndStatus(draftId, "PENDING").stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<DraftParticipantTrade> findByDraftId(UUID draftId) {
    return jpaRepository.findByDraftId(draftId).stream().map(this::toDomain).toList();
  }

  private DraftParticipantTrade toDomain(DraftParticipantTradeEntity e) {
    return DraftParticipantTrade.restore(
        e.getId(),
        e.getDraftId(),
        e.getProposerParticipantId(),
        e.getTargetParticipantId(),
        e.getPlayerFromProposerId(),
        e.getPlayerFromTargetId(),
        DraftParticipantTradeStatus.valueOf(e.getStatus()),
        e.getProposedAt(),
        e.getResolvedAt());
  }
}
