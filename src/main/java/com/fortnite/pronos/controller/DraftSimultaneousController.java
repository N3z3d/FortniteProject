package com.fortnite.pronos.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.ConflictResolutionResponse;
import com.fortnite.pronos.dto.SimultaneousStatusResponse;
import com.fortnite.pronos.dto.SimultaneousSubmitRequest;
import com.fortnite.pronos.service.draft.DraftSimultaneousService;

/** REST API for simultaneous draft mode (anonymous submit + conflict resolution). */
@RestController
@RequestMapping("/api/draft/simultaneous")
public class DraftSimultaneousController {

  private final DraftSimultaneousService simultaneousService;

  public DraftSimultaneousController(DraftSimultaneousService simultaneousService) {
    this.simultaneousService = simultaneousService;
  }

  /**
   * Opens a new submission window for the current draft slot.
   *
   * <p>Called by the host/game manager to start a simultaneous pick round.
   */
  @PostMapping("/{draftId}/open-window")
  public ResponseEntity<String> openWindow(
      @PathVariable UUID draftId,
      @RequestParam String slot,
      @RequestParam int deadlineSeconds,
      @RequestParam int totalParticipants) {
    Instant deadline = Instant.now().plusSeconds(deadlineSeconds);
    var window = simultaneousService.openWindow(draftId, slot, deadline, totalParticipants);
    return ResponseEntity.ok(window.getId().toString());
  }

  /**
   * Submits an anonymous player pick for the current window.
   *
   * <p>The chosen player is not broadcast to other participants until resolution.
   */
  @PostMapping("/{draftId}/submit")
  public ResponseEntity<Void> submit(
      @PathVariable UUID draftId, @RequestBody SimultaneousSubmitRequest request) {
    simultaneousService.submit(request.windowId(), request.participantId(), request.playerId());
    return ResponseEntity.ok().build();
  }

  /**
   * Returns the current submission count (N/total) for the draft.
   *
   * <p>Does not reveal which players were chosen — preserves anonymity.
   */
  @GetMapping("/{draftId}/status")
  public ResponseEntity<SimultaneousStatusResponse> getStatus(@PathVariable UUID draftId) {
    return simultaneousService
        .getStatus(draftId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /** Manually triggers conflict resolution for a specific window (coin flip). */
  @PostMapping("/{draftId}/resolve-conflict/{windowId}")
  public ResponseEntity<ConflictResolutionResponse> resolveConflict(
      @PathVariable UUID draftId, @PathVariable UUID windowId) {
    ConflictResolutionResponse resolution = simultaneousService.resolveConflict(windowId);
    return ResponseEntity.ok(resolution);
  }
}
