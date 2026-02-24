package com.fortnite.pronos.service.draft;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.draft.model.DraftAsyncSelection;
import com.fortnite.pronos.domain.draft.model.DraftAsyncWindow;
import com.fortnite.pronos.domain.draft.model.DraftAsyncWindowStatus;
import com.fortnite.pronos.domain.port.out.DraftAsyncRepositoryPort;
import com.fortnite.pronos.dto.ConflictResolutionResponse;
import com.fortnite.pronos.dto.SimultaneousStatusResponse;

/**
 * Orchestrates the simultaneous draft mode.
 *
 * <ul>
 *   <li>Manages submission windows (open / resolving / resolved).
 *   <li>Detects conflicts (same player chosen by 2+ participants).
 *   <li>Runs server-side random coin flip to guarantee fairness and auditability.
 *   <li>Broadcasts real-time updates via WebSocket on count change and conflict resolution.
 * </ul>
 */
@Service
@Transactional
public class DraftSimultaneousService {

  static final String TOPIC_PREFIX = "/topic/draft/";
  static final String TOPIC_SUFFIX = "/simultaneous";
  static final String WINDOW_ID_FIELD = "windowId";

  private final DraftAsyncRepositoryPort asyncRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final Random random;

  public DraftSimultaneousService(
      DraftAsyncRepositoryPort asyncRepository,
      SimpMessagingTemplate messagingTemplate,
      Random random) {
    this.asyncRepository = asyncRepository;
    this.messagingTemplate = messagingTemplate;
    this.random = random;
  }

  /**
   * Opens a new submission window for the given draft slot.
   *
   * @param draftId the draft identifier
   * @param slot slot label (e.g. "R1P2")
   * @param deadline absolute deadline after which pick-auto triggers
   * @param totalExpected total number of participants who must submit
   * @return the persisted window
   */
  public DraftAsyncWindow openWindow(
      UUID draftId, String slot, Instant deadline, int totalExpected) {
    DraftAsyncWindow window = new DraftAsyncWindow(draftId, slot, deadline, totalExpected);
    return asyncRepository.saveWindow(window);
  }

  /**
   * Records an anonymous submission for a participant.
   *
   * <p>If this is the last expected submission, automatically checks for conflicts and broadcasts
   * either {@code ALL_RESOLVED} (no conflicts) or {@code CONFLICT_RESOLVED} (one conflict).
   *
   * @param windowId the window to submit into
   * @param participantId the submitting participant
   * @param playerId the chosen player
   * @throws IllegalStateException if the participant already submitted or window is closed
   */
  public void submit(UUID windowId, UUID participantId, UUID playerId) {
    DraftAsyncWindow window = requireOpenWindow(windowId);
    requireNotAlreadySubmitted(windowId, participantId);

    asyncRepository.saveSelection(new DraftAsyncSelection(windowId, participantId, playerId));

    int submitted = asyncRepository.countSelectionsByWindowId(windowId);
    broadcastCount(window.getDraftId(), windowId, submitted, window.getTotalExpected());

    if (submitted == window.getTotalExpected()) {
      resolveWindow(window);
    }
  }

  /**
   * Returns the current submission count for the first open window of a draft.
   *
   * @param draftId the draft identifier
   * @return status, or empty if no open window exists
   */
  @Transactional(readOnly = true)
  public Optional<SimultaneousStatusResponse> getStatus(UUID draftId) {
    return asyncRepository.findOpenWindowsByDraftId(draftId).stream()
        .findFirst()
        .map(
            w -> {
              int count = asyncRepository.countSelectionsByWindowId(w.getId());
              return new SimultaneousStatusResponse(
                  draftId, w.getId(), count, w.getTotalExpected());
            });
  }

  /**
   * Resolves the first outstanding conflict in a window. Used by the HTTP endpoint.
   *
   * @param windowId the window to resolve
   * @return the conflict resolution result
   * @throws IllegalStateException if the window is not RESOLVING or has no conflicts
   */
  public ConflictResolutionResponse resolveConflict(UUID windowId) {
    DraftAsyncWindow window =
        asyncRepository
            .findWindowById(windowId)
            .orElseThrow(() -> new IllegalStateException("Window not found: " + windowId));
    if (window.getStatus() != DraftAsyncWindowStatus.RESOLVING) {
      throw new IllegalStateException("Window is not in RESOLVING state: " + windowId);
    }

    List<DraftAsyncSelection> selections = asyncRepository.findSelectionsByWindowId(windowId);
    ConflictResolutionResponse resolution =
        pickFirstConflict(windowId, window.getDraftId(), selections);
    if (resolution == null) {
      throw new IllegalStateException("No conflicts found in window: " + windowId);
    }
    return resolution;
  }

  // ===== PRIVATE HELPERS =====

