package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.draft.model.DraftParticipantTrade;

/**
 * Output port for DraftParticipantTrade persistence operations (FR-34, FR-35). Implemented by the
 * persistence adapter (DraftParticipantTradeRepositoryAdapter).
 */
public interface DraftParticipantTradeRepositoryPort {

  DraftParticipantTrade save(DraftParticipantTrade trade);

  Optional<DraftParticipantTrade> findById(UUID tradeId);

  List<DraftParticipantTrade> findPendingByDraftId(UUID draftId);

  List<DraftParticipantTrade> findByDraftId(UUID draftId);
}
