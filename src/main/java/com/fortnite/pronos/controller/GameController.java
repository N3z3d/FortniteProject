package com.fortnite.pronos.controller;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.application.usecase.GameQueryUseCase;
import com.fortnite.pronos.core.usecase.CreateGameUseCase;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.DraftDto;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.dto.JoinGameWithCodeRequest;
import com.fortnite.pronos.dto.RenameGameRequest;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.GameService;
import com.fortnite.pronos.service.InvitationCodeAttemptGuard;
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

  private static final String ERROR_KEY = "error";
  private static final String MESSAGE_KEY = "message";
  private static final String SUCCESS_KEY = "success";
  private static final String USER_REQUIRED_MESSAGE = "Utilisateur requis";

  private final GameService gameService;
  private final GameQueryUseCase gameQueryUseCase;
  private final ValidationService validationService;
  private final UserResolver userResolver;
  private final CreateGameUseCase createGameUseCase;
  private final InvitationCodeAttemptGuard invitationCodeAttemptGuard;

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
    log.info(
        "GameController: createGame requested - gameName={}, requestedUser={}",
        request.getName(),
        username != null ? username : "-");

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn(
          "GameController: createGame unauthorized - requestedUser={}, remoteAddr={}",
          username != null ? username : "-",
          httpRequest.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    UUID creatorId = user.getId();
    request.setCreatorId(creatorId);

    GameDto createdGame = createGameUseCase.execute(creatorId, request);
    log.info(
        "GameController: createGame succeeded - gameId={}, creatorId={}",
        createdGame.getId(),
        creatorId);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdGame);
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
    log.info(
        "GameController: joinGame requested - gameId={}, requestedUser={}",
        request.getGameId(),
        username != null ? username : "-");

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn(
          "GameController: joinGame unauthorized - gameId={}, requestedUser={}, remoteAddr={}",
          request.getGameId(),
          username != null ? username : "-",
          httpRequest.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of(ERROR_KEY, USER_REQUIRED_MESSAGE));
    }

    UUID userId = user.getId();
    request.setUserId(userId);

    validationService.validateJoinGameRequest(request);
    gameService.joinGame(userId, request);
    log.info(
        "GameController: joinGame succeeded - gameId={}, userId={}", request.getGameId(), userId);

    return ResponseEntity.ok(
        Map.of(SUCCESS_KEY, true, MESSAGE_KEY, "Utilisateur rejoint la game avec succes"));
  }

  @Operation(
      summary = "Join a game with invitation code",
      description = "Allows a user to join an existing game using an invitation code")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successfully joined game"),
        @ApiResponse(responseCode = "400", description = "Missing invitation code"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Game not found for invitation code")
      })
  @PostMapping("/join-with-code")
  public ResponseEntity<GameDto> joinGameWithCode(
      @Valid @RequestBody(required = false) JoinGameWithCodeRequest payload,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.info(
        "GameController: joinGameWithCode requested - requestedUser={}",
        username != null ? username : "-");

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn(
          "GameController: joinGameWithCode unauthorized - requestedUser={}, remoteAddr={}",
          username != null ? username : "-",
          httpRequest.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String invitationCode = payload == null ? null : payload.resolveInvitationCode();
    if (invitationCode == null || invitationCode.isBlank()) {
      log.warn("GameController: joinGameWithCode invalid payload - userId={}", user.getId());
      return ResponseEntity.badRequest().build();
    }

    String normalizedCode = normalizeInvitationCode(invitationCode);
    invitationCodeAttemptGuard.registerAttemptOrThrow(user.getId(), httpRequest.getRemoteAddr());
    return gameService
        .getGameByInvitationCode(normalizedCode)
        .map(game -> joinUserToGameByInvitationCode(game, user.getId(), normalizedCode))
        .orElseGet(() -> notFoundByInvitationCode(user.getId(), normalizedCode));
  }

  @Operation(
      summary = "Leave a game",
      description = "Allows a non-creator participant to leave a game")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successfully left game"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Creator cannot leave game"),
        @ApiResponse(responseCode = "404", description = "Game not found")
      })
  @PostMapping("/{id:" + UUID_PATH_PATTERN + "}/leave")
  public ResponseEntity<Map<String, Object>> leaveGame(
      @PathVariable UUID id,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.info(
        "GameController: leaveGame requested - gameId={}, requestedUser={}",
        id,
        username != null ? username : "-");

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn(
          "GameController: leaveGame unauthorized - gameId={}, requestedUser={}, remoteAddr={}",
          id,
          username != null ? username : "-",
          httpRequest.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of(ERROR_KEY, USER_REQUIRED_MESSAGE));
    }

    gameService.leaveGame(user.getId(), id);
    log.info("GameController: leaveGame succeeded - gameId={}, userId={}", id, user.getId());
    return ResponseEntity.ok(
        Map.of(SUCCESS_KEY, true, MESSAGE_KEY, "Utilisateur a quitte la game avec succes"));
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
    log.info(
        "GameController: startDraft requested - gameId={}, requestedUser={}",
        id,
        username != null ? username : "-");

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn(
          "GameController: startDraft unauthorized - gameId={}, requestedUser={}, remoteAddr={}",
          id,
          username != null ? username : "-",
          httpRequest.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of(ERROR_KEY, USER_REQUIRED_MESSAGE));
    }

    GameDto gameDto = gameQueryUseCase.getGameByIdOrThrow(id);
    ResponseEntity<Map<String, Object>> invalidStartDraftResponse =
        validateStartDraftRequest(id, user.getId(), gameDto.getCreatorId());
    if (invalidStartDraftResponse != null) {
      return invalidStartDraftResponse;
    }

    DraftDto draft = gameService.startDraft(id, user.getId());
    log.info(
        "GameController: startDraft succeeded - gameId={}, userId={}, draftId={}",
        id,
        user.getId(),
        draft.getId());
    return ResponseEntity.ok(
        Map.of(SUCCESS_KEY, true, MESSAGE_KEY, "Draft demarree avec succes", "draft", draft));
  }

  @Operation(
      summary = "Delete game",
      description = "Deletes a game permanently (only creator can delete)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Game deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to delete this game"),
        @ApiResponse(responseCode = "404", description = "Game not found"),
        @ApiResponse(responseCode = "409", description = "Cannot delete game in progress")
      })
  @DeleteMapping("/{id:" + UUID_PATH_PATTERN + "}")
  public ResponseEntity<Map<String, Object>> deleteGame(
      @PathVariable UUID id,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.info(
        "GameController: deleteGame requested - gameId={}, requestedUser={}",
        id,
        username != null ? username : "-");

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn(
          "GameController: deleteGame unauthorized - gameId={}, requestedUser={}, remoteAddr={}",
          id,
          username != null ? username : "-",
          httpRequest.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of(ERROR_KEY, USER_REQUIRED_MESSAGE));
    }

    GameDto gameDto = gameQueryUseCase.getGameByIdOrThrow(id);
    if (!user.getId().equals(gameDto.getCreatorId())) {
      log.warn(
          "GameController: deleteGame forbidden - gameId={}, userId={}, creatorId={}",
          id,
          user.getId(),
          gameDto.getCreatorId());
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of(ERROR_KEY, "Seul le createur peut supprimer cette partie"));
    }

    gameService.deleteGame(id);
    log.info("GameController: deleteGame succeeded - gameId={}, userId={}", id, user.getId());
    return ResponseEntity.ok(Map.of(SUCCESS_KEY, true, MESSAGE_KEY, "Game supprimee avec succes"));
  }

  @Operation(
      summary = "Regenerate invitation code",
      description =
          "Generates a new invitation code with optional duration (24h, 48h, 7d, permanent)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Code regenerated successfully"),
        @ApiResponse(responseCode = "404", description = "Game not found")
      })
  @PostMapping("/{id:" + UUID_PATH_PATTERN + "}/regenerate-code")
  public ResponseEntity<GameDto> regenerateInvitationCode(
      @PathVariable UUID id,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest,
      @RequestParam(name = "duration", required = false, defaultValue = "permanent")
          String duration) {
    log.info(
        "GameController: regenerateInvitationCode requested - gameId={}, duration={}, requestedUser={}",
        id,
        duration,
        username != null ? username : "-");

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn(
          "GameController: regenerateInvitationCode unauthorized - gameId={}, requestedUser={}, remoteAddr={}",
          id,
          username != null ? username : "-",
          httpRequest.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    GameDto gameDto = gameQueryUseCase.getGameByIdOrThrow(id);
    if (!user.getId().equals(gameDto.getCreatorId())) {
      log.warn(
          "GameController: regenerateInvitationCode forbidden - gameId={}, userId={}, creatorId={}",
          id,
          user.getId(),
          gameDto.getCreatorId());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    GameDto game = gameService.regenerateInvitationCode(id, duration);
    log.info(
        "GameController: regenerateInvitationCode succeeded - gameId={}, userId={}, duration={}",
        id,
        user.getId(),
        duration);
    return ResponseEntity.ok(game);
  }

  @Operation(summary = "Rename game", description = "Renames an existing game")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Game renamed successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to rename"),
        @ApiResponse(responseCode = "404", description = "Game not found")
      })
  @PostMapping("/{id:" + UUID_PATH_PATTERN + "}/rename")
  public ResponseEntity<GameDto> renameGame(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) RenameGameRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    log.info(
        "GameController: renameGame requested - gameId={}, requestedUser={}",
        id,
        username != null ? username : "-");

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn(
          "GameController: renameGame unauthorized - gameId={}, requestedUser={}, remoteAddr={}",
          id,
          username != null ? username : "-",
          httpRequest.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    GameDto gameDto = gameQueryUseCase.getGameByIdOrThrow(id);
    if (!user.getId().equals(gameDto.getCreatorId())) {
      log.warn(
          "GameController: renameGame forbidden - gameId={}, userId={}, creatorId={}",
          id,
          user.getId(),
          gameDto.getCreatorId());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    String newName = request == null ? null : request.sanitizedName();
    return handleRenameGame(id, user.getId(), newName);
  }

  private String normalizeInvitationCode(String invitationCode) {
    return invitationCode.trim().toUpperCase(Locale.ROOT);
  }

  private ResponseEntity<GameDto> joinUserToGameByInvitationCode(
      GameDto game, UUID userId, String normalizedCode) {
    JoinGameRequest joinRequest = new JoinGameRequest();
    joinRequest.setGameId(game.getId());
    joinRequest.setUserId(userId);
    joinRequest.setInvitationCode(normalizedCode);
    validationService.validateJoinGameRequest(joinRequest);
    gameService.joinGame(userId, joinRequest);
    log.info(
        "GameController: joinGameWithCode succeeded - gameId={}, userId={}", game.getId(), userId);
    return ResponseEntity.ok(game);
  }

  private ResponseEntity<GameDto> notFoundByInvitationCode(UUID userId, String normalizedCode) {
    log.warn(
        "GameController: joinGameWithCode notFound - userId={}, codeLength={}",
        userId,
        normalizedCode.length());
    return ResponseEntity.notFound().build();
  }

  private ResponseEntity<Map<String, Object>> validateStartDraftRequest(
      UUID gameId, UUID userId, UUID creatorId) {
    if (creatorId == null) {
      log.warn("GameController: startDraft invalidState - gameId={}, creatorId=null", gameId);
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of(ERROR_KEY, "Createur de la partie introuvable"));
    }

    if (!userId.equals(creatorId)) {
      log.warn(
          "GameController: startDraft forbidden - gameId={}, userId={}, creatorId={}",
          gameId,
          userId,
          creatorId);
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of(ERROR_KEY, "Utilisateur non autorise pour ce draft"));
    }

    return null;
  }

  private ResponseEntity<GameDto> handleRenameGame(UUID gameId, UUID userId, String newName) {
    if (newName == null || newName.isBlank()) {
      log.warn("GameController: renameGame badRequest - gameId={}, userId={}", gameId, userId);
      return ResponseEntity.badRequest().build();
    }

    GameDto updatedGame = gameService.renameGame(gameId, newName);
    log.info(
        "GameController: renameGame succeeded - gameId={}, userId={}, newNameLength={}",
        gameId,
        userId,
        newName.length());
    return ResponseEntity.ok(updatedGame);
  }
}
