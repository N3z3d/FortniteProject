package com.fortnite.pronos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.config.SeedProperties;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.TradeRepository;
import com.fortnite.pronos.repository.UserRepository;
import com.fortnite.pronos.service.seed.SeedDataProviderSelectorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceGameSeedService {

  private static final String REFERENCE_GAME_NAME = "Partie principale - 147 joueurs";
  private static final String REFERENCE_GAME_DESCRIPTION =
      "Partie de reference CSV (Thibaut, Teddy, Marcel)";
  private static final int CURRENT_SEASON = 2025;
  private static final List<String> REQUIRED_USERS = List.of("Thibaut", "Teddy", "Marcel");
  private static final String DEFAULT_PASSWORD = "password";

  private final Environment environment;
  private final SeedProperties seedProperties;
  private final CsvDataLoaderService csvDataLoaderService;
  private final SeedDataProviderSelectorService seedDataProviderSelector;
  private final UserRepository userRepository;
  private final GameRepository gameRepository;
  private final PlayerRepository playerRepository;
  private final TeamRepository teamRepository;
  private final TradeRepository tradeRepository;
  private final PasswordEncoder passwordEncoder;

  @EventListener(ApplicationReadyEvent.class)
  @Order(3)
  @Transactional
  public void ensureReferenceGame() {
    if (!seedProperties.isEnabled()) {
      return;
    }
    if (!isDevProfile()) {
      return;
    }

    if (seedProperties.isResetMode()) {
      resetGameData();
    }

    Map<String, User> users = ensureReferenceUsers();
    User creator = users.get("Thibaut");
    if (creator == null) {
      log.warn("Reference seed skipped: missing creator user");
      return;
    }

    if (gameRepository.existsByNameAndCreator(REFERENCE_GAME_NAME, creator)) {
      log.info("Reference game already seeded: {}", REFERENCE_GAME_NAME);
      return;
    }
    if (gameRepository.count() > 0) {
      log.warn(
          "Reference seed skipped: existing games detected. Set fortnite.seed.mode=reset to replace.");
      return;
    }

    Map<String, List<Player>> assignments = loadAssignments();
    if (assignments.isEmpty()) {
      log.warn("Reference seed skipped: no seed assignments loaded");
      return;
    }

    Game game = buildReferenceGame(creator, assignments);
    attachParticipants(game, users);
    Game savedGame = gameRepository.save(game);

    List<Team> teams = buildTeams(savedGame, users, assignments);
    teamRepository.saveAll(teams);

    int totalPlayers = assignments.values().stream().mapToInt(List::size).sum();
    log.info(
        "Reference game seeded: name={}, players={}, teams={}",
        REFERENCE_GAME_NAME,
        totalPlayers,
        teams.size());
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
    tradeRepository.deleteAll();
    teamRepository.deleteAll();
    gameRepository.deleteAll();
    log.info("Reference seed reset: games and teams cleared");
  }

  private Map<String, List<Player>> loadAssignments() {
    String providerKey = environment.getProperty("fortnite.data.provider", "csv");
    if ("csv".equalsIgnoreCase(providerKey)) {
      csvDataLoaderService.loadAllCsvData();
      // Reload players from DB to ensure they are in current persistence context
      Map<String, Player> playersByNickname = loadPlayersByNickname();
      Map<String, List<Player>> result = new LinkedHashMap<>();
      for (Map.Entry<String, List<Player>> entry :
          csvDataLoaderService.getAllPlayerAssignments().entrySet()) {
        List<Player> managedPlayers = new ArrayList<>();
        for (Player p : entry.getValue()) {
          Player managed = playersByNickname.get(normalizeNickname(p.getNickname()));
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
    MockDataGeneratorService.MockDataSet seedData = seedDataProviderSelector.loadSeedData();
    if (seedData.total() == 0) {
      return Map.of();
    }
    return resolveAssignments(seedData);
  }

  private Map<String, List<Player>> resolveAssignments(
      MockDataGeneratorService.MockDataSet seedData) {
    Map<String, Player> playersByNickname = loadPlayersByNickname();
    Map<String, List<Player>> resolved = new LinkedHashMap<>();

    for (Map.Entry<String, List<MockDataGeneratorService.PlayerWithScore>> entry :
        seedData.playersByPronosticator().entrySet()) {
      List<Player> resolvedPlayers = new ArrayList<>();
      for (MockDataGeneratorService.PlayerWithScore playerData : entry.getValue()) {
        Player seedPlayer = playerData.player();
        String key = normalizeNickname(seedPlayer.getNickname());
        Player persisted = playersByNickname.get(key);
        // Only add persisted players, skip transient ones
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

  private Map<String, Player> loadPlayersByNickname() {
    Map<String, Player> playersByNickname = new LinkedHashMap<>();
    for (Player player : playerRepository.findAll()) {
      String key = normalizeNickname(player.getNickname());
      if (!key.isEmpty()) {
        playersByNickname.putIfAbsent(key, player);
      }
    }
    return playersByNickname;
  }

  private String normalizeNickname(String nickname) {
    return nickname == null ? "" : nickname.trim().toLowerCase();
  }

  private Map<String, User> ensureReferenceUsers() {
    Map<String, User> users = new LinkedHashMap<>();
    users.put("Thibaut", ensureUser("Thibaut", "thibaut@test.com"));
    users.put("Teddy", ensureUser("Teddy", "teddy@test.com"));
    users.put("Marcel", ensureUser("Marcel", "marcel@test.com"));
    return users;
  }

  private User ensureUser(String username, String email) {
    return userRepository
        .findByUsernameIgnoreCase(username)
        .orElseGet(() -> createUser(username, email));
  }

  private User createUser(String username, String email) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
    user.setRole(User.UserRole.USER);
    user.setCurrentSeason(CURRENT_SEASON);
    return userRepository.save(user);
  }

  private Game buildReferenceGame(User creator, Map<String, List<Player>> assignments) {
    Game game =
        Game.builder()
            .name(REFERENCE_GAME_NAME)
            .description(REFERENCE_GAME_DESCRIPTION)
            .creator(creator)
            .maxParticipants(REQUIRED_USERS.size())
            .status(GameStatus.ACTIVE)
            .participants(new ArrayList<>())
            .regionRules(new ArrayList<>())
            .build();

    addRegionRules(game, assignments);
    game.generateInvitationCode();
    return game;
  }

  private void attachParticipants(Game game, Map<String, User> users) {
    for (int i = 0; i < REQUIRED_USERS.size(); i++) {
      String username = REQUIRED_USERS.get(i);
      User user = users.get(username);
      if (user == null) {
        continue;
      }
      GameParticipant participant =
          GameParticipant.builder()
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

  private List<Team> buildTeams(
      Game game, Map<String, User> users, Map<String, List<Player>> assignments) {
    List<Team> teams = new ArrayList<>();

    for (String username : REQUIRED_USERS) {
      User owner = users.get(username);
      if (owner == null) {
        log.warn("Reference seed: missing user {}, team skipped", username);
        continue;
      }

      List<Player> players = resolvePlayersFor(username, assignments);
      if (players.isEmpty()) {
        log.warn("Reference seed: no players for {}, team skipped", username);
        continue;
      }

      Team team = buildTeam(game, owner, players);
      teams.add(team);
    }

    return teams;
  }

  private List<Player> resolvePlayersFor(String username, Map<String, List<Player>> assignments) {
    for (Map.Entry<String, List<Player>> entry : assignments.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(username)) {
        return entry.getValue();
      }
    }
    return List.of();
  }

  private Team buildTeam(Game game, User owner, List<Player> players) {
    Team team = new Team();
    team.setName("Equipe " + owner.getUsername());
    team.setOwner(owner);
    team.setSeason(CURRENT_SEASON);
    team.setGame(game);

    int position = 1;
    for (Player player : players) {
      team.addPlayer(player, position++);
    }

    return team;
  }

  private void addRegionRules(Game game, Map<String, List<Player>> assignments) {
    Map<Player.Region, Long> counts = countPlayersByRegion(assignments);
    for (Player.Region region : Player.Region.values()) {
      long count = counts.getOrDefault(region, 0L);
      int maxPlayers = (int) Math.min(10, Math.max(1, count));
      GameRegionRule rule =
          GameRegionRule.builder().game(game).region(region).maxPlayers(maxPlayers).build();
      game.addRegionRule(rule);
    }
  }

  private Map<Player.Region, Long> countPlayersByRegion(Map<String, List<Player>> assignments) {
    Map<Player.Region, Long> counts = new LinkedHashMap<>();
    for (List<Player> players : assignments.values()) {
      for (Player player : players) {
        if (player != null && player.getRegion() != null) {
          counts.merge(player.getRegion(), 1L, Long::sum);
        }
      }
    }
    return counts;
  }
}
