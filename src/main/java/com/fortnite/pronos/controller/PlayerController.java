package com.fortnite.pronos.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.dto.player.PlayerDto;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.service.PlayerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Player Management Controller - API-001 COMPLETE DOCUMENTATION
 *
 * <p>Contrôleur REST pour la gestion des joueurs Fortnite. Fournit des endpoints pour rechercher,
 * filtrer et récupérer les informations des joueurs avec pagination optimisée.
 */
@Tag(name = "Player Management", description = "Fortnite player data, search, and statistics")
@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PlayerController {
  private final PlayerService playerService;

  @Operation(
      summary = "Get all players with pagination",
      description =
          "Retrieve all players with optimized pagination (max 50 per page for performance)")
  @ApiResponse(responseCode = "200", description = "Players retrieved successfully")
  @GetMapping
  public ResponseEntity<Page<PlayerDto>> getAllPlayers(
      @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Page size (max 50)", example = "20")
          @RequestParam(defaultValue = "20")
          int size,
      @Parameter(description = "Sort field", example = "nickname")
          @RequestParam(defaultValue = "nickname")
          String sortBy,
      @Parameter(description = "Sort direction", example = "asc")
          @RequestParam(defaultValue = "asc")
          String sortDirection,
      Pageable pageable) {

    // Override with custom pagination for performance
    Pageable optimizedPageable =
        PageRequest.of(
            page,
            Math.min(size, 50), // Max 50 per page
            Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

    return ResponseEntity.ok(playerService.getAllPlayers(optimizedPageable));
  }

  @Operation(
      summary = "Get all players (legacy)",
      description =
          "Legacy endpoint for backward compatibility. ⚠️ Not optimized for 149+ players - use paginated endpoint instead")
  @ApiResponse(responseCode = "200", description = "All players retrieved (non-paginated)")
  @GetMapping("/all")
  public ResponseEntity<List<PlayerDto>> getAllPlayersLegacy() {
    // Use pagination internally but return as list for backward compatibility
    Pageable pageable = PageRequest.of(0, 200);
    return ResponseEntity.ok(playerService.getAllPlayers(pageable).getContent());
  }

  @Operation(
      summary = "Get player by ID",
      description = "Retrieve a specific player by their unique identifier")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Player found",
            content = @Content(schema = @Schema(implementation = PlayerDto.class))),
        @ApiResponse(responseCode = "404", description = "Player not found")
      })
  @GetMapping("/{id}")
  public ResponseEntity<PlayerDto> getPlayerById(
      @Parameter(description = "Player unique identifier", required = true) @PathVariable UUID id) {
    return ResponseEntity.ok(playerService.getPlayerById(id));
  }

  @Operation(
      summary = "Get players by region",
      description = "Retrieve all players from a specific Fortnite region")
  @ApiResponse(responseCode = "200", description = "Players retrieved successfully")
  @GetMapping("/region/{region}")
  public ResponseEntity<List<PlayerDto>> getPlayersByRegion(
      @Parameter(
              description = "Fortnite region (EU, NAE, NAW, BR, ASIA, OCE, ME)",
              required = true,
              example = "EU")
          @PathVariable
          Player.Region region) {
    return ResponseEntity.ok(playerService.getPlayersByRegion(region));
  }

  @Operation(
      summary = "Get players by skill tranche",
      description = "Retrieve players filtered by their skill level tranche")
  @ApiResponse(responseCode = "200", description = "Players retrieved successfully")
  @GetMapping("/tranche/{tranche}")
  public ResponseEntity<List<PlayerDto>> getPlayersByTranche(
      @Parameter(description = "Skill tranche identifier", required = true, example = "PRO")
          @PathVariable
          String tranche) {
    return ResponseEntity.ok(playerService.getPlayersByTranche(tranche));
  }

  @Operation(
      summary = "Search players with filters",
      description = "Advanced player search with multiple filter options and pagination")
  @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
  @GetMapping("/search")
  public ResponseEntity<Page<PlayerDto>> searchPlayers(
      @Parameter(description = "Search query (nickname, real name)", example = "Ninja")
          @RequestParam(required = false)
          String query,
      @Parameter(description = "Filter by region", example = "NAE") @RequestParam(required = false)
          Player.Region region,
      @Parameter(description = "Filter by skill tranche", example = "PRO")
          @RequestParam(required = false)
          String tranche,
      @Parameter(description = "Show only available players", example = "true")
          @RequestParam(required = false, defaultValue = "true")
          boolean available,
      Pageable pageable) {
    return ResponseEntity.ok(
        playerService.searchPlayers(query, region, tranche, available, pageable));
  }

  @Operation(summary = "Get active players", description = "Retrieve only currently active players")
  @ApiResponse(responseCode = "200", description = "Active players retrieved successfully")
  @GetMapping("/active")
  public ResponseEntity<List<PlayerDto>> getActivePlayers() {
    return ResponseEntity.ok(playerService.getActivePlayers());
  }

  @Operation(
      summary = "Get player statistics",
      description = "Retrieve aggregated statistics about all players")
  @ApiResponse(responseCode = "200", description = "Player statistics retrieved successfully")
  @GetMapping("/stats")
  public ResponseEntity<?> getPlayersStats() {
    return ResponseEntity.ok(playerService.getPlayersStats());
  }
}
