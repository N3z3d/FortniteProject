package com.fortnite.pronos.controller.v1;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.core.usecase.CreateGameUseCase;
import com.fortnite.pronos.core.usecase.JoinGameUseCase;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.FlexibleAuthenticationService;
import com.fortnite.pronos.service.game.GameQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PHASE 2B: API VERSIONING - Game Controller V1
 *
 * <p>Provides backward compatibility for existing API consumers New features and breaking changes
 * should be added to V2
 *
 * <p>Versioning Strategy: - V1: Current stable API, maintained for backward compatibility - V2: New
 * features, optimizations, potential breaking changes
 *
 * <p>This approach allows gradual migration of API consumers
 */
@Tag(name = "Games V1", description = "Fantasy league game management API - Version 1 (Stable)")
@Slf4j
@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameControllerV1 {

  private final CreateGameUseCase createGameUseCase;
  private final JoinGameUseCase joinGameUseCase;
  private final GameQueryService gameQueryService;
  private final FlexibleAuthenticationService flexibleAuthenticationService;

  /** Create a new fantasy league game - V1 API */
  @Operation(
      summary = "Create a new fantasy league game (V1)",
      description = "Creates a new fantasy league game with V1 API compatibility")
  @PostMapping
  public ResponseEntity<GameDto> createGame(@Valid @RequestBody CreateGameRequest request) {
    log.info("V1 API: Creating game '{}'", request.getName());

    User currentUser = flexibleAuthenticationService.getCurrentUser();
    GameDto createdGame = createGameUseCase.execute(currentUser.getId(), request);

    return ResponseEntity.status(HttpStatus.CREATED).body(createdGame);
  }

  /** Get all games - V1 API */
  @Operation(summary = "Get all games (V1)", description = "Retrieves all games with V1 API format")
  @GetMapping
  public ResponseEntity<List<GameDto>> getAllGames() {
    log.debug("V1 API: Retrieving all games");

    List<GameDto> games = gameQueryService.getAllGames();
    return ResponseEntity.ok(games);
  }

  /** Get game by ID - V1 API */
  @Operation(
      summary = "Get game by ID (V1)",
      description = "Retrieves a specific game by ID with V1 API format")
  @GetMapping("/{gameId}")
  public ResponseEntity<GameDto> getGame(@PathVariable UUID gameId) {
    log.debug("V1 API: Retrieving game {}", gameId);

    Optional<GameDto> game = gameQueryService.getGameById(gameId);
    return game.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  /** Join a game - V1 API */
  @Operation(
      summary = "Join a game (V1)",
      description = "Join a fantasy league game with V1 API compatibility")
  @PostMapping("/join")
  public ResponseEntity<Void> joinGame(@Valid @RequestBody JoinGameRequest request) {
    log.info("V1 API: User joining game {}", request.getGameId());

    User currentUser = flexibleAuthenticationService.getCurrentUser();
    boolean success = joinGameUseCase.execute(currentUser.getId(), request);

    if (success) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  /** Get user's games - V1 API */
  @Operation(
      summary = "Get user's games (V1)",
      description = "Retrieves games associated with the current user")
  @GetMapping("/my-games")
  public ResponseEntity<List<GameDto>> getUserGames() {
    log.debug("V1 API: Retrieving user games");

    User currentUser = flexibleAuthenticationService.getCurrentUser();
    List<GameDto> userGames = gameQueryService.getGamesByUser(currentUser.getId());

    return ResponseEntity.ok(userGames);
  }
}
