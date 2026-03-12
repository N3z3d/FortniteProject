package com.fortnite.pronos.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.model.DraftParticipantTradeEntity;

/** JPA repository for DraftParticipantTradeEntity. */
@Repository
public interface DraftParticipantTradeJpaRepository
    extends JpaRepository<DraftParticipantTradeEntity, UUID> {

  List<DraftParticipantTradeEntity> findByDraftIdAndStatus(UUID draftId, String status);

  List<DraftParticipantTradeEntity> findByDraftId(UUID draftId);
}
