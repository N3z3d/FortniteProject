package com.fortnite.pronos.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.draft.model.DraftRegionCursor;

/** Output port for persisting and retrieving snake-draft cursors per region. */
public interface DraftRegionCursorRepositoryPort {

  Optional<DraftRegionCursor> findByDraftIdAndRegion(UUID draftId, String region);

  DraftRegionCursor save(DraftRegionCursor cursor);
}
