package com.fortnite.pronos.controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.service.ScoreService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/scores")
@RequiredArgsConstructor
public class ScoreController {

  private final ScoreService scoreService;

  @GetMapping
  public ResponseEntity<List<Score>> getAllScores() {
    return ResponseEntity.ok(scoreService.getAllScores());
  }

  @GetMapping("/user/{userId}/latest")
  public ResponseEntity<List<Score>> getUserLatestScores(@PathVariable UUID userId) {
    return ResponseEntity.ok(scoreService.getUserLatestScores(userId));
  }

  @GetMapping("/player/{playerId}/history")
  public ResponseEntity<Map<UUID, List<Score>>> getPlayerScoreHistory(@PathVariable UUID playerId) {
    return ResponseEntity.ok(scoreService.getPlayerScoreHistory(playerId));
  }

  @PostMapping("/player/{playerId}")
  public ResponseEntity<Void> updatePlayerScore(
      @PathVariable UUID playerId, @RequestBody ScoreUpdateRequest request) {
    scoreService.updatePlayerScores(playerId, request.points(), request.timestamp());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/batch")
  public ResponseEntity<Void> updateBatchPlayerScores(
      @RequestBody BatchScoreUpdateRequest request) {
    scoreService.updateBatchPlayerScores(request.playerScores(), request.timestamp());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/recalculate/season/{season}")
  public ResponseEntity<Void> recalculateSeasonScores(@PathVariable int season) {
    scoreService.recalculateSeasonScores(season);
    return ResponseEntity.ok().build();
  }

  @PostMapping
  public ResponseEntity<Score> createScore(@RequestBody Score score) {
    return ResponseEntity.ok(scoreService.saveScore(score));
  }

  @DeleteMapping("/player/{playerId}")
  public ResponseEntity<Void> deletePlayerScores(@PathVariable UUID playerId) {
    scoreService.deleteScore(playerId);
    return ResponseEntity.noContent().build();
  }

  // DTOs for request bodies
  record ScoreUpdateRequest(int points, OffsetDateTime timestamp) {}

  record BatchScoreUpdateRequest(Map<UUID, Integer> playerScores, OffsetDateTime timestamp) {}
}
