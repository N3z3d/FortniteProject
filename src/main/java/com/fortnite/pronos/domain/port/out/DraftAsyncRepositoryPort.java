package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.draft.model.DraftAsyncSelection;
import com.fortnite.pronos.domain.draft.model.DraftAsyncWindow;

/** Output port for persisting simultaneous draft windows and selections. */
public interface DraftAsyncRepositoryPort {

  DraftAsyncWindow saveWindow(DraftAsyncWindow window);

  Optional<DraftAsyncWindow> findWindowById(UUID windowId);

  List<DraftAsyncWindow> findOpenWindowsByDraftId(UUID draftId);

  DraftAsyncSelection saveSelection(DraftAsyncSelection selection);

  List<DraftAsyncSelection> findSelectionsByWindowId(UUID windowId);

  boolean existsSelectionByWindowAndParticipant(UUID windowId, UUID participantId);

  int countSelectionsByWindowId(UUID windowId);
}
