package com.fortnite.pronos.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.core.usecase.CreateGameUseCase;
import com.fortnite.pronos.core.usecase.JoinGameUseCase;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.DraftDto;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.FlexibleAuthenticationService;
import com.fortnite.pronos.service.GameService;
import com.fortnite.pronos.service.ValidationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Game Management Controller - LEGACY
 *
 * <p>PHASE 2B: This controller now delegates to versioned APIs for backward compatibility New
 * development should use: - /api/v1/games for stable backward-compatible API - /api/v2/games for
 * enhanced features and optimizations
 *
 * <p>This controller maintains existing /api/games endpoints for legacy clients
 */
@Tag(name = "Games", description = "Fantasy league game management and participation")
@Slf4j
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

  private final GameService gameService;
  private final ValidationService validationService;
  private final FlexibleAuthenticationService flexibleAuthenticationService;

  // PHASE 2A: CLEAN ARCHITECTURE - Use Cases for business logic
  private final CreateGameUseCase createGameUseCase;
  private final JoinGameUseCase joinGameUseCase;

  /** Create a new fantasy league game */
  @Operation(
      summary = "Create a new fantasy league game",
      description = "Creates a new fantasy league game with specified configuration",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Game created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GameDto.class),
                    examples =
                        @ExampleObject(
                            name = "Successful game creation",
                            value =
                                """
                    {
                      "id": "550e8400-e29b-41d4-a716-446655440000",
                      "name": "Championship League 2025",
                      "status": "WAITING_FOR_PLAYERS",
                      "maxParticipants": 10,
                      "currentParticipants": 1,
                      "createdBy": "john.doe",
                      "createdAt": "2025-08-03T10:30:00Z",
                      "regionRules": {
                        "EU": { "min": 2, "max": 4 },
                        "NAE": { "min": 1, "max": 3 }
                      }
                    }
                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "Validation error",
                            value =
                                """
                    {
                      "success": false,
                      "message": "Validation failed",
                      "error": {
                        "code": "VALIDATION_ERROR",
                        "details": ["Game name must be between 3 and 50 characters"]
                      },
                      "timestamp": "2025-08-03T10:30:00Z"
                    }
                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                    {
                      "success": false,
                      "message": "Authentication required",
                      "error": {
                        "code": "UNAUTHORIZED",
                        "message": "Valid JWT token required"
                      },
                      "timestamp": "2025-08-03T10:30:00Z"
                    }
                    """)))
      })
  @PostMapping
  public ResponseEntity<GameDto> createGame(
      @Parameter(
              description = "Game creation request with name, settings, and region rules",
              required = true,
              schema = @Schema(implementation = CreateGameRequest.class))
          @Valid
          @RequestBody
          CreateGameRequest request) {
    log.info("Création d'une nouvelle game: {}", request.getName());

    // PHASE 2A: CLEAN ARCHITECTURE - Use case orchestrates business logic
    User currentUser = flexibleAuthenticationService.getCurrentUser();
    log.debug(
        "Utilisateur pour création de game: {} ({})",
        currentUser.getUsername(),
        flexibleAuthenticationService.getEnvironmentInfo());

    GameDto createdGame = createGameUseCase.execute(currentUser.getId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdGame);
  }

  /** Get game details by ID */
  @Operation(
      summary = "Get game details by ID",
      description =
          """
        Retrieves detailed information about a specific fantasy league game.

        **Returned Information:**
        - Game metadata (name, status, participants)
        - Draft status and current turn
        - Region distribution rules
        - Participant list with usernames
        - Game creation and modification timestamps

        **Access Control:**
        - Public games: Accessible to all authenticated users
        - Private games: Only accessible to participants
        """,
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Game details retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GameDto.class),
                    examples =
                        @ExampleObject(
                            name = "Game details response",
                            value =
                                """
                    {
                      "id": "550e8400-e29b-41d4-a716-446655440000",
                      "name": "Championship League 2025",
                      "status": "DRAFT_IN_PROGRESS",
                      "maxParticipants": 10,
                      "currentParticipants": 8,
                      "participants": {
                        "user1-id": "john.doe",
                        "user2-id": "jane.smith"
                      },
                      "draft": {
                        "status": "IN_PROGRESS",
                        "currentTurn": "user1-id",
                        "round": 2,
                        "pickNumber": 15
                      },
                      "createdAt": "2025-08-03T08:00:00Z",
                      "startedAt": "2025-08-03T09:00:00Z"
                    }
                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Game not found",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                    {
                      "success": false,
                      "message": "Game not found",
                      "error": {
                        "code": "GAME_NOT_FOUND",
                        "message": "No game found with ID: 550e8400-e29b-41d4-a716-446655440000"
                      },
                      "timestamp": "2025-08-03T10:30:00Z"
                    }
                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Access denied to private game",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                """
                    {
                      "success": false,
                      "message": "Access denied",
                      "error": {
                        "code": "ACCESS_DENIED",
                        "message": "You are not a participant in this private game"
                      },
                      "timestamp": "2025-08-03T10:30:00Z"
                    }
                    """)))
      })
  @GetMapping("/{id}")
  public ResponseEntity<GameDto> getGameById(
      @Parameter(
              description = "Unique identifier of the game",
              required = true,
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID id,
      @Parameter(
              description =
                  "Optional user parameter for development/testing (ignored in production)",
              required = false)
          @RequestParam(name = "user", required = false)
          String userParam) {
    log.debug("Récupération de la game: {} (user param: {})", id, userParam);

    try {
      // Si aucun paramètre user n'est fourni, utiliser l'authentification normale
      if (userParam == null) {
        User currentUser = flexibleAuthenticationService.getCurrentUser();
        log.debug("Utilisateur authentifié: {}", currentUser.getUsername());
      } else {
        log.debug("Paramètre user fourni: {} (ignoré pour l'instant)", userParam);
      }

      GameDto gameDto = gameService.getGameByIdOrThrow(id);
      return ResponseEntity.ok(gameDto);

    } catch (IllegalStateException e) {
      log.warn("Erreur d'authentification: {}", e.getMessage());
      // En cas d'erreur d'authentification, essayer de récupérer la game quand même
      try {
        GameDto gameDto = gameService.getGameByIdOrThrow(id);
        return ResponseEntity.ok(gameDto);
      } catch (Exception fallbackError) {
        log.error("Erreur lors de la récupération fallback de la game", fallbackError);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
      }
    } catch (IllegalArgumentException e) {
      log.warn("Game non trouvée: {}", id);
      return ResponseEntity.notFound().build();

    } catch (Exception e) {
      log.error("Erreur lors de la récupération de la game", e);
      return ResponseEntity.internalServerError().body(null);
    }
  }

  /** Récupère toutes les games */
  @GetMapping
  public ResponseEntity<List<GameDto>> getAllGames() {
    log.debug("Récupération de toutes les games");

    try {
      List<GameDto> gameDtos = gameService.getAllGames();
      return ResponseEntity.ok(gameDtos);

    } catch (Exception e) {
      log.error("Erreur lors de la récupération des games", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Récupère les games de l'utilisateur connecté Clean Code : méthode focalisée sur une seule
   * responsabilité
   */
  @GetMapping("/my-games")
  public ResponseEntity<List<GameDto>> getMyGames() {
    log.debug("Récupération des games de l'utilisateur connecté");

    try {
      // Utiliser directement le paramètre user si fourni, sinon récupérer l'utilisateur connecté
      User currentUser;
      currentUser = flexibleAuthenticationService.getCurrentUser();

      List<GameDto> userGames = gameService.getGamesByUser(currentUser.getId());

      log.info(
          "Games de l'utilisateur {} récupérées: {}", currentUser.getUsername(), userGames.size());
      return ResponseEntity.ok(userGames);

    } catch (UserNotFoundException e) {
      log.warn("Erreur d'authentification: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      log.error("Erreur lors de la récupération des games de l'utilisateur", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Récupère les games d'un utilisateur */
  @GetMapping("/user/{userId}")
  public ResponseEntity<List<GameDto>> getGamesByUser(@PathVariable UUID userId) {
    log.debug("Récupération des games de l'utilisateur: {}", userId);

    try {
      List<GameDto> games = gameService.getGamesByUser(userId);
      return ResponseEntity.ok(games);

    } catch (IllegalArgumentException e) {
      log.warn("Utilisateur non trouvé: {}", userId);
      return ResponseEntity.notFound().build();

    } catch (Exception e) {
      log.error("Erreur lors de la récupération des games de l'utilisateur", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * @deprecated Endpoint supprimé - Les games publiques n'existent plus.
   * Utilisez /api/games/my-games pour récupérer vos games.
   */
  @Deprecated
  @GetMapping("/available")
  public ResponseEntity<List<GameDto>> getAvailableGames(
      @RequestParam(name = "user", required = false) String userParam) {
    log.debug("Endpoint /available déprécié - retourne une liste vide");
    return ResponseEntity.ok(List.of());
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Permet à un utilisateur de rejoindre une game */
  @PostMapping("/join")
  public ResponseEntity<Map<String, Object>> joinGame(@RequestBody JoinGameRequest request) {
    log.info("Tentative de rejoindre la game: {}", request.getGameId());

    try {
      // Validation de la requête
      validationService.validateJoinGameRequest(request);

      // Récupération de l'utilisateur authentifié
      User currentUser = flexibleAuthenticationService.getCurrentUser();

      gameService.joinGame(currentUser.getId(), request);

      return ResponseEntity.ok(
          Map.of("success", true, "message", "Utilisateur rejoint la game avec succès"));

    } catch (IllegalStateException e) {
      log.warn("Erreur d'authentification: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Erreur lors de la tentative de rejoindre la game", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Erreur interne du serveur"));
    }
  }

  /** Démarre le draft d'une game */
  @PostMapping("/{id}/start-draft")
  public ResponseEntity<Map<String, Object>> startDraft(@PathVariable UUID id) {
    log.info("Démarrage du draft pour la game: {}", id);

    try {
      // Récupération de l'utilisateur authentifié
      User currentUser = flexibleAuthenticationService.getCurrentUser();
      UUID userId = currentUser.getId();

      DraftDto draft = gameService.startDraft(id, userId);

      return ResponseEntity.ok(
          Map.of("success", true, "message", "Draft démarré avec succès", "draft", draft));

    } catch (IllegalStateException e) {
      log.warn("Erreur d'authentification: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    } catch (SecurityException e) {
      log.warn("Permissions insuffisantes: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
      log.warn("Game non trouvée pour démarrer le draft: {}", id);
      return ResponseEntity.notFound().build();

    } catch (Exception e) {
      log.error("Erreur lors du démarrage du draft", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Erreur interne du serveur"));
    }
  }

  /** Supprime une game */
  @DeleteMapping("/{id}")
  public ResponseEntity<Map<String, Object>> deleteGame(@PathVariable UUID id) {
    log.info("Suppression de la game: {}", id);

    try {
      gameService.deleteGame(id);

      return ResponseEntity.ok(Map.of("success", true, "message", "Game supprimée avec succès"));

    } catch (IllegalArgumentException e) {
      log.warn("Game non trouvée pour suppression: {}", id);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("success", false, "message", "Game non trouvée"));

    } catch (Exception e) {
      log.error("Erreur lors de la suppression de la game", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Erreur interne du serveur"));
    }
  }

  /** Récupère les participants d'une game */
  @GetMapping("/{id}/participants")
  public ResponseEntity<List<Map<String, Object>>> getGameParticipants(@PathVariable UUID id) {
    log.debug("Récupération des participants de la game: {}", id);

    try {
      // Récupération de l'utilisateur authentifié
      User currentUser = flexibleAuthenticationService.getCurrentUser();
      UUID userId = currentUser.getId();

      GameDto gameDto = gameService.getGameByIdOrThrow(id);

      // Convertir les participants en format attendu par le frontend
      List<Map<String, Object>> participants =
          gameDto.getParticipants().entrySet().stream()
              .map(
                  entry -> {
                    Map<String, Object> participant = new HashMap<>();
                    participant.put("userId", entry.getKey());
                    participant.put("username", entry.getValue());
                    return participant;
                  })
              .collect(Collectors.toList());

      return ResponseEntity.ok(participants);

    } catch (IllegalStateException e) {
      log.warn("Erreur d'authentification: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (IllegalArgumentException e) {
      log.warn("Game non trouvée: {}", id);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Erreur lors de la récupération des participants", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Gestionnaire d'erreurs global pour ce controller */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGlobalException(Exception e) {
    log.error("Erreur non gérée dans GameController", e);
    return ResponseEntity.internalServerError().body(Map.of("error", "Erreur interne du serveur"));
  }
}
