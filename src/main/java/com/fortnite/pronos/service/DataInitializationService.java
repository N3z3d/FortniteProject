package com.fortnite.pronos.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.config.SeedProperties;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;
import com.fortnite.pronos.service.seed.GameSeedService;
import com.fortnite.pronos.service.seed.SeedDataProviderSelectorService;
import com.fortnite.pronos.service.seed.UserSeedService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service d'initialisation des donnees de test. Orchestrates user, player, team, and game seeding.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService {

  private static final String SEED_RESET_PROPERTY = "fortnite.seed.reset";

  private final UserRepository userRepository;
  private final PlayerRepository playerRepository;
  private final TeamRepository teamRepository;
  private final ScoreRepository scoreRepository;
  private final GameRepository gameRepository;
  private final Environment environment;
  private final CsvDataLoaderService csvDataLoaderService;
  private final SeedDataProviderSelectorService seedDataProviderSelector;
  private final SeedProperties seedProperties;
  private final UserSeedService userSeedService;
  private final GameSeedService gameSeedService;

  /** Initialise les donnees de test au demarrage de l'application */
  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void initializeTestData() {
    if (!shouldInitialize()) {
      return;
    }

    if (hasExistingData() && !handleExistingData()) {
      return;
    }

    try {
      List<User> allUsers = initializeUsers();
      List<Player> savedPlayers = initializePlayers();
      List<Team> savedTeams = initializeTeams(allUsers);

      log.info("Creating test games with real teams...");
      gameSeedService.createTestGamesWithRealTeams(allUsers, savedTeams);

      logSummary(allUsers, savedPlayers, savedTeams);

    } catch (Exception e) {
      log.error("Error during test data initialization", e);
      log.error("Error message: {}", e.getMessage());
      handleInitializationError();
    }
  }

  private boolean shouldInitialize() {
    if (!seedProperties.isEnabled()) {
      log.info("Seed disabled (fortnite.seed.enabled=false)");
      return false;
    }
    if (!seedProperties.isLegacyEnabled()) {
      log.info("Skipping legacy data initialization (fortnite.seed.legacy-enabled=false)");
      return false;
    }
    if (!isDevProfile()) {
      log.info("Skipping test data initialization - not in dev mode");
      return false;
    }
    return true;
  }

  private boolean handleExistingData() {
    if (!isSeedResetEnabled()) {
      log.info(
          "Seed skipped: existing data detected. Set {}=true to re-seed.", SEED_RESET_PROPERTY);
      return false;
    }

    log.info("Seed reset enabled. Clearing existing data before CSV re-seed.");
    clearAllData();
    return true;
  }

  private void clearAllData() {
    teamRepository.deleteAll();
    scoreRepository.deleteAll();
    gameRepository.deleteAll();
    playerRepository.deleteAll();
    userRepository.deleteAll();
    log.info("Database cleared before seed.");
  }

  private List<User> initializeUsers() {
    List<User> usersToCreate = userSeedService.createDefaultUsers();
    userSeedService.saveUsers(usersToCreate);

    List<User> allUsers = userSeedService.getAllUsers();
    log.info("Total users in database: {}", allUsers.size());
    return allUsers;
  }

  private List<Player> initializePlayers() {
    log.info("Loading data from CSV...");
    MockDataGeneratorService.MockDataSet mockData = seedDataProviderSelector.loadSeedData();

    if (mockData.total() == 0) {
      log.warn("No mock data loaded, falling back to CsvDataLoaderService");
      csvDataLoaderService.loadAllCsvData();
    } else {
      saveMockPlayersAndScores(mockData);
    }

    List<Player> savedPlayers = playerRepository.findAll();
    log.info("{} real players loaded from CSV with their complete scores", savedPlayers.size());
    return savedPlayers;
  }

  private void saveMockPlayersAndScores(MockDataGeneratorService.MockDataSet mockData) {
    List<Player> mockPlayers = mockData.getAllPlayers();
    List<Player> savedMockPlayers = playerRepository.saveAll(mockPlayers);

    List<Score> mockScores = mockData.getAllScores();
    for (int i = 0; i < savedMockPlayers.size() && i < mockScores.size(); i++) {
      mockScores.get(i).setPlayer(savedMockPlayers.get(i));
    }
    scoreRepository.saveAll(mockScores);

    log.info("{} mock players loaded from CSV", savedMockPlayers.size());
  }

  private List<Team> initializeTeams(List<User> allUsers) {
    log.info("Creating teams with real players from CSV assignments...");
    List<Team> teams = createTeamsFromCsvAssignments(allUsers);
    List<Team> savedTeams = teamRepository.saveAll(teams);
    log.info("{} teams created with real players from CSV", savedTeams.size());
    return savedTeams;
  }

  private List<Team> createTeamsFromCsvAssignments(List<User> users) {
    MockDataGeneratorService.MockDataSet mockData = seedDataProviderSelector.loadSeedData();

    if (mockData.total() == 0) {
      log.warn("No mock data available, using fallback method");
      return createFallbackTeams(users);
    }

    List<Team> teams = new ArrayList<>();
    List<String> pronosticators = mockData.getPronosticators();

    for (String pronostiqueur : pronosticators) {
      Team team = createTeamForPronostiqueur(pronostiqueur, users, mockData);
      if (team != null) {
        teams.add(team);
      }
    }

    log.info("{} teams created from CSV assignments", teams.size());
    return teams;
  }

  private Team createTeamForPronostiqueur(
      String pronostiqueur, List<User> users, MockDataGeneratorService.MockDataSet mockData) {

    List<MockDataGeneratorService.PlayerWithScore> playerDataList =
        mockData.getPlayersFor(pronostiqueur);

    if (playerDataList.isEmpty()) {
      log.warn("No players assigned for {}, team skipped", pronostiqueur);
      return null;
    }

    User owner =
        users.stream()
            .filter(u -> u.getUsername().equalsIgnoreCase(pronostiqueur))
            .findFirst()
            .orElse(null);

    if (owner == null) {
      log.warn("User not found for pronosticator '{}', team skipped", pronostiqueur);
      return null;
    }

    List<Player> assignedPlayers = findPlayersForTeam(playerDataList);
    if (assignedPlayers.isEmpty()) {
      log.warn("No players found in DB for {}, team skipped", pronostiqueur);
      return null;
    }

    String teamName = "Equipe " + pronostiqueur;
    Team team = createTeam(teamName, owner, assignedPlayers);
    log.info("{} created with {} players assigned from CSV", teamName, assignedPlayers.size());
    return team;
  }

  private List<Player> findPlayersForTeam(
      List<MockDataGeneratorService.PlayerWithScore> playerDataList) {

    List<Player> assignedPlayers = new ArrayList<>();
    for (MockDataGeneratorService.PlayerWithScore playerData : playerDataList) {
      String username = playerData.player().getUsername();
      List<Player> matchingPlayers =
          playerRepository.findAll().stream()
              .filter(p -> p.getUsername().equals(username))
              .limit(1)
              .toList();
      if (!matchingPlayers.isEmpty()) {
        assignedPlayers.add(matchingPlayers.get(0));
      }
    }
    return assignedPlayers;
  }

  private List<Team> createFallbackTeams(List<User> users) {
    log.info("Using fallback method to create teams");

    List<Player> allPlayers = playerRepository.findAll();
    if (allPlayers.size() < 3) {
      log.warn("Not enough players for fallback teams");
      return new ArrayList<>();
    }

    List<User> participants =
        users.stream().filter(u -> u.getRole() == User.UserRole.USER).toList();

    if (participants.size() < 3) {
      log.warn("Not enough participant users for teams");
      return new ArrayList<>();
    }

    List<Team> teams = new ArrayList<>();
    int playersPerTeam = Math.max(1, allPlayers.size() / participants.size());

    for (int i = 0; i < Math.min(participants.size(), 3); i++) {
      User owner = participants.get(i);
      String teamName = "Equipe " + owner.getUsername();

      int startIndex = i * playersPerTeam;
      int endIndex = Math.min(startIndex + playersPerTeam, allPlayers.size());
      List<Player> teamPlayers = allPlayers.subList(startIndex, endIndex);

      if (!teamPlayers.isEmpty()) {
        Team team = createTeam(teamName, owner, teamPlayers);
        teams.add(team);
        log.info("{} created with {} players (fallback)", teamName, teamPlayers.size());
      }
    }

    return teams;
  }

  private Team createTeam(String name, User owner, List<Player> players) {
    Team team = new Team();
    team.setOwner(owner);
    team.setName(name);
    team.setSeason(2025);

    for (int i = 0; i < players.size(); i++) {
      Player player = players.get(i);
      if (player.getId() != null) {
        player = playerRepository.findById(player.getId()).orElse(player);
      }
      team.addPlayer(player, i + 1);
    }

    return team;
  }

  private void logSummary(List<User> allUsers, List<Player> savedPlayers, List<Team> savedTeams) {
    long scoreCount = scoreRepository.count();
    log.info("Real data initialized successfully from CSV");
    log.info("   Users: {}", allUsers.size());
    log.info("   Real players: {}", savedPlayers.size());
    log.info("   Teams: {}", savedTeams.size());
    log.info("   Real scores: {}", scoreCount);
  }

  private void handleInitializationError() {
    try {
      userSeedService.createMinimalTestData();
    } catch (Exception fallbackError) {
      log.error("Critical error during fallback", fallbackError);
    }
  }

  private boolean isDevProfile() {
    return Arrays.asList(environment.getActiveProfiles()).contains("dev");
  }

  private boolean isSeedResetEnabled() {
    return seedProperties.isReset();
  }

  private boolean hasExistingData() {
    return userRepository.count() > 0 || playerRepository.count() > 0;
  }
}
