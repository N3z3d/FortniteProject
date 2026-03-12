package com.fortnite.pronos.controller;

import java.security.Principal;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.DraftPickDto;
import com.fortnite.pronos.dto.admin.AdminRosterAssignRequest;
import com.fortnite.pronos.service.admin.AdminAuditLogService;
import com.fortnite.pronos.service.admin.AdminDraftRosterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Admin endpoint for manual roster management — assign and remove players in any game draft. */
@Slf4j
@RestController
@RequestMapping("/api/admin/games/{gameId}/roster")
@RequiredArgsConstructor
public class AdminDraftRosterController {

  private final AdminDraftRosterService rosterService;
  private final AdminAuditLogService auditLogService;

  @PostMapping
  public ResponseEntity<DraftPickDto> assignPlayer(
      @PathVariable UUID gameId,
      @RequestBody @Valid AdminRosterAssignRequest request,
      Principal principal) {
    DraftPickDto result =
        rosterService.assignPlayer(gameId, request.participantUserId(), request.playerId());
    String actor = principal != null ? principal.getName() : "admin";
    auditLogService.recordAction(
        actor,
        "ASSIGN_ROSTER",
        "DRAFT",
        gameId.toString(),
        "player=" + request.playerId() + ", participant=" + request.participantUserId());
    return ResponseEntity.ok(result);
  }

  @DeleteMapping("/{playerId}")
  public ResponseEntity<Void> removePlayer(
      @PathVariable UUID gameId, @PathVariable UUID playerId, Principal principal) {
    rosterService.removePlayer(gameId, playerId);
    String actor = principal != null ? principal.getName() : "admin";
    auditLogService.recordAction(
        actor, "REMOVE_ROSTER", "DRAFT", gameId.toString(), "player=" + playerId);
    return ResponseEntity.noContent().build();
  }
}
