package com.fortnite.pronos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.config.SeedProperties;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.service.seed.PlayerSeedService;
import com.fortnite.pronos.service.seed.ReferenceUserSeedService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for seeding a reference game with real CSV data.
 *
 * <p>Refactored to reduce coupling: 7 dependencies (was 10). Uses specialized seed services for
 * user, player, and team operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"java:S135"})
public class ReferenceGameSeedService {

  private static final String REFERENCE_GAME_NAME = "Partie principale - 147 joueurs";
  private static final String REFERENCE_GAME_DESCRIPTION =
      "Partie de reference CSV (Thibaut, Teddy, Marcel)";
  private static final int CURRENT_SEASON = 2025;
  private static final String CREATOR_USERNAME = "Thibaut";
  private static final List<String> REQUIRED_USERS = List.of(CREATOR_USERNAME, "Teddy", "Marcel");

  // Configuration dependencies (2)
  private final Environment environment;
  private final SeedProperties seedProperties;

  // Seed service dependencies (4)
  private final ReferenceUserSeedService referenceUserSeedService;
  private final PlayerSeedService playerSeedService;
  private final CsvDataLoaderService csvDataLoaderService;

  // Repository for game-specific operations (1)
  private final GameRepositoryPort gameRepository;

  @EventListener(ApplicationReadyEvent.class)
  @Order(3)
  @Transactional
  public void ensureReferenceGame() {
    if (!seedProperties.isEnabled() || !isDevProfile()) {
      return;
    }

    if (seedProperties.isResetMode()) {
      resetGameData();
    }

    SeedPreparationResult preparation = prepareSeedData();
    if (!preparation.canSeed()) {
      return;
    }

    com.fortnite.pronos.model.Game game =
        buildReferenceGame(preparation.creator(), preparation.assignments());
    attachParticipants(game, preparation.users());
    com.fortnite.pronos.model.Game savedGame = gameRepository.save(game);

    List<com.fortnite.pronos.model.Team> teams =
        buildTeams(savedGame, preparation.users(), preparation.assignments());

    int totalPlayers = preparation.assignments().values().stream().mapToInt(List::size).sum();
    log.info(
        "Reference game seeded: name={}, players={}, teams={}",
        REFERENCE_GAME_NAME,
        totalPlayers,
        teams.size());
  }

  private SeedPreparationResult prepareSeedData() {
    Map<String, com.fortnite.pronos.model.User> users =
        referenceUserSeedService.ensureReferenceUsers();
    com.fortnite.pronos.model.User creator = users.get(CREATOR_USERNAME);
    Map<String, List<com.fortnite.pronos.model.Player>> assignments = Map.of();
    boolean canSeed = true;

    if (creator == null) {
      log.warn("Reference seed skipped: missing creator user");
      canSeed = false;
    } else if (gameRepository.existsByNameAndCreator(REFERENCE_GAME_NAME, creator)) {
      log.info("Reference game already seeded: {}", REFERENCE_GAME_NAME);
      canSeed = false;
    } else if (gameRepository.count() > 0) {
      log.warn(
          "Reference seed skipped: existing games detected. Set fortnite.seed.mode=reset to replace.");
      canSeed = false;
    } else {
      assignments = loadAssignments();
      if (assignments.isEmpty()) {
        log.warn("Reference seed skipped: no seed assignments loaded");
        canSeed = false;
      }
    }

    return new SeedPreparationResult(canSeed, users, creator, assignments);
  }

  private boolean isDevProfile() {
    for (String profile : environment.getActiveProfiles()) {
      if ("dev".equals(profile) || "h2".equals(profile)) {
        return true;
      }
    }
    return false;
  }

  private void resetGameData() {
    gameRepository.deleteAll();
    log.info("Reference seed reset: games cleared");
  }

  private Map<String, List<com.fortnite.pronos.model.Player>> loadAssignments() {
    String providerKey = environment.getProperty("fortnite.data.provider", "csv");
    if ("csv".equalsIgnoreCase(providerKey)) {
      csvDataLoaderService.loadAllCsvData();
      Map<String, com.fortnite.pronos.model.Player> playersByNickname = loadPlayersByNickname();
      Map<String, List<com.fortnite.pronos.model.Player>> result = new LinkedHashMap<>();
      for (Map.Entry<String, List<com.fortnite.pronos.model.Player>> entry :
          csvDataLoaderService.getAllPlayerAssignments().entrySet()) {
        List<com.fortnite.pronos.model.Player> managedPlayers = new ArrayList<>();
        for (com.fortnite.pronos.model.Player p : entry.getValue()) {
          com.fortnite.pronos.model.Player managed =
              playersByNickname.get(normalizeNickname(p.getNickname()));
          if (managed != null) {
            managedPlayers.add(managed);
          }
        }
        if (!managedPlayers.isEmpty()) {
          result.put(entry.getKey(), managedPlayers);
        }
      }
      return result;
    }

    // Fallback for other providers
    MockDataGeneratorService.MockDataSet seedData = playerSeedService.loadSeedData();
    if (seedData.total() == 0) {
      return Map.of();
    }
    return resolveAssignments(seedData);
  }

