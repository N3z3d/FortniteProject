package com.fortnite.pronos.adapter.out.persistence.draft;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link DraftRegionCursorEntity}. */
public interface DraftRegionCursorJpaRepository
    extends JpaRepository<DraftRegionCursorEntity, DraftRegionCursorId> {

  Optional<DraftRegionCursorEntity> findByIdDraftIdAndIdRegion(UUID draftId, String region);
}
