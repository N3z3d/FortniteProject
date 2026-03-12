package com.fortnite.pronos.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.model.DraftSwapAuditEntity;

/** JPA repository for DraftSwapAuditEntity. */
@Repository
public interface DraftSwapAuditJpaRepository extends JpaRepository<DraftSwapAuditEntity, UUID> {

  List<DraftSwapAuditEntity> findByDraftIdOrderByOccurredAtDesc(UUID draftId);
}
