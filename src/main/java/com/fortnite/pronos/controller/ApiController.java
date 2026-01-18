package com.fortnite.pronos.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.PlayerService;
import com.fortnite.pronos.service.UserService;
import com.fortnite.pronos.service.team.TeamQueryService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

  private static final Logger log = LoggerFactory.getLogger(ApiController.class);

  private final TeamQueryService teamQueryService;
  private final PlayerService playerService;
  private final UserService userService;
  private final org.springframework.core.env.Environment environment;

  @GetMapping("/teams")
  public List<TeamDTO> allTeams(@RequestParam(defaultValue = "2025") int season) {
    List<Team> teams = teamQueryService.findTeamsBySeasonWithFetch(season);
    return teams.stream().map(TeamDTO::fromTeam).collect(Collectors.toList());
  }

  @GetMapping("/teams/{teamId}")
  public ResponseEntity<TeamDTO> getTeamById(@PathVariable UUID teamId) {
    try {
      Team team =
          teamQueryService
              .findTeamByIdWithFetch(teamId)
              .orElseThrow(() -> new RuntimeException("Equipe non trouvee"));

      return ResponseEntity.ok(TeamDTO.fromTeam(team));
    } catch (Exception e) {
      log.error("Erreur lors de la recuperation de l'equipe {}: {}", teamId, e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }
  }

  @GetMapping("/players")
  public ResponseEntity<?> players(@RequestParam(required = false) String region) {
    try {
      List<Player> players;
      if (region == null || region.isBlank()) {
        players = playerService.findAllPlayers();
      } else {
        Player.Region regionEnum = Player.Region.valueOf(region.toUpperCase());
        players = playerService.findPlayersByRegion(regionEnum);
      }

      List<PlayerDTO> playerDTOs =
          players.stream().map(PlayerDTO::fromPlayer).collect(Collectors.toList());

      return ResponseEntity.ok(playerDTOs);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(
              "Invalid region: " + region + ". Valid regions are: EU, NAW, NAC, BR, ASIA, OCE, ME");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred: " + e.getMessage());
    }
  }

  @GetMapping("/players/{id}")
  public ResponseEntity<?> one(@PathVariable UUID id) {
    return playerService
        .findPlayerById(id)
        .map(player -> ResponseEntity.ok(PlayerDTO.fromPlayer(player)))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/trade-form-data")
  @Transactional(readOnly = true)
  public ResponseEntity<?> getTradeFormData(
      @RequestParam(value = "user", required = false) String user,
      HttpServletRequest request,
      Authentication auth) {
    log.debug("Trade form data requested by user: {}", auth != null ? auth.getName() : "anonymous");

    if (log.isTraceEnabled()) {
      log.trace("Request URL: {}", request.getRequestURL());
    }

    try {
      String username;

      if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
        username = auth.getName();
        log.debug("Authenticated user: {}", username);
      } else if (user != null && !user.isBlank()) {
        username = user;
      } else if (isTestProfile()) {
        username = "Thibaut"; // fallback test
      } else {
        log.warn("Unauthorized access attempt to trade-form-data");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Authentication required"));
      }

      log.debug("Looking up user in database");
      // Try to find by email first (auth principal uses email), then by username
      User currentUser = userService.findUserByEmailOrUsername(username).orElse(null);

      if (currentUser == null) {
        log.warn("User not found: {}", username);
        if (user != null && !user.isBlank()) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(Map.of("error", "User not found"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
      }
      log.debug("User found: {}", currentUser.getUsername());

      log.debug("Looking up user's team");
      List<Team> seasonTeams = teamQueryService.findTeamsBySeasonWithFetch(2025);
      Team resolvedTeam = teamQueryService.findTeamByOwnerAndSeason(currentUser, 2025).orElse(null);

      if (resolvedTeam == null) {
        resolvedTeam =
            seasonTeams.stream()
                .filter(team -> team.getOwner() != null)
                .filter(team -> team.getOwner().getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(null);
      }

      if (resolvedTeam == null && isTestProfile()) {
        // Fallback test : utiliser l'équipe Thibaut si présente
        resolvedTeam =
            seasonTeams.stream()
                .filter(team -> "Team Thibaut".equalsIgnoreCase(team.getName()))
                .findFirst()
                .orElse(null);
      }

      if (resolvedTeam == null) {
        log.debug("No team found for user: {}", username);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "No team found for user"));
      }

      final Team myTeam = resolvedTeam;
      log.debug("Team found: {}", myTeam.getName());

      log.debug("Preparing response data");
      return ResponseEntity.ok(
          Map.of(
              "myTeam", TeamDTO.fromTeam(myTeam),
              "myPlayers",
                  myTeam.getPlayers().stream()
                      .map(TeamPlayer::getPlayer)
                      .map(PlayerDTO::fromPlayer)
                      .collect(Collectors.toList()),
              "otherTeams",
                  seasonTeams.stream()
                      .filter(team -> !team.equals(myTeam))
                      .map(TeamDTO::fromTeam)
                      .collect(Collectors.toList()),
              "allPlayers",
                  playerService.findAllPlayers().stream()
                      .map(PlayerDTO::fromPlayer)
                      .collect(Collectors.toList())));

    } catch (Exception e) {
      log.error("Error in getTradeFormData: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Internal server error"));
    }
  }

  private boolean isTestProfile() {
    if (environment == null) {
      return true;
    }
    return java.util.Arrays.stream(environment.getActiveProfiles())
        .anyMatch(p -> p.equalsIgnoreCase("test") || p.equalsIgnoreCase("h2"));
  }

  @Data
  public static class TeamDTO {
    private UUID id;
    private String name;
    private Integer season;
    private List<TeamPlayerDTO> players;

    public static TeamDTO fromTeam(Team team) {
      TeamDTO dto = new TeamDTO();
      dto.id = team.getId();
      dto.name = team.getName();
      dto.season = team.getSeason();
      dto.players =
          team.getPlayers().stream()
              .map(TeamPlayerDTO::fromTeamPlayer)
              .collect(Collectors.toList());
      return dto;
    }
  }

  @Data
  public static class TeamPlayerDTO {
    private PlayerDTO player;

    public static TeamPlayerDTO fromTeamPlayer(TeamPlayer teamPlayer) {
      TeamPlayerDTO dto = new TeamPlayerDTO();
      dto.player = PlayerDTO.fromPlayer(teamPlayer.getPlayer());
      return dto;
    }
  }

  @Data
  public static class PlayerDTO {
    private UUID id;
    private String username;
    private String nickname;
    private String fortniteId;
    private Player.Region region;
    private String tranche;
    private Integer currentSeason;

    public static PlayerDTO fromPlayer(Player player) {
      PlayerDTO dto = new PlayerDTO();
      dto.id = player.getId();
      dto.username = player.getUsername();
      dto.nickname = player.getNickname();
      dto.fortniteId = player.getFortniteId();
      dto.region = player.getRegion();
      dto.tranche = player.getTranche();
      dto.currentSeason = player.getCurrentSeason();
      return dto;
    }
  }
}
