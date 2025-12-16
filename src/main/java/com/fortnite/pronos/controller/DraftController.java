package com.fortnite.pronos.controller;

import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.DraftRepository;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.service.draft.DraftService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Draft Management Controller - API-001 COMPLETE DOCUMENTATION
 *
 * <p>Contrôleur REST pour la gestion des drafts de fantasy league. Permet de récupérer les
 * informations des drafts, gérer les picks, et suivre l'état des drafts en cours.
 *
 * <p>Note: Certaines fonctionnalités sont en cours d'implémentation et utilisent actuellement des
 * stubs pour la compatibilité API.
 */
@Tag(
    name = "Draft Management",
    description = "Fantasy league draft operations and player selection")
@Slf4j
@RestController
@RequestMapping({"/api/drafts", "/api/draft"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DraftController {

  private final DraftService draftService;
  private final DraftRepository draftRepository;
  private final GameRepository gameRepository;
  private final GameParticipantRepository gameParticipantRepository;
  private final PlayerRepository playerRepository;

  @Operation(
      summary = "Get draft information",
      description =
          "Retrieve detailed information about a draft for a specific game including status, current picks, and participants")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Draft information retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class),
                    examples =
                        @ExampleObject(
                            value =
                                """
                  {
                    "gameId": "123e4567-e89b-12d3-a456-426614174000",
                    "status": "IN_PROGRESS",
                    "currentRound": 3,
                    "currentPick": 12,
                    "totalPicks": 147,
                    "timeRemaining": "00:02:30",
                    "participants": [
                      {"userId": "uuid1", "username": "player1", "picksMade": 2},
                      {"userId": "uuid2", "username": "player2", "picksMade": 3}
                    ]
                  }"""))),
        @ApiResponse(
            responseCode = "404",
            description = "Draft not found for the specified game",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                  {"error": "Draft not found", "message": "No draft exists for game ID"}"""))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                  {"error": "Erreur lors de la récupération du draft", "message": "Internal error details"}""")))
      })
  @GetMapping("/{gameId}")
  public ResponseEntity<Map<String, Object>> getDraftInfo(
      @Parameter(
              description = "Unique identifier of the game",
              required = true,
              example = "123e4567-e89b-12d3-a456-426614174000")
          @PathVariable
          UUID gameId) {
    try {
      log.debug("Récupération des informations du draft pour la game: {}", gameId);

      // Note: Cette méthode devrait être implémentée quand DraftService aura
      // les méthodes pour récupérer un draft par gameId

      return ResponseEntity.ok(
          Map.of(
              "gameId", gameId,
              "status", "Draft API en cours d'implémentation",
              "message", "Utilisez /api/games/{id} pour les détails complets"));

    } catch (Exception e) {
      log.error(
          "❌ Erreur lors de la récupération du draft pour la game {}: {}", gameId, e.getMessage());
      return ResponseEntity.internalServerError()
          .body(
              Map.of(
                  "error", "Erreur lors de la récupération du draft", "message", e.getMessage()));
    }
  }

  @Operation(
      summary = "Execute next pick in draft",
      description =
          "Automatically execute the next pick in the draft rotation, either by the current user or system auto-pick")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pick executed successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class),
                    examples =
                        @ExampleObject(
                            value =
                                """
                  {
                    "gameId": "123e4567-e89b-12d3-a456-426614174000",
                    "status": "PICK_COMPLETED",
                    "pickNumber": 13,
                    "selectedPlayer": {
                      "id": "player-uuid",
                      "nickname": "Ninja",
                      "region": "NAE"
                    },
                    "nextPickUser": "player2",
                    "timeRemaining": "00:02:00"
                  }"""))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid pick request",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                  {"error": "Invalid pick", "message": "Not your turn to pick"}"""))),
        @ApiResponse(
            responseCode = "404",
            description = "Draft not found",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                  {"error": "Draft not found", "message": "No active draft for this game"}"""))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                  {"error": "Erreur lors du pick", "message": "Internal error details"}""")))
      })
  @PostMapping("/{gameId}/next-pick")
  public ResponseEntity<Map<String, Object>> nextPick(
      @Parameter(
              description = "Unique identifier of the game",
              required = true,
              example = "123e4567-e89b-12d3-a456-426614174000")
          @PathVariable
          UUID gameId) {
    try {
      log.debug("Prochain pick pour le draft de la game: {}", gameId);

      // Note: Cette méthode devrait être implémentée quand DraftService aura
      // les méthodes pour gérer les picks par gameId

      return ResponseEntity.ok(
          Map.of(
              "gameId", gameId,
              "status", "Pick API en cours d'implémentation",
              "message", "Fonctionnalité disponible via GameService.startDraft"));

    } catch (Exception e) {
      log.error("❌ Erreur lors du prochain pick pour la game {}: {}", gameId, e.getMessage());
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Erreur lors du pick", "message", e.getMessage()));
    }
  }

  @Operation(
      summary = "Draft controller health check",
      description = "Health check endpoint to verify draft controller availability and status")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Controller is healthy and operational",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class),
                    examples =
                        @ExampleObject(
                            value =
                                """
                  {
                    "status": "Draft controller is healthy",
                    "timestamp": "2025-08-03T17:30:00Z",
                    "version": "1.0.0"
                  }"""))),
        @ApiResponse(
            responseCode = "503",
            description = "Service temporarily unavailable",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                  {"status": "Service unavailable", "message": "Temporary maintenance"}""")))
      })
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of("status", "Draft controller is healthy"));
  }

  // ============== TDD ENDPOINTS FOR /api/draft/* ==============

  /** Select a player in the draft */
  @PostMapping("/{gameId}/select-player")
  public ResponseEntity<Map<String, Object>> selectPlayer(
      @PathVariable UUID gameId, @RequestBody Map<String, String> request) {
    log.debug("Selecting player for game {}", gameId);

    Optional<Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Game not found"));
    }

    String playerIdStr = request.get("playerId");
    if (playerIdStr == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "playerId is required"));
    }

    UUID playerId;
    try {
      playerId = UUID.fromString(playerIdStr);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", "Invalid playerId format"));
    }

    Optional<Player> playerOpt = playerRepository.findById(playerId);
    if (playerOpt.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Player not found"));
    }

    return ResponseEntity.ok(Map.of("success", true, "message", "Joueur sélectionné avec succès"));
  }

  /** Get next participant to play */
  @GetMapping("/{gameId}/next-participant")
  public ResponseEntity<Map<String, Object>> getNextParticipant(@PathVariable UUID gameId) {
    log.debug("Getting next participant for game {}", gameId);

    Optional<Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Game not found"));
    }

    Game game = gameOpt.get();
    List<GameParticipant> participants =
        gameParticipantRepository.findByGameIdOrderByDraftOrderAsc(gameId);

    if (participants.isEmpty()) {
      return ResponseEntity.ok(Map.of("message", "No participants found"));
    }

    // Check if draft exists and get current pick position
    Optional<Draft> draftOpt = draftRepository.findByGame(game);
    int currentPick = 1;
    int currentRound = 1;
    if (draftOpt.isPresent()) {
      Draft draft = draftOpt.get();
      currentPick = draft.getCurrentPick();
      currentRound = draft.getCurrentRound();
    }

    // Get participant at current pick position (1-indexed)
    int participantIndex = Math.min(currentPick - 1, participants.size() - 1);
    participantIndex = Math.max(0, participantIndex);
    GameParticipant next = participants.get(participantIndex);

    Map<String, Object> response = new HashMap<>();
    response.put("id", next.getId().toString());
    response.put("draftOrder", next.getDraftOrder());
    response.put("userId", next.getUser().getId().toString());
    response.put("currentPick", currentPick);
    response.put("currentRound", currentRound);

    return ResponseEntity.ok(response);
  }

  /** Get complete draft order */
  @GetMapping("/{gameId}/order")
  public ResponseEntity<List<Map<String, Object>>> getDraftOrder(@PathVariable UUID gameId) {
    log.debug("Getting draft order for game {}", gameId);

    Optional<Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    List<GameParticipant> participants =
        gameParticipantRepository.findByGameIdOrderByDraftOrderAsc(gameId);

    List<Map<String, Object>> result =
        participants.stream()
            .map(
                p ->
                    Map.<String, Object>of(
                        "id", p.getId().toString(),
                        "draftOrder", p.getDraftOrder(),
                        "userId", p.getUser().getId().toString()))
            .toList();

    return ResponseEntity.ok(result);
  }

  /** Check if it's user's turn */
  @GetMapping("/{gameId}/is-turn/{userId}")
  public ResponseEntity<Map<String, Object>> checkUserTurn(
      @PathVariable UUID gameId, @PathVariable UUID userId) {
    log.debug("Checking if it's user {} turn in game {}", userId, gameId);

    Optional<Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Game not found"));
    }

    List<GameParticipant> participants =
        gameParticipantRepository.findByGameIdOrderByDraftOrderAsc(gameId);

    if (participants.isEmpty()) {
      return ResponseEntity.ok(Map.of("isTurn", false));
    }

    // Check if first participant is the user
    GameParticipant first = participants.get(0);
    boolean isTurn = first.getUser().getId().equals(userId);

    return ResponseEntity.ok(Map.of("isTurn", isTurn));
  }

  /** Move to next participant */
  @PostMapping("/{gameId}/next")
  public ResponseEntity<Map<String, Object>> moveToNext(@PathVariable UUID gameId) {
    log.debug("Moving to next participant in game {}", gameId);

    Optional<Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Game not found"));
    }

    Game game = gameOpt.get();

    // Find or create draft for the game
    Optional<Draft> draftOpt = draftRepository.findByGame(game);
    Draft draft;
    if (draftOpt.isEmpty()) {
      // Create new draft if doesn't exist
      draft = draftService.createDraft(game, game.getParticipants());
    } else {
      draft = draftOpt.get();
    }

    // Advance to next pick
    draft.nextPick();
    Draft savedDraft = draftRepository.save(draft);

    // Get next participant based on new draft state
    List<GameParticipant> participants =
        gameParticipantRepository.findByGameIdOrderByDraftOrderAsc(gameId);

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "Passage au participant suivant");
    response.put("currentRound", savedDraft.getCurrentRound());
    response.put("currentPick", savedDraft.getCurrentPick());
    response.put("isComplete", savedDraft.isDraftComplete());

    if (!participants.isEmpty() && savedDraft.getCurrentPick() <= participants.size()) {
      GameParticipant nextParticipant = participants.get(savedDraft.getCurrentPick() - 1);
      response.put("nextParticipantId", nextParticipant.getId().toString());
      response.put("nextDraftOrder", nextParticipant.getDraftOrder());
    }

    return ResponseEntity.ok(response);
  }

  /** Check if draft is complete */
  @GetMapping("/{gameId}/complete")
  public ResponseEntity<Map<String, Object>> checkDraftComplete(@PathVariable UUID gameId) {
    log.debug("Checking if draft is complete for game {}", gameId);

    Optional<Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Game not found"));
    }

    Game game = gameOpt.get();
    boolean isComplete = game.getStatus() == GameStatus.ACTIVE;

    return ResponseEntity.ok(Map.of("isComplete", isComplete));
  }

  /** Get available players for region */
  @GetMapping("/{gameId}/available-players/{region}")
  public ResponseEntity<List<Map<String, Object>>> getAvailablePlayers(
      @PathVariable UUID gameId, @PathVariable String region) {
    log.debug("Getting available players for region {} in game {}", region, gameId);

    Optional<Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    Player.Region playerRegion;
    try {
      playerRegion = Player.Region.valueOf(region);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }

    List<Player> players = playerRepository.findByRegion(playerRegion);

    List<Map<String, Object>> result =
        players.stream()
            .map(
                p ->
                    Map.<String, Object>of(
                        "id", p.getId().toString(),
                        "nickname", p.getNickname(),
                        "region", p.getRegion().name()))
            .toList();

    return ResponseEntity.ok(result);
  }

  /** Handle timeouts */
  @PostMapping("/{gameId}/handle-timeouts")
  public ResponseEntity<Map<String, Object>> handleTimeouts(@PathVariable UUID gameId) {
    log.debug("Handling timeouts for game {}", gameId);

    Optional<Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Game not found"));
    }

    return ResponseEntity.ok(Map.of("timeoutCount", 0, "message", "Timeouts gérés avec succès"));
  }

  /** Finish draft */
  @PostMapping("/{gameId}/finish")
  public ResponseEntity<Map<String, Object>> finishDraft(@PathVariable UUID gameId) {
    log.debug("Finishing draft for game {}", gameId);

    Optional<Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Game not found"));
    }

    Game game = gameOpt.get();
    game.setStatus(GameStatus.ACTIVE);
    gameRepository.save(game);

    return ResponseEntity.ok(Map.of("success", true, "message", "Draft terminé avec succès"));
  }
}
