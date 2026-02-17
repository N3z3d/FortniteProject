package com.fortnite.pronos.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.application.usecase.GameQueryUseCase;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
public class GameQueryController {

  private static final String UUID_PATH_PATTERN =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

  private final GameQueryUseCase gameQueryUseCase;
  private final UserResolver userResolver;

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

    List<GameDto> userGames = gameQueryUseCase.getGamesByUser(user.getId());
    return ResponseEntity.ok(userGames);
  }

  @Operation(
      summary = "Get game details by ID",
      description = "Retrieves detailed information about a specific fantasy league game",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Game details retrieved successfully"),
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

    GameDto gameDto = gameQueryUseCase.getGameByIdOrThrow(id);
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
      return ResponseEntity.ok(gameQueryUseCase.getGamesByUser(user.getId()));
    }
    return ResponseEntity.ok(gameQueryUseCase.getAllGames());
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

    return ResponseEntity.ok(gameQueryUseCase.getAvailableGames());
  }

  @Operation(
      summary = "Get game participants",
      description = "Retrieves the list of participants for a game")
  @GetMapping("/{id:" + UUID_PATH_PATTERN + "}/participants")
  public ResponseEntity<List<Map<String, Object>>> getGameParticipants(@PathVariable UUID id) {
    log.debug("Recuperation des participants de la game: {}", id);

    GameDto gameDto = gameQueryUseCase.getGameByIdOrThrow(id);

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
