package com.fortnite.pronos.adapter.out.persistence.draft;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link DraftAsyncWindowEntity}. */
public interface DraftAsyncWindowJpaRepository extends JpaRepository<DraftAsyncWindowEntity, UUID> {

  List<DraftAsyncWindowEntity> findByDraftIdAndStatus(UUID draftId, String status);
}
