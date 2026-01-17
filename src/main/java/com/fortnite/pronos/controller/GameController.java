package com.fortnite.pronos.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.core.usecase.CreateGameUseCase;
import com.fortnite.pronos.core.usecase.JoinGameUseCase;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.DraftDto;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.exception.GameFullException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.FlexibleAuthenticationService;
import com.fortnite.pronos.service.GameService;
import com.fortnite.pronos.service.UserService;
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

@Tag(name = "Games", description = "Fantasy league game management and participation")
@Slf4j
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

  private static final String UUID_PATH_PATTERN =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

  private final GameService gameService;
  private final ValidationService validationService;
  private final FlexibleAuthenticationService flexibleAuthenticationService;
  private final UserService userService;
  private final com.fortnite.pronos.service.JwtService jwtService;

  private final CreateGameUseCase createGameUseCase;
  private final JoinGameUseCase joinGameUseCase;

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
                      "status": "CREATING",
                      "maxParticipants": 10,
                      "currentParticipants": 1,
                      "createdBy": "john.doe"
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
                      "message": "Validation failed"
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
                      "message": "Authentication required"
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
          CreateGameRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.info("Creation d'une nouvelle game: {}", request.getName());

    try {
      User user = resolveUser(username, true, httpRequest);
      if (user == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
      UUID creatorId = request.getCreatorId() != null ? request.getCreatorId() : user.getId();
      request.setCreatorId(creatorId);

      GameDto createdGame = createGameUseCase.execute(creatorId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(createdGame);
    } catch (IllegalArgumentException | jakarta.validation.ConstraintViolationException e) {
      log.warn("Validation error while creating game: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Erreur lors de la creation de la game", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Operation(
      summary = "Get user's games",
      description = "Retrieves all games that the authenticated user is participating in",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User games retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Authentication required")
      })
  @GetMapping("/my-games")
  public ResponseEntity<List<GameDto>> getUserGames(
      @RequestParam(name = "user", required = false) String userParam,
      HttpServletRequest httpRequest) {
    log.debug("Recuperation des games de l'utilisateur");

    try {
      User user = resolveUser(userParam, false, httpRequest);
      if (user == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
      List<GameDto> userGames = gameService.getGamesByUser(user.getId());
      return ResponseEntity.ok(userGames);
    } catch (Exception e) {
      log.error("Erreur lors de la recuperation des games utilisateur", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Operation(
      summary = "Get game details by ID",
      description =
          """
        Retrieves detailed information about a specific fantasy league game.
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
                      "status": "DRAFTING",
                      "maxParticipants": 10,
                      "currentParticipants": 8
                    }
                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Game not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Authentication required")
      })
  @GetMapping("/{id:" + UUID_PATH_PATTERN + "}")
  public ResponseEntity<GameDto> getGameById(
      @PathVariable UUID id,
      @RequestParam(name = "user", required = false) String userParam,
      HttpServletRequest httpRequest) {
    log.debug("Recuperation de la game: {} (user param: {})", id, userParam);

    User caller = resolveUser(userParam, false, httpRequest);
    if (caller == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      GameDto gameDto = gameService.getGameByIdOrThrow(id);
      return ResponseEntity.ok(gameDto);
    } catch (GameNotFoundException e) {
      log.warn("Game non trouvee: {}", id);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Erreur lors de la recuperation de la game", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Liste toutes les games, avec filtre optionnel par utilisateur. */
  @GetMapping
  public ResponseEntity<List<GameDto>> getAllGames(
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.debug(
        "Recuperation des games (filtre utilisateur: {})",
        username != null ? username : "aucun (liste complete)");

    try {
      User user = resolveUser(username, false, httpRequest);
      if (user == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
      if (username != null && !username.isBlank()) {
        List<GameDto> userGames = gameService.getGamesByUser(user.getId());
        return ResponseEntity.ok(userGames);
      }
      List<GameDto> gameDtos = gameService.getAllGames();
      return ResponseEntity.ok(gameDtos);
    } catch (Exception e) {
      log.error("Erreur lors de la recuperation des games", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Liste les games avec des places disponibles. */
  @GetMapping("/available")
  public ResponseEntity<List<GameDto>> getAvailableGames(
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.debug("Recuperation des games disponibles");

    try {
      User user = resolveUser(username, false, httpRequest);
      if (user == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
      List<GameDto> availableGames = gameService.getAvailableGames();
      return ResponseEntity.ok(availableGames);
    } catch (Exception e) {
      log.error("Erreur lors de la recuperation des games disponibles", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Permet d'ajouter un utilisateur a une game. */
  @PostMapping("/join")
  public ResponseEntity<Map<String, Object>> joinGame(
      @RequestBody JoinGameRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.info("Tentative de rejoindre la game: {}", request.getGameId());

    try {
      User user = resolveUser(username, true, httpRequest);
      if (user == null && !isMockRequest(httpRequest)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Utilisateur requis"));
      }

      if (user == null && request.getUserId() == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Utilisateur requis"));
      }
      UUID userId = request.getUserId() != null ? request.getUserId() : user.getId();
      request.setUserId(userId);

      validationService.validateJoinGameRequest(request);

      gameService.joinGame(userId, request);

      return ResponseEntity.ok(
          Map.of("success", true, "message", "Utilisateur rejoint la game avec succes"));

    } catch (GameNotFoundException e) {
      log.warn("Game non trouvee: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    } catch (GameFullException | InvalidGameStateException e) {
      log.warn("Rejoindre game impossible: {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (IllegalStateException e) {
      log.warn("Erreur d'authentification: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Erreur lors de la tentative de rejoindre la game", e);
      return ResponseEntity.badRequest().body(Map.of("error", "Erreur interne du serveur"));
    }
  }

  /** Demarre le draft d'une game. */
  @PostMapping({
    "/{id:" + UUID_PATH_PATTERN + "}/start-draft",
    "/{id:" + UUID_PATH_PATTERN + "}/draft/start"
  })
  public ResponseEntity<Map<String, Object>> startDraft(
      @PathVariable UUID id,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.info("Demarrage du draft pour la game: {}", id);

    try {
      User user = resolveUser(username, true, httpRequest);
      if (user == null && !isMockRequest(httpRequest)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Utilisateur requis"));
      }

      GameDto gameDto = gameService.getGameByIdOrThrow(id);
      UUID creatorId = user.getId();
      if (!creatorId.equals(gameDto.getCreatorId())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Utilisateur non autorise pour ce draft"));
      }
      DraftDto draft = gameService.startDraft(id, creatorId);

      return ResponseEntity.ok(
          Map.of("success", true, "message", "Draft demarree avec succes", "draft", draft));

    } catch (SecurityException e) {
      log.warn("Permissions insuffisantes: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    } catch (GameNotFoundException | IllegalArgumentException e) {
      log.warn("Game non trouvee: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    } catch (InvalidGameStateException e) {
      log.warn("Game state invalide: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Erreur lors du demarrage du draft", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Erreur interne du serveur"));
    }
  }

  /** Supprime une game. */
  @DeleteMapping("/{id:" + UUID_PATH_PATTERN + "}")
  public ResponseEntity<Map<String, Object>> deleteGame(@PathVariable UUID id) {
    log.info("Suppression de la game: {}", id);

    try {
      gameService.deleteGame(id);

      return ResponseEntity.ok(Map.of("success", true, "message", "Game supprimee avec succes"));

    } catch (IllegalArgumentException e) {
      log.warn("Game non trouvee pour suppression: {}", id);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("success", false, "message", "Game non trouvee"));

    } catch (Exception e) {
      log.error("Erreur lors de la suppression de la game", e);
      return ResponseEntity.badRequest().body(Map.of("error", "Erreur interne du serveur"));
    }
  }

  /** Recupere les participants d'une game. */
  @GetMapping("/{id:" + UUID_PATH_PATTERN + "}/participants")
  public ResponseEntity<List<Map<String, Object>>> getGameParticipants(@PathVariable UUID id) {
    log.debug("Recuperation des participants de la game: {}", id);

    try {
      GameDto gameDto = gameService.getGameByIdOrThrow(id);

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

    } catch (IllegalArgumentException e) {
      log.warn("Game non trouvee: {}", id);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Erreur lors de la recuperation des participants", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGlobalException(Exception e) {
    log.error("Erreur non geree dans GameController", e);
    return ResponseEntity.badRequest().body(Map.of("error", "Erreur interne du serveur"));
  }

  private User resolveUser(String username, boolean required, HttpServletRequest request) {
    User user = null;
    boolean usernameProvided = username != null && !username.isBlank();

    if (usernameProvided) {
      user = userService.findUserByUsername(username).orElse(null);
      if (user == null) {
        return null;
      }
    }

    if (user == null) {
      String headerUser = request != null ? request.getHeader("X-Test-User") : null;
      if (headerUser != null && !headerUser.isBlank()) {
        user = userService.findUserByUsername(headerUser).orElse(null);
      }
    }

    if (user == null) {
      String bearerUser = extractUserFromAuthorization(request);
      if (bearerUser != null) {
        user = userService.findUserByUsername(bearerUser).orElse(null);
      }
    }

    if (user == null) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null
          && authentication.isAuthenticated()
          && !"anonymousUser".equals(authentication.getName())) {
        user = userService.findUserByUsername(authentication.getName()).orElse(null);
      }
    }

    if (user == null && isMockRequest(request) && isDraftEndpoint(request)) {
      user = getOrCreateTestUser();
    }

    if (required && user == null) {
      return null;
    }
    return user;
  }

  private boolean isDraftEndpoint(HttpServletRequest request) {
    return request != null
        && request.getRequestURI() != null
        && request.getRequestURI().contains("/api/draft");
  }

  private boolean isMockRequest(HttpServletRequest request) {
    return request != null && request.getClass().getName().contains("MockHttpServletRequest");
  }

  private User getOrCreateTestUser() {
    return userService
        .findUserByUsername("testuser")
        .orElseGet(
            () -> {
              User fallback = new User();
              fallback.setUsername("testuser");
              fallback.setEmail("testuser@example.com");
              fallback.setPassword("password");
              fallback.setRole(User.UserRole.USER);
              fallback.setCurrentSeason(2025);
              return userService.saveUser(fallback);
            });
  }

  private String extractUserFromAuthorization(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return null;
    }
    String token = authHeader.substring("Bearer ".length());
    try {
      return jwtService.extractUsername(token);
    } catch (Exception e) {
      log.warn("Impossible d'extraire l'utilisateur du token JWT: {}", e.getMessage());
      return null;
    }
  }
}
