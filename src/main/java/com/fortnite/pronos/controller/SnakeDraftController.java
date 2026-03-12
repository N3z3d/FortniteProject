package com.fortnite.pronos.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.PlayerRecommendResponse;
import com.fortnite.pronos.dto.SnakePickRequest;
import com.fortnite.pronos.dto.SnakeTurnResponse;
import com.fortnite.pronos.dto.common.ApiResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.draft.DraftTrancheService;
import com.fortnite.pronos.service.draft.SnakeDraftService;
import com.fortnite.pronos.service.game.GameDraftService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** REST API for snake draft mode: initialize order, query current turn, submit picks, recommend. */
@Slf4j
@RestController
@RequestMapping("/api/games/{gameId}/draft/snake")
@RequiredArgsConstructor
public class SnakeDraftController {

  private final SnakeDraftService snakeDraftService;
  private final GameDraftService gameDraftService;
  private final UserResolver userResolver;
  private final DraftTrancheService draftTrancheService;

  /**
   * Initializes the snake draft for the given game.
   *
   * <p>Shuffles participants randomly and creates per-region cursors (or a single GLOBAL cursor).
   * Returns the first turn.
   */
  @PostMapping("/initialize")
  public ResponseEntity<ApiResponse<SnakeTurnResponse>> initializeCursors(
      @PathVariable UUID gameId,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn("SnakeDraftController: unauthenticated initialize attempt for game {}", gameId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    log.info("Snake draft: initializing cursors for game {} by user {}", gameId, user.getId());
    SnakeTurnResponse firstTurn = snakeDraftService.initializeCursors(gameId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(firstTurn, "Snake draft initialized."));
  }

  /**
   * Returns the current turn for the given game and region.
   *
   * @return 200 with the current turn, or 404 if the draft has not been initialized yet
   */
  @GetMapping("/turn")
  public ResponseEntity<ApiResponse<SnakeTurnResponse>> getCurrentTurn(
      @PathVariable UUID gameId, @RequestParam(defaultValue = "GLOBAL") String region) {

    return snakeDraftService
        .getCurrentTurn(gameId, region)
        .map(turn -> ResponseEntity.ok(ApiResponse.success(turn)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Returns the recommended player for the current draft slot.
   *
   * <p>Only meaningful when tranches are enabled. Returns the best available player whose tranche
   * floor is at or above the current slot's required floor.
   *
   * @return 200 with the recommended player, or 404 if none is available
   */
  @GetMapping("/recommend")
  public ResponseEntity<ApiResponse<PlayerRecommendResponse>> recommend(
      @PathVariable UUID gameId, @RequestParam(defaultValue = "GLOBAL") String region) {

    return draftTrancheService
        .recommendPlayer(gameId, region)
        .map(p -> ResponseEntity.ok(ApiResponse.success(p)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Submits a snake draft pick.
   *
   * <p>Validates the tranche floor rule, then validates it is the caller's turn, advances the
   * cursor, broadcasts the next turn via WebSocket, and delegates pick recording to {@code
   * GameDraftService.selectPlayer()}.
   *
   * @return 200 with the next turn, 401 if unauthenticated, 400 if tranche violated, 403 if not the
   *     caller's turn
   */
  @PostMapping("/pick")
  public ResponseEntity<ApiResponse<SnakeTurnResponse>> processPick(
      @PathVariable UUID gameId,
      @Valid @RequestBody SnakePickRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn("SnakeDraftController: unauthenticated pick attempt for game {}", gameId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    log.info(
        "Snake draft: pick by user {} in game {} (region={}, player={})",
        user.getId(),
        gameId,
        request.getRegion(),
        request.getPlayerId());

    draftTrancheService.validatePick(gameId, request.getRegion(), request.getPlayerId());

    SnakeTurnResponse nextTurn =
        snakeDraftService.validateAndAdvance(gameId, user.getId(), request.getRegion());

    gameDraftService.selectPlayer(gameId, user.getId(), request.getPlayerId());

    return ResponseEntity.ok(ApiResponse.success(nextTurn));
  }
}
