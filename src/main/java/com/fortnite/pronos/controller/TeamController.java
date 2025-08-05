package com.fortnite.pronos.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;
import com.fortnite.pronos.service.TeamService;

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
 * <p>Contrôleur REST pour la gestion des équipes fantasy. Permet de créer, modifier, supprimer les
 * équipes et gérer les joueurs. Inclut des endpoints pour les opérations CRUD et la gestion des
 * rosters.
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
  private final UserRepository userRepository;

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
    return ResponseEntity.ok(teamService.getTeam(userId, season));
  }

  @Operation(
      summary = "Get all teams for season",
      description = "Retrieve all teams for a specific season")
  @ApiResponse(responseCode = "200", description = "Teams retrieved successfully")
  @GetMapping("/season/{season}")
  public ResponseEntity<List<TeamDto>> getAllTeamsForSeason(
      @Parameter(description = "Season year", required = true, example = "2025") @PathVariable
          int season) {
    return ResponseEntity.ok(teamService.getAllTeams(season));
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
    log.debug("Récupération des équipes pour l'utilisateur '{}' et l'année {}", username, year);

    try {
      // Recherche de l'utilisateur par nom d'utilisateur
      User user =
          userRepository
              .findByUsernameIgnoreCase(username)
              .orElseThrow(
                  () -> new IllegalArgumentException("Utilisateur non trouvé: " + username));

      // Récupération de l'équipe pour cette saison
      TeamDto team = teamService.getTeam(user.getId(), year);

      // Retour d'une liste contenant une seule équipe (cohérent avec l'interface)
      return ResponseEntity.ok(List.of(team));

    } catch (IllegalArgumentException e) {
      log.warn("Utilisateur non trouvé: {}", username);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error(
          "Erreur lors de la récupération des équipes pour {} ({}): {}",
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
          CreateTeamRequest request) {
    return ResponseEntity.ok(
        teamService.createTeam(request.userId(), request.name(), request.season()));
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
          AddPlayerRequest request) {
    return ResponseEntity.ok(
        teamService.addPlayerToTeam(userId, request.playerId(), request.position(), season));
  }

  @Operation(summary = "Remove player from team", description = "Remove a player from a team")
  @ApiResponse(responseCode = "200", description = "Player removed successfully")
  @PostMapping("/user/{userId}/season/{season}/players/remove")
  public ResponseEntity<TeamDto> removePlayerFromTeam(
      @Parameter(description = "User ID", required = true) @PathVariable UUID userId,
      @Parameter(description = "Season", required = true) @PathVariable int season,
      @Parameter(description = "Remove player request", required = true) @RequestBody
          RemovePlayerRequest request) {
    return ResponseEntity.ok(teamService.removePlayerFromTeam(userId, request.playerId(), season));
  }

  @Operation(
      summary = "Make bulk player changes",
      description = "Execute multiple player changes in a single transaction")
  @ApiResponse(responseCode = "200", description = "Player changes executed successfully")
  @PostMapping("/user/{userId}/season/{season}/players/changes")
  public ResponseEntity<TeamDto> makePlayerChanges(
      @Parameter(description = "User ID", required = true) @PathVariable UUID userId,
      @Parameter(description = "Season", required = true) @PathVariable int season,
      @Parameter(
              description = "Player changes map (old player ID -> new player ID)",
              required = true)
          @RequestBody
          Map<UUID, UUID> playerChanges) {
    return ResponseEntity.ok(teamService.makePlayerChanges(userId, playerChanges, season));
  }

  // DTOs for requests
  record CreateTeamRequest(UUID userId, String name, int season) {}

  record AddPlayerRequest(UUID playerId, int position) {}

  record RemovePlayerRequest(UUID playerId) {}
}
