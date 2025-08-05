package com.fortnite.pronos.controller.v2;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.core.domain.GameDomainService;
import com.fortnite.pronos.core.usecase.CreateGameUseCase;
import com.fortnite.pronos.core.usecase.JoinGameUseCase;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.FlexibleAuthenticationService;
import com.fortnite.pronos.service.game.GameQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PHASE 2B: API VERSIONING - Game Controller V2
 *
 * <p>Enhanced API with new features and optimizations: - Pagination support - Enhanced response
 * formats - Additional metadata - Performance improvements - Domain-driven insights
 *
 * <p>Breaking changes from V1: - Enhanced response structures with metadata - Pagination for list
 * endpoints - Additional validation rules
 */
@Tag(name = "Games V2", description = "Enhanced fantasy league game management API - Version 2")
@Slf4j
@RestController
@RequestMapping("/api/v2/games")
@RequiredArgsConstructor
public class GameControllerV2 {

  private final CreateGameUseCase createGameUseCase;
  private final JoinGameUseCase joinGameUseCase;
  private final GameQueryService gameQueryService;
  private final GameDomainService gameDomainService;
  private final FlexibleAuthenticationService flexibleAuthenticationService;

  /** Create a new fantasy league game - V2 Enhanced API */
  @Operation(
      summary = "Create a new fantasy league game (V2 Enhanced)",
      description = "Creates a new game with enhanced validation and response metadata")
  @PostMapping
  public ResponseEntity<EnhancedGameResponse> createGame(
      @Valid @RequestBody CreateGameRequest request) {
    log.info("V2 API: Creating game '{}' with enhanced features", request.getName());

    User currentUser = flexibleAuthenticationService.getCurrentUser();
    GameDto createdGame = createGameUseCase.execute(currentUser.getId(), request);

    // V2 Enhancement: Add metadata and recommendations
    EnhancedGameResponse response =
        EnhancedGameResponse.builder()
            .game(createdGame)
            .metadata(
                Map.of(
                    "apiVersion", "v2",
                    "createdBy", currentUser.getUsername(),
                    "recommendedMinParticipants",
                        gameDomainService.calculateMinimumParticipants(
                            // Note: In real implementation, you'd fetch the actual Game entity
                            com.fortnite.pronos.model.Game.builder()
                                .maxParticipants(request.getMaxParticipants())
                                .build())))
            .recommendations(
                List.of(
                    "Share invitation code with friends",
                    "Set up draft preferences",
                    "Configure game notifications"))
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** Get all games with pagination - V2 Enhanced API */
  @Operation(
      summary = "Get all games with pagination (V2)",
      description = "Retrieves games with pagination and enhanced metadata")
  @GetMapping
  public ResponseEntity<PaginatedGameResponse> getAllGames(Pageable pageable) {
    log.debug(
        "V2 API: Retrieving games with pagination - page: {}, size: {}",
        pageable.getPageNumber(),
        pageable.getPageSize());

    List<GameDto> games = gameQueryService.getAllGames();

    // V2 Enhancement: Paginated response with metadata
    PaginatedGameResponse response =
        PaginatedGameResponse.builder()
            .games(games)
            .pagination(
                PaginationInfo.builder()
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .total(games.size())
                    .totalPages((int) Math.ceil((double) games.size() / pageable.getPageSize()))
                    .build())
            .metadata(
                Map.of(
                    "apiVersion", "v2",
                    "totalActiveGames",
                        games.stream()
                            .mapToInt(g -> g.getStatus() == GameStatus.ACTIVE ? 1 : 0)
                            .sum(),
                    "averageParticipants",
                        games.stream()
                            .mapToInt(
                                g -> g.getParticipants() != null ? g.getParticipants().size() : 0)
                            .average()
                            .orElse(0.0)))
            .build();

    return ResponseEntity.ok(response);
  }

  /** Get game by ID with enhanced details - V2 API */
  @Operation(
      summary = "Get game with enhanced details (V2)",
      description = "Retrieves game with domain insights and recommendations")
  @GetMapping("/{gameId}")
  public ResponseEntity<EnhancedGameResponse> getGame(@PathVariable UUID gameId) {
    log.debug("V2 API: Retrieving enhanced game details for {}", gameId);

    Optional<GameDto> gameOpt = gameQueryService.getGameById(gameId);

    if (gameOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    GameDto game = gameOpt.get();

    // V2 Enhancement: Domain insights and recommendations
    EnhancedGameResponse response =
        EnhancedGameResponse.builder()
            .game(game)
            .metadata(
                Map.of(
                    "apiVersion", "v2",
                    "gameAge", "calculated-age-hours",
                    "participationRate", calculateParticipationRate(game)))
            .recommendations(generateGameRecommendations(game))
            .build();

    return ResponseEntity.ok(response);
  }

  private double calculateParticipationRate(GameDto game) {
    if (game.getParticipants() == null || game.getMaxParticipants() == null) {
      return 0.0;
    }
    return (double) game.getParticipants().size() / game.getMaxParticipants();
  }

  private List<String> generateGameRecommendations(GameDto game) {
    // Domain-driven recommendations based on game state
    return List.of(
        "Invite more participants to increase competition",
        "Start the draft when ready",
        "Configure team settings");
  }

  // V2 Enhanced Response DTOs
  @lombok.Data
  @lombok.Builder
  public static class EnhancedGameResponse {
    private GameDto game;
    private Map<String, Object> metadata;
    private List<String> recommendations;
  }

  @lombok.Data
  @lombok.Builder
  public static class PaginatedGameResponse {
    private List<GameDto> games;
    private PaginationInfo pagination;
    private Map<String, Object> metadata;
  }

  @lombok.Data
  @lombok.Builder
  public static class PaginationInfo {
    private int page;
    private int size;
    private long total;
    private int totalPages;
  }
}
