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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.core.usecase.CreateGameUseCase;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.DraftDto;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.GameService;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.ValidationService;

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

/** Game Management Controller - Handles fantasy league game operations. */
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
  private final UserResolver userResolver;
  private final CreateGameUseCase createGameUseCase;

  @Operation(
      summary = "Create a new fantasy league game",
      description = "Creates a new fantasy league game with specified configuration",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
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
                    {"id": "550e8400-e29b-41d4-a716-446655440000", "name": "Championship League 2025", "status": "CREATING"}
                    """))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
      })
  @PostMapping
  public ResponseEntity<GameDto> createGame(
      @Parameter(description = "Game creation request", required = true) @Valid @RequestBody
          CreateGameRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.info("Creation d'une nouvelle game: {}", request.getName());

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    UUID creatorId = request.getCreatorId() != null ? request.getCreatorId() : user.getId();
    request.setCreatorId(creatorId);

    GameDto createdGame = createGameUseCase.execute(creatorId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdGame);
  }

  @Operation(
      summary = "Get user's games",
      description = "Retrieves all games that the authenticated user is participating in",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User games retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
      })
  @GetMapping("/my-games")
  public ResponseEntity<List<GameDto>> getUserGames(
      @RequestParam(name = "user", required = false) String userParam,
      HttpServletRequest httpRequest) {
    log.debug("Recuperation des games de l'utilisateur");

    User user = userResolver.resolve(userParam, httpRequest);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    List<GameDto> userGames = gameService.getGamesByUser(user.getId());
    return ResponseEntity.ok(userGames);
  }

  @Operation(
      summary = "Get game details by ID",
      description = "Retrieves detailed information about a specific fantasy league game",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Game details retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GameDto.class))),
        @ApiResponse(responseCode = "404", description = "Game not found"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
      })
  @GetMapping("/{id:" + UUID_PATH_PATTERN + "}")
  public ResponseEntity<GameDto> getGameById(
      @PathVariable UUID id,
      @RequestParam(name = "user", required = false) String userParam,
      HttpServletRequest httpRequest) {
    log.debug("Recuperation de la game: {}", id);

    User caller = userResolver.resolve(userParam, httpRequest);
    if (caller == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    GameDto gameDto = gameService.getGameByIdOrThrow(id);
    return ResponseEntity.ok(gameDto);
  }

  @Operation(
      summary = "Get all games",
      description = "Lists all games, optionally filtered by user")
  @GetMapping
  public ResponseEntity<List<GameDto>> getAllGames(
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.debug("Recuperation des games (filtre utilisateur: {})", username != null ? username : "-");

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    if (username != null && !username.isBlank()) {
      return ResponseEntity.ok(gameService.getGamesByUser(user.getId()));
    }
    return ResponseEntity.ok(gameService.getAllGames());
  }

  @Operation(summary = "Get available games", description = "Lists games with available slots")
  @GetMapping("/available")
  public ResponseEntity<List<GameDto>> getAvailableGames(
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.debug("Recuperation des games disponibles");

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return ResponseEntity.ok(gameService.getAvailableGames());
  }

  @Operation(summary = "Join a game", description = "Allows a user to join an existing game")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successfully joined game"),
        @ApiResponse(responseCode = "400", description = "Cannot join game"),
        @ApiResponse(responseCode = "404", description = "Game not found"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
      })
  @PostMapping("/join")
  public ResponseEntity<Map<String, Object>> joinGame(
      @RequestBody JoinGameRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.info("Tentative de rejoindre la game: {}", request.getGameId());

    User user = userResolver.resolve(username, httpRequest);
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
  }

  @Operation(summary = "Start draft", description = "Starts the draft phase for a game")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Draft started successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to start draft"),
        @ApiResponse(responseCode = "404", description = "Game not found"),
        @ApiResponse(responseCode = "409", description = "Invalid game state for draft")
      })
  @PostMapping({"/{id:" + UUID_PATH_PATTERN + "}/start-draft"})
  public ResponseEntity<Map<String, Object>> startDraft(
      @PathVariable UUID id,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.info("Demarrage du draft pour la game: {}", id);

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilisateur requis"));
    }

    GameDto gameDto = gameService.getGameByIdOrThrow(id);
    if (!user.getId().equals(gameDto.getCreatorId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "Utilisateur non autorise pour ce draft"));
    }

    DraftDto draft = gameService.startDraft(id, user.getId());
    return ResponseEntity.ok(
        Map.of("success", true, "message", "Draft demarree avec succes", "draft", draft));
  }

  @Operation(summary = "Delete game", description = "Deletes a game permanently")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Game deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Game not found")
      })
  @DeleteMapping("/{id:" + UUID_PATH_PATTERN + "}")
  public ResponseEntity<Map<String, Object>> deleteGame(@PathVariable UUID id) {
    log.info("Suppression de la game: {}", id);
    gameService.deleteGame(id);
    return ResponseEntity.ok(Map.of("success", true, "message", "Game supprimee avec succes"));
  }

  @Operation(
      summary = "Get game participants",
      description = "Retrieves the list of participants for a game")
  @GetMapping("/{id:" + UUID_PATH_PATTERN + "}/participants")
  public ResponseEntity<List<Map<String, Object>>> getGameParticipants(@PathVariable UUID id) {
    log.debug("Recuperation des participants de la game: {}", id);

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
  }
}
