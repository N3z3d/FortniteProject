package com.fortnite.pronos.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.application.usecase.TeamQueryUseCase;
import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.TeamService;
import com.fortnite.pronos.service.UserResolver;

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
 * Team Management Controller - API-001 COMPLETE DOCUMENTATION
 *
 * <p>ContrÃ´leur REST pour la gestion des Ã©quipes fantasy. Permet de crÃ©er, modifier, supprimer
 * les Ã©quipes et gÃ©rer les joueurs. Inclut des endpoints pour les opÃ©rations CRUD et la gestion
 * des rosters.
 */
@Tag(
    name = "Team Management",
    description = "Fantasy team operations, roster management, and player assignments")
@Slf4j
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

  private final TeamService teamService;
  private final TeamQueryUseCase teamQueryUseCase;
  private final UserResolver userResolver;

  @Operation(
      summary = "Get team by user ID and season",
      description = "Retrieve a specific team for a user in a given season")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Team retrieved successfully",
            content = @Content(schema = @Schema(implementation = TeamDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Team not found",
            content =
                @Content(examples = @ExampleObject(value = "{\"error\": \"Team not found\"}")))
      })
  @GetMapping("/user/{userId}/season/{season}")
  public ResponseEntity<TeamDto> getTeamForUserAndSeason(
      @Parameter(description = "User unique identifier", required = true) @PathVariable UUID userId,
      @Parameter(description = "Season year", required = true, example = "2025") @PathVariable
          int season) {
    return ResponseEntity.ok(teamQueryUseCase.getTeam(userId, season));
  }

  @Operation(
      summary = "Get all teams for season",
      description = "Retrieve all teams for a specific season")
  @ApiResponse(responseCode = "200", description = "Teams retrieved successfully")
  @GetMapping("/season/{season}")
  public ResponseEntity<List<TeamDto>> getAllTeamsForSeason(
      @Parameter(description = "Season year", required = true, example = "2025") @PathVariable
          int season) {
    return ResponseEntity.ok(teamQueryUseCase.getAllTeams(season));
  }

  @Operation(
      summary = "Get all teams for a game",
      description = "Retrieve all teams participating in a specific game")
  @ApiResponse(responseCode = "200", description = "Teams retrieved successfully")
  @GetMapping("/game/{gameId}")
  public ResponseEntity<List<TeamDto>> getTeamsByGame(
      @Parameter(description = "Game unique identifier", required = true) @PathVariable
          UUID gameId) {
    log.debug("RÃ©cupÃ©ration des Ã©quipes pour la game {}", gameId);
    return ResponseEntity.ok(teamQueryUseCase.getTeamsByGame(gameId));
  }

  @Operation(
      summary = "Get teams by username and year",
      description = "Retrieve teams for a user by username and year using query parameters")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Teams retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  @GetMapping("/user")
  public ResponseEntity<List<TeamDto>> getTeamsByUserAndYear(
      @Parameter(description = "Username", required = true, example = "Marcel")
          @RequestParam("user")
          String username,
      @Parameter(description = "Year", required = true, example = "2025") @RequestParam("year")
          int year) {
    log.debug("Recuperation des equipes pour l'utilisateur '{}' et l'annee {}", username, year);

    try {
      return ResponseEntity.ok(teamQueryUseCase.getTeamsByUsernameAndYear(username, year));
    } catch (IllegalArgumentException e) {
      log.warn("Utilisateur non trouve: {}", username);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error(
          "Erreur lors de la recuperation des equipes pour {} ({}): {}",
          username,
          year,
          e.getMessage());
      return ResponseEntity.internalServerError().build();
    }
  }

  @Operation(summary = "Create new team", description = "Create a new fantasy team for a user")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Team created successfully",
            content = @Content(schema = @Schema(implementation = TeamDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
      })
  @PostMapping
  public ResponseEntity<TeamDto> createTeam(
      @Parameter(description = "Team creation request", required = true) @RequestBody
          CreateTeamRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    User currentUser = userResolver.resolve(username, httpRequest);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (request.userId() != null && !request.userId().equals(currentUser.getId())) {
      log.warn(
          "Tentative de creation d'equipe avec userId forge: path={}, current={}",
          request.userId(),
          currentUser.getId());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    return ResponseEntity.ok(
        teamService.createTeam(currentUser.getId(), request.name(), request.season()));
  }

  @Operation(summary = "Update team", description = "Update an existing team's information")
  @ApiResponse(responseCode = "200", description = "Team updated successfully")
  @PostMapping("/{teamId}")
  public ResponseEntity<TeamDto> updateTeam(
      @Parameter(description = "Team unique identifier", required = true) @PathVariable UUID teamId,
      @Parameter(description = "Updated team data", required = true) @RequestBody TeamDto teamDto) {
    return ResponseEntity.ok(teamService.updateTeam(teamId, teamDto));
  }

  @Operation(summary = "Delete team", description = "Delete a team permanently")
  @ApiResponse(responseCode = "204", description = "Team deleted successfully")
  @DeleteMapping("/{teamId}")
  public ResponseEntity<Void> deleteTeam(
      @Parameter(description = "Team unique identifier", required = true) @PathVariable
          UUID teamId) {
    teamService.deleteTeam(teamId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Add player to team",
      description = "Add a player to a team at a specific position")
  @ApiResponse(responseCode = "200", description = "Player added successfully")
  @PostMapping("/user/{userId}/season/{season}/players/add")
  public ResponseEntity<TeamDto> addPlayerToTeam(
      @Parameter(description = "User ID", required = true) @PathVariable UUID userId,
      @Parameter(description = "Season", required = true) @PathVariable int season,
      @Parameter(description = "Add player request", required = true) @RequestBody
          AddPlayerRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    User currentUser = userResolver.resolve(username, httpRequest);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (!userId.equals(currentUser.getId())) {
      log.warn(
          "Tentative d'ajout de joueur sur equipe d'un autre utilisateur: path={}, current={}",
          userId,
          currentUser.getId());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    return ResponseEntity.ok(
        teamService.addPlayerToTeam(
            currentUser.getId(), request.playerId(), request.position(), season));
  }

  @Operation(summary = "Remove player from team", description = "Remove a player from a team")
  @ApiResponse(responseCode = "403", description = "Operation forbidden outside trade flow")
  @PostMapping("/user/{userId}/season/{season}/players/remove")
  public ResponseEntity<TeamDto> removePlayerFromTeam(
      @Parameter(description = "User ID", required = true) @PathVariable UUID userId,
      @Parameter(description = "Season", required = true) @PathVariable int season,
      @Parameter(description = "Remove player request", required = true) @RequestBody
          RemovePlayerRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    User currentUser = userResolver.resolve(username, httpRequest);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (!userId.equals(currentUser.getId())) {
      log.warn(
          "Tentative de suppression de joueur sur equipe d'un autre utilisateur: path={}, current={}",
          userId,
          currentUser.getId());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    log.warn(
        "Suppression directe de joueur refusee (hors trade): userId={}, season={}, playerId={}",
        currentUser.getId(),
        season,
        request.playerId());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  }

  @Operation(
      summary = "Make bulk player changes",
      description = "Execute multiple player changes in a single transaction")
  @ApiResponse(responseCode = "403", description = "Operation forbidden outside trade flow")
  @PostMapping("/user/{userId}/season/{season}/players/changes")
  public ResponseEntity<TeamDto> makePlayerChanges(
      @Parameter(description = "User ID", required = true) @PathVariable UUID userId,
      @Parameter(description = "Season", required = true) @PathVariable int season,
      @Parameter(
              description = "Player changes map (old player ID -> new player ID)",
              required = true)
          @RequestBody
          Map<UUID, UUID> playerChanges,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {
    User currentUser = userResolver.resolve(username, httpRequest);
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (!userId.equals(currentUser.getId())) {
      log.warn(
          "Tentative de changements roster sur equipe d'un autre utilisateur: path={}, current={}",
          userId,
          currentUser.getId());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    log.warn(
        "Changements roster directs refuses (hors trade): userId={}, season={}, changesCount={}",
        currentUser.getId(),
        season,
        playerChanges == null ? 0 : playerChanges.size());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  }

  // DTOs for requests
  record CreateTeamRequest(UUID userId, String name, int season) {}

  record AddPlayerRequest(UUID playerId, int position) {}

  record RemovePlayerRequest(UUID playerId) {}
}
