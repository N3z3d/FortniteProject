package com.fortnite.pronos.adapter.out.persistence.draft;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.draft.model.DraftAsyncSelection;
import com.fortnite.pronos.domain.draft.model.DraftAsyncWindow;
import com.fortnite.pronos.domain.port.out.DraftAsyncRepositoryPort;

/** Persistence adapter for simultaneous-draft windows and selections. */
@Component
public class DraftAsyncRepositoryAdapter implements DraftAsyncRepositoryPort {

  private final DraftAsyncWindowJpaRepository windowRepo;
  private final DraftAsyncSelectionJpaRepository selectionRepo;
  private final DraftAsyncEntityMapper mapper;

  public DraftAsyncRepositoryAdapter(
      DraftAsyncWindowJpaRepository windowRepo,
      DraftAsyncSelectionJpaRepository selectionRepo,
      DraftAsyncEntityMapper mapper) {
    this.windowRepo = windowRepo;
    this.selectionRepo = selectionRepo;
    this.mapper = mapper;
  }

  @Override
  public DraftAsyncWindow saveWindow(DraftAsyncWindow window) {
    DraftAsyncWindowEntity entity = mapper.windowToEntity(window);
    DraftAsyncWindowEntity saved = windowRepo.save(entity);
    return mapper.windowToDomain(saved);
  }

  @Override
  public Optional<DraftAsyncWindow> findWindowById(UUID windowId) {
    return windowRepo.findById(windowId).map(mapper::windowToDomain);
  }

  @Override
  public List<DraftAsyncWindow> findOpenWindowsByDraftId(UUID draftId) {
    return windowRepo.findByDraftIdAndStatus(draftId, "OPEN").stream()
        .map(mapper::windowToDomain)
        .toList();
  }

  @Override
  public DraftAsyncSelection saveSelection(DraftAsyncSelection selection) {
    DraftAsyncSelectionEntity entity = mapper.selectionToEntity(selection);
    DraftAsyncSelectionEntity saved = selectionRepo.save(entity);
    return mapper.selectionToDomain(saved);
  }

  @Override
  public List<DraftAsyncSelection> findSelectionsByWindowId(UUID windowId) {
    return selectionRepo.findByWindowId(windowId).stream().map(mapper::selectionToDomain).toList();
  }

  @Override
  public boolean existsSelectionByWindowAndParticipant(UUID windowId, UUID participantId) {
    return selectionRepo.existsByWindowIdAndParticipantId(windowId, participantId);
  }

  @Override
  public int countSelectionsByWindowId(UUID windowId) {
    return selectionRepo.countByWindowId(windowId);
  }
}
