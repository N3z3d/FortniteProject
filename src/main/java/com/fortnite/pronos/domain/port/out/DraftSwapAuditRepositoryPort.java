package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.UUID;

import com.fortnite.pronos.domain.draft.model.DraftSwapAuditEntry;

/** Output port for persisting and querying solo swap audit entries (FR-36). */
public interface DraftSwapAuditRepositoryPort {

  DraftSwapAuditEntry save(DraftSwapAuditEntry entry);

  List<DraftSwapAuditEntry> findByDraftId(UUID draftId);
}
