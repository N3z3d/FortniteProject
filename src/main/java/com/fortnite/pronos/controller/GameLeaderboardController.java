package com.fortnite.pronos.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.TeamDeltaLeaderboardEntryDto;
import com.fortnite.pronos.service.leaderboard.TeamDeltaLeaderboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** REST controller exposing the game leaderboard ordered by delta PR descending. */
@Slf4j
@RestController
@RequestMapping("/api/games/{gameId}/leaderboard")
@RequiredArgsConstructor
public class GameLeaderboardController {

  private final TeamDeltaLeaderboardService leaderboardService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<TeamDeltaLeaderboardEntryDto>> getLeaderboard(
      @PathVariable UUID gameId) {
    log.info("GameLeaderboard: fetching leaderboard for game={}", gameId);
    List<TeamDeltaLeaderboardEntryDto> leaderboard = leaderboardService.getLeaderboard(gameId);
    log.info("GameLeaderboard: returned {} entries for game={}", leaderboard.size(), gameId);
    return ResponseEntity.ok(leaderboard);
  }
}
