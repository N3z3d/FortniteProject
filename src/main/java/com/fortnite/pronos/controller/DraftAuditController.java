package com.fortnite.pronos.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.DraftAuditEntryResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.draft.DraftAuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Controller exposing the draft audit trail (FR-36). */
@Slf4j
@RestController
@RequestMapping("/api/games/{gameId}/draft/audit")
@RequiredArgsConstructor
public class DraftAuditController {

  private final DraftAuditService auditService;
  private final UserResolver userResolver;

  /** GET /api/games/{gameId}/draft/audit — returns all swap/trade audit entries, newest first. */
  @GetMapping
  public ResponseEntity<List<DraftAuditEntryResponse>> getAudit(
      @PathVariable UUID gameId,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn("DraftAuditController: unauthenticated getAudit for game {}", gameId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    log.info("Draft audit requested: game={} caller={}", gameId, user.getId());
    return ResponseEntity.ok(auditService.getAuditForGame(gameId));
  }
}
