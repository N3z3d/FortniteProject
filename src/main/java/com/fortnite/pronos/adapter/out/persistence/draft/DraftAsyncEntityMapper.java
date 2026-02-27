package com.fortnite.pronos.adapter.out.persistence.draft;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.draft.model.DraftAsyncSelection;
import com.fortnite.pronos.domain.draft.model.DraftAsyncWindow;
import com.fortnite.pronos.domain.draft.model.DraftAsyncWindowStatus;

/** Converts between JPA entities and domain models for the simultaneous draft. */
@Component
public class DraftAsyncEntityMapper {

  public DraftAsyncWindow windowToDomain(DraftAsyncWindowEntity entity) {
    return DraftAsyncWindow.restore(
        entity.getId(),
        entity.getDraftId(),
        entity.getSlot(),
        entity.getDeadline(),
        DraftAsyncWindowStatus.valueOf(entity.getStatus()),
        entity.getTotalExpected());
  }

  public DraftAsyncWindowEntity windowToEntity(DraftAsyncWindow window) {
    return new DraftAsyncWindowEntity(
        window.getId(),
        window.getDraftId(),
        window.getSlot(),
        window.getDeadline(),
        window.getStatus().name(),
        window.getTotalExpected());
  }

  public DraftAsyncSelection selectionToDomain(DraftAsyncSelectionEntity entity) {
    return DraftAsyncSelection.restore(
        entity.getId(),
        entity.getWindowId(),
        entity.getParticipantId(),
        entity.getPlayerId(),
        entity.getSubmittedAt());
  }

  public DraftAsyncSelectionEntity selectionToEntity(DraftAsyncSelection selection) {
    return new DraftAsyncSelectionEntity(
        selection.getId(),
        selection.getWindowId(),
        selection.getParticipantId(),
        selection.getPlayerId(),
        selection.getSubmittedAt());
  }
}
