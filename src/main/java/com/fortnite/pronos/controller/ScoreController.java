package com.fortnite.pronos.controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.application.usecase.ScoreCommandUseCase;
import com.fortnite.pronos.application.usecase.ScoreQueryUseCase;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/scores")
@RequiredArgsConstructor
@Slf4j
public class ScoreController {

  private final ScoreQueryUseCase scoreQueryUseCase;
  private final ScoreCommandUseCase scoreCommandUseCase;
  private final UserResolver userResolver;

  @GetMapping
  public ResponseEntity<List<Score>> getAllScores() {
    return ResponseEntity.ok(scoreQueryUseCase.getAllScores());
  }

  @GetMapping("/user/{userId}/latest")
  public ResponseEntity<List<Score>> getUserLatestScores(@PathVariable UUID userId) {
    return ResponseEntity.ok(scoreQueryUseCase.getUserLatestScores(userId));
  }

  @GetMapping("/player/{playerId}/history")
  public ResponseEntity<Map<UUID, List<Score>>> getPlayerScoreHistory(@PathVariable UUID playerId) {
    return ResponseEntity.ok(scoreQueryUseCase.getPlayerScoreHistory(playerId));
  }

  @PostMapping("/player/{playerId}")
  public ResponseEntity<Void> updatePlayerScore(
      @PathVariable UUID playerId,
      @RequestBody ScoreUpdateRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    HttpStatus authorizationStatus = resolveWriteAuthorizationStatus(username, httpRequest);
    if (authorizationStatus != null) {
      return ResponseEntity.status(authorizationStatus).build();
    }
    scoreCommandUseCase.updatePlayerScores(playerId, request.points(), request.timestamp());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/batch")
  public ResponseEntity<Void> updateBatchPlayerScores(
      @RequestBody BatchScoreUpdateRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    HttpStatus authorizationStatus = resolveWriteAuthorizationStatus(username, httpRequest);
    if (authorizationStatus != null) {
      return ResponseEntity.status(authorizationStatus).build();
    }
    scoreCommandUseCase.updateBatchPlayerScores(request.playerScores(), request.timestamp());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/recalculate/season/{season}")
  public ResponseEntity<Void> recalculateSeasonScores(
      @PathVariable int season,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    HttpStatus authorizationStatus = resolveWriteAuthorizationStatus(username, httpRequest);
    if (authorizationStatus != null) {
      return ResponseEntity.status(authorizationStatus).build();
    }
    scoreCommandUseCase.recalculateSeasonScores(season);
    return ResponseEntity.ok().build();
  }

  @PostMapping
  public ResponseEntity<Score> createScore(
      @RequestBody Score score,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    HttpStatus authorizationStatus = resolveWriteAuthorizationStatus(username, httpRequest);
    if (authorizationStatus != null) {
      return ResponseEntity.status(authorizationStatus).build();
    }
    return ResponseEntity.ok(scoreCommandUseCase.saveScore(score));
  }

  @DeleteMapping("/player/{playerId}")
  public ResponseEntity<Void> deletePlayerScores(
      @PathVariable UUID playerId,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    HttpStatus authorizationStatus = resolveWriteAuthorizationStatus(username, httpRequest);
    if (authorizationStatus != null) {
      return ResponseEntity.status(authorizationStatus).build();
    }
    scoreCommandUseCase.deleteScore(playerId);
    return ResponseEntity.noContent().build();
  }

  private HttpStatus resolveWriteAuthorizationStatus(String username, HttpServletRequest request) {
    User currentUser = userResolver.resolve(username, request);
    if (currentUser == null) {
      return HttpStatus.UNAUTHORIZED;
    }
    if (!User.UserRole.ADMIN.equals(currentUser.getRole())) {
      log.warn(
          "Acces refuse endpoint write score pour utilisateur non-admin: username={}, role={}",
          currentUser.getUsername(),
          currentUser.getRole());
      return HttpStatus.FORBIDDEN;
    }
    return null;
  }

  // DTOs for request bodies
  record ScoreUpdateRequest(int points, OffsetDateTime timestamp) {}

  record BatchScoreUpdateRequest(Map<UUID, Integer> playerScores, OffsetDateTime timestamp) {}
}