  private Map<String, List<com.fortnite.pronos.model.Player>> resolveAssignments(
      MockDataGeneratorService.MockDataSet seedData) {
    Map<String, com.fortnite.pronos.model.Player> playersByNickname = loadPlayersByNickname();
    Map<String, List<com.fortnite.pronos.model.Player>> resolved = new LinkedHashMap<>();

    for (Map.Entry<String, List<MockDataGeneratorService.PlayerWithScore>> entry :
        seedData.playersByPronosticator().entrySet()) {
      List<com.fortnite.pronos.model.Player> resolvedPlayers = new ArrayList<>();
      for (MockDataGeneratorService.PlayerWithScore playerData : entry.getValue()) {
        com.fortnite.pronos.model.Player seedPlayer = playerData.player();
        String key = normalizeNickname(seedPlayer.getNickname());
        com.fortnite.pronos.model.Player persisted = playersByNickname.get(key);
        if (persisted != null) {
          resolvedPlayers.add(persisted);
        }
      }
      if (!resolvedPlayers.isEmpty()) {
        resolved.put(entry.getKey(), resolvedPlayers);
      }
    }

    return resolved;
  }

  private Map<String, com.fortnite.pronos.model.Player> loadPlayersByNickname() {
    Map<String, com.fortnite.pronos.model.Player> playersByNickname = new LinkedHashMap<>();
    for (com.fortnite.pronos.model.Player player : playerSeedService.getAllPlayers()) {
      String key = normalizeNickname(player.getNickname());
      if (!key.isEmpty()) {
        playersByNickname.putIfAbsent(key, player);
      }
    }
    return playersByNickname;
  }

  private String normalizeNickname(String nickname) {
    return nickname == null ? "" : nickname.trim().toLowerCase(Locale.ROOT);
  }

  private com.fortnite.pronos.model.Game buildReferenceGame(
      com.fortnite.pronos.model.User creator,
      Map<String, List<com.fortnite.pronos.model.Player>> assignments) {
    com.fortnite.pronos.model.Game game =
        com.fortnite.pronos.model.Game.builder()
            .name(REFERENCE_GAME_NAME)
            .description(REFERENCE_GAME_DESCRIPTION)
            .creator(creator)
            .maxParticipants(REQUIRED_USERS.size())
            .participants(new ArrayList<>())
            .regionRules(new ArrayList<>())
            .build();
    game.setLegacyStatus(com.fortnite.pronos.model.Game.Status.ACTIVE);

    ReferenceGameRegionRuleSupport.addRegionRules(game, assignments);
    game.generateInvitationCode();
    return game;
  }

  private void attachParticipants(
      com.fortnite.pronos.model.Game game, Map<String, com.fortnite.pronos.model.User> users) {
    for (int i = 0; i < REQUIRED_USERS.size(); i++) {
      String username = REQUIRED_USERS.get(i);
      com.fortnite.pronos.model.User user = users.get(username);
      if (user == null) {
        continue;
      }
      com.fortnite.pronos.model.GameParticipant participant =
          com.fortnite.pronos.model.GameParticipant.builder()
              .game(game)
              .user(user)
              .draftOrder(i + 1)
              .joinedAt(LocalDateTime.now())
              .creator(user.getId().equals(game.getCreator().getId()))
              .selectedPlayers(new ArrayList<>())
              .build();
      game.addParticipant(participant);
    }
  }

  private List<com.fortnite.pronos.model.Team> buildTeams(
      com.fortnite.pronos.model.Game game,
      Map<String, com.fortnite.pronos.model.User> users,
      Map<String, List<com.fortnite.pronos.model.Player>> assignments) {
    List<com.fortnite.pronos.model.Team> teams = new ArrayList<>();

    for (String username : REQUIRED_USERS) {
      com.fortnite.pronos.model.User owner = users.get(username);
      if (owner == null) {
        log.warn("Reference seed: missing user {}, team skipped", username);
        continue;
      }

      List<com.fortnite.pronos.model.Player> players = resolvePlayersFor(username, assignments);
      if (players.isEmpty()) {
        log.warn("Reference seed: no players for {}, team skipped", username);
        continue;
      }

      com.fortnite.pronos.model.Team team = buildTeam(game, owner, players);
      teams.add(team);
    }

    return teams;
  }

  private List<com.fortnite.pronos.model.Player> resolvePlayersFor(
      String username, Map<String, List<com.fortnite.pronos.model.Player>> assignments) {
    for (Map.Entry<String, List<com.fortnite.pronos.model.Player>> entry : assignments.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(username)) {
        return entry.getValue();
      }
    }
    return List.of();
  }

  private com.fortnite.pronos.model.Team buildTeam(
      com.fortnite.pronos.model.Game game,
      com.fortnite.pronos.model.User owner,
      List<com.fortnite.pronos.model.Player> players) {
    com.fortnite.pronos.model.Team team = new com.fortnite.pronos.model.Team();
    team.setName("Equipe " + owner.getUsername());
    team.setOwner(owner);
    team.setSeason(CURRENT_SEASON);
    team.setGame(game);

    int position = 1;
    for (com.fortnite.pronos.model.Player player : players) {
      team.addPlayer(player, position);
      position += 1;
    }

    return team;
  }

  private record SeedPreparationResult(
      boolean canSeed,
      Map<String, com.fortnite.pronos.model.User> users,
      com.fortnite.pronos.model.User creator,
      Map<String, List<com.fortnite.pronos.model.Player>> assignments) {}
}
