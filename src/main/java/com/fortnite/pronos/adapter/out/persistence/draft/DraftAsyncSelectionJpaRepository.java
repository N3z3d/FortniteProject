package com.fortnite.pronos.adapter.out.persistence.draft;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link DraftAsyncSelectionEntity}. */
public interface DraftAsyncSelectionJpaRepository
    extends JpaRepository<DraftAsyncSelectionEntity, UUID> {

  List<DraftAsyncSelectionEntity> findByWindowId(UUID windowId);

  boolean existsByWindowIdAndParticipantId(UUID windowId, UUID participantId);

  int countByWindowId(UUID windowId);
}