  private void resolveWindow(DraftAsyncWindow window) {
    List<DraftAsyncSelection> selections = asyncRepository.findSelectionsByWindowId(window.getId());
    Map<UUID, List<DraftAsyncSelection>> byPlayer = groupByPlayer(selections);

    boolean hasConflict = byPlayer.values().stream().anyMatch(l -> l.size() > 1);
    if (!hasConflict) {
      DraftAsyncWindow resolved = window.resolve();
      asyncRepository.saveWindow(resolved);
      broadcastAllResolved(window.getDraftId(), window.getId(), selections);
      return;
    }

    DraftAsyncWindow resolving = window.startResolving();
    asyncRepository.saveWindow(resolving);
    pickFirstConflict(window.getId(), window.getDraftId(), selections);
  }

  private ConflictResolutionResponse pickFirstConflict(
      UUID windowId, UUID draftId, List<DraftAsyncSelection> selections) {
    Map<UUID, List<DraftAsyncSelection>> byPlayer = groupByPlayer(selections);

    return byPlayer.values().stream()
        .filter(group -> group.size() > 1)
        .findFirst()
        .map(
            conflicted -> {
              int winnerIndex = random.nextInt(conflicted.size());
              UUID winnerParticipantId = conflicted.get(winnerIndex).getParticipantId();
              UUID loserParticipantId =
                  conflicted.stream()
                      .filter(s -> !s.getParticipantId().equals(winnerParticipantId))
                      .findFirst()
                      .map(DraftAsyncSelection::getParticipantId)
                      .orElseThrow();
              UUID contestedPlayerId = conflicted.get(0).getPlayerId();
              boolean hasMoreConflicts =
                  byPlayer.values().stream()
                      .filter(g -> g != conflicted)
                      .anyMatch(g -> g.size() > 1);

              ConflictResolutionResponse resolution =
                  new ConflictResolutionResponse(
                      windowId,
                      contestedPlayerId,
                      winnerParticipantId,
                      loserParticipantId,
                      hasMoreConflicts);
              broadcastConflictResolved(draftId, resolution);
              return resolution;
            })
        .orElse(null);
  }

  private Map<UUID, List<DraftAsyncSelection>> groupByPlayer(List<DraftAsyncSelection> selections) {
    Map<UUID, List<DraftAsyncSelection>> result = new HashMap<>();
    selections.forEach(
        s -> result.computeIfAbsent(s.getPlayerId(), k -> new java.util.ArrayList<>()).add(s));
    return result;
  }

  private void broadcastCount(UUID draftId, UUID windowId, int submitted, int total) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "SUBMISSION_COUNT");
    message.put(WINDOW_ID_FIELD, windowId.toString());
    message.put("submitted", submitted);
    message.put("total", total);
    messagingTemplate.convertAndSend(topic(draftId), message);
  }

  private void broadcastAllResolved(
      UUID draftId, UUID windowId, List<DraftAsyncSelection> selections) {
    List<Map<String, String>> selList =
        selections.stream()
            .map(
                s -> {
                  Map<String, String> entry = new HashMap<>();
                  entry.put("participantId", s.getParticipantId().toString());
                  entry.put("playerId", s.getPlayerId().toString());
                  return entry;
                })
            .collect(Collectors.toList());

    Map<String, Object> message = new HashMap<>();
    message.put("type", "ALL_RESOLVED");
    message.put(WINDOW_ID_FIELD, windowId.toString());
    message.put("selections", selList);
    messagingTemplate.convertAndSend(topic(draftId), message);
  }

  private void broadcastConflictResolved(UUID draftId, ConflictResolutionResponse resolution) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", "CONFLICT_RESOLVED");
    message.put(WINDOW_ID_FIELD, resolution.windowId().toString());
    message.put("contestedPlayerId", resolution.contestedPlayerId().toString());
    message.put("winnerParticipantId", resolution.winnerParticipantId().toString());
    message.put("loserParticipantId", resolution.loserParticipantId().toString());
    message.put("hasMoreConflicts", resolution.hasMoreConflicts());
    messagingTemplate.convertAndSend(topic(draftId), message);
  }

  private String topic(UUID draftId) {
    return TOPIC_PREFIX + draftId + TOPIC_SUFFIX;
  }

  private DraftAsyncWindow requireOpenWindow(UUID windowId) {
    DraftAsyncWindow window =
        asyncRepository
            .findWindowById(windowId)
            .orElseThrow(() -> new IllegalStateException("Window not found: " + windowId));
    if (window.getStatus() != DraftAsyncWindowStatus.OPEN) {
      throw new IllegalStateException("Window is not OPEN: " + windowId);
    }
    return window;
  }

  private void requireNotAlreadySubmitted(UUID windowId, UUID participantId) {
    if (asyncRepository.existsSelectionByWindowAndParticipant(windowId, participantId)) {
      throw new IllegalStateException(
          "Participant " + participantId + " already submitted in window " + windowId);
    }
  }
}
