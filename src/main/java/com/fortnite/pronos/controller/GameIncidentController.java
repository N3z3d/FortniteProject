package com.fortnite.pronos.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.IncidentReportRequest;
import com.fortnite.pronos.dto.common.ApiResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.admin.GameIncidentService;
import com.fortnite.pronos.service.admin.IncidentEntry;
import com.fortnite.pronos.service.admin.IncidentReportingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Endpoints for participant incident reporting and admin incident review. */
@Slf4j
@RestController
@RequiredArgsConstructor
public class GameIncidentController {

  private static final int MAX_LIMIT = 500;

  private final IncidentReportingService incidentReportingService;
  private final GameIncidentService gameIncidentService;
  private final UserResolver userResolver;

  /**
   * Submits an incident report for a game. Only authenticated participants may report.
   *
   * @return 201 Created with the recorded IncidentEntry
   */
  @PostMapping("/api/games/{gameId}/incidents")
  public ResponseEntity<ApiResponse<IncidentEntry>> reportIncident(
      @PathVariable UUID gameId,
      @Valid @RequestBody IncidentReportRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn("GameIncidentController: unauthorized incident report attempt for game {}", gameId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    IncidentEntry entry =
        incidentReportingService.reportIncident(gameId, user.getId(), user.getUsername(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(entry, "Incident reported successfully. Thank you."));
  }

  /**
   * Returns recent incidents for admin review. Optionally filtered by game.
   *
   * @return list of incidents, most recent first
   */
  @GetMapping("/api/admin/incidents")
  public ResponseEntity<ApiResponse<List<IncidentEntry>>> getIncidents(
      @RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) UUID gameId) {

    int effectiveLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
    log.info("Admin: fetching incidents (limit={}, gameId={})", effectiveLimit, gameId);

    List<IncidentEntry> incidents = gameIncidentService.getRecentIncidents(effectiveLimit, gameId);
    return ResponseEntity.ok(ApiResponse.success(incidents));
  }
}
