package com.fortnite.pronos.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

  private static final Logger LOG = LoggerFactory.getLogger(ApiController.class);
  private static final String ERROR_KEY = "error";
  private static final String AUTH_REQUIRED_MESSAGE = "Authentication required";
  private static final String USER_NOT_FOUND_MESSAGE = "User not found";
  private static final String NO_TEAM_FOUND_MESSAGE = "No team found for user";
  private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";
  private static final int DEFAULT_SEASON = 2025;
  private static final String DEFAULT_TEST_USERNAME = "Thibaut";

  private final TeamQueryService teamQueryService;
  private final PlayerService playerService;
  private final UserService userService;
  private final org.springframework.core.env.Environment environment;

  @GetMapping("/teams")
  public List<TeamDTO> allTeams(@RequestParam(defaultValue = "2025") int season) {
    List<Team> teams = teamQueryService.findTeamsBySeasonWithFetch(season);
    return teams.stream().map(TeamDTO::fromTeam).toList();
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
      LOG.error("Erreur lors de la recuperation de l'equipe {}", teamId, e);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }
  }

  @GetMapping("/players")
  public ResponseEntity<Object> players(@RequestParam(required = false) String region) {
    try {
      List<Player> players;
      if (region == null || region.isBlank()) {
        players = playerService.findAllPlayers();
      } else {
        Player.Region regionEnum = Player.Region.valueOf(region.toUpperCase(Locale.ROOT));
        players = playerService.findPlayersByRegion(regionEnum);
      }

      List<PlayerDTO> playerDTOs = players.stream().map(PlayerDTO::fromPlayer).toList();

      return ResponseEntity.ok(playerDTOs);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(
              "Invalid region: " + region + ". Valid regions are: EU, NAW, NAC, BR, ASIA, OCE, ME");
    } catch (Exception e) {
      LOG.error("Error while loading players for region {}", region, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred: " + e.getMessage());
    }
  }

  @GetMapping("/players/{id}")
  public ResponseEntity<PlayerDTO> one(@PathVariable UUID id) {
    return ResponseEntity.of(playerService.findPlayerById(id).map(PlayerDTO::fromPlayer));
  }

  @GetMapping("/trade-form-data")
  @Transactional(readOnly = true)
  public ResponseEntity<Object> getTradeFormData(
      @RequestParam(value = "user", required = false) String user,
      HttpServletRequest request,
      Authentication auth) {
    LOG.debug("Trade form data requested by user: {}", auth != null ? auth.getName() : "anonymous");

    if (LOG.isTraceEnabled()) {
      LOG.trace("Request URL: {}", request.getRequestURL());
    }

    ResponseEntity<Object> response;
    try {
      String username = resolveRequestUsername(user, auth);
      response = resolveTradeFormDataResponse(user, username);
    } catch (Exception e) {
      LOG.error("Error in getTradeFormData", e);
      response =
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of(ERROR_KEY, INTERNAL_SERVER_ERROR_MESSAGE));
    }
    return response;
  }

  private ResponseEntity<Object> resolveTradeFormDataResponse(String user, String username) {
    ResponseEntity<Object> response;
    if (username == null) {
      LOG.warn("Unauthorized access attempt to trade-form-data");
      response =
          ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of(ERROR_KEY, AUTH_REQUIRED_MESSAGE));
    } else {
      LOG.debug("Looking up user in database");
      User currentUser = userService.findUserByEmailOrUsername(username).orElse(null);

      if (currentUser == null) {
        LOG.warn("User not found: {}", username);
        HttpStatus status =
            user != null && !user.isBlank() ? HttpStatus.BAD_REQUEST : HttpStatus.NOT_FOUND;
        response = ResponseEntity.status(status).body(Map.of(ERROR_KEY, USER_NOT_FOUND_MESSAGE));
      } else {
        LOG.debug("User found: {}", currentUser.getUsername());

        List<Team> seasonTeams = teamQueryService.findTeamsBySeasonWithFetch(DEFAULT_SEASON);
        Team resolvedTeam = resolveTeamForUser(currentUser, seasonTeams);

        if (resolvedTeam == null) {
          LOG.debug("No team found for user: {}", username);
          response =
              ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(Map.of(ERROR_KEY, NO_TEAM_FOUND_MESSAGE));
        } else {
          LOG.debug("Team found: {}", resolvedTeam.getName());
          response = ResponseEntity.ok(buildTradeFormDataResponse(resolvedTeam, seasonTeams));
        }
      }
    }
    return response;
  }

  private String resolveRequestUsername(String user, Authentication auth) {
    String resolvedUsername = null;
    if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
      LOG.debug("Authenticated user: {}", auth.getName());
      resolvedUsername = auth.getName();
    } else if (user != null && !user.isBlank()) {
      resolvedUsername = user;
    } else if (isTestProfile()) {
      resolvedUsername = DEFAULT_TEST_USERNAME;
    }
    return resolvedUsername;
  }

  private Team resolveTeamForUser(User currentUser, List<Team> seasonTeams) {
    Team team = teamQueryService.findTeamByOwnerAndSeason(currentUser, DEFAULT_SEASON).orElse(null);
    if (team != null) {
      return team;
    }

    team =
        seasonTeams.stream()
            .filter(candidate -> candidate.getOwner() != null)
            .filter(candidate -> candidate.getOwner().getId().equals(currentUser.getId()))
            .findFirst()
            .orElse(null);
    if (team != null || !isTestProfile()) {
      return team;
    }

    return seasonTeams.stream()
        .filter(candidate -> "Team Thibaut".equalsIgnoreCase(candidate.getName()))
        .findFirst()
        .orElse(null);
  }

  private Map<String, Object> buildTradeFormDataResponse(Team myTeam, List<Team> seasonTeams) {
    return Map.of(
        "myTeam",
        TeamDTO.fromTeam(myTeam),
        "myPlayers",
        myTeam.getPlayers().stream().map(TeamPlayer::getPlayer).map(PlayerDTO::fromPlayer).toList(),
        "otherTeams",
        seasonTeams.stream().filter(team -> !team.equals(myTeam)).map(TeamDTO::fromTeam).toList(),
        "allPlayers",
        playerService.findAllPlayers().stream().map(PlayerDTO::fromPlayer).toList());
  }

  private boolean isTestProfile() {
    if (environment == null) {
      return true;
    }
    return Arrays.stream(environment.getActiveProfiles())
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
      dto.players = team.getPlayers().stream().map(TeamPlayerDTO::fromTeamPlayer).toList();
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
