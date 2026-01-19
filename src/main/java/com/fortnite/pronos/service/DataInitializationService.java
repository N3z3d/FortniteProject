package com.fortnite.pronos.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.config.SeedProperties;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.seed.GameSeedService;
import com.fortnite.pronos.service.seed.PlayerSeedService;
import com.fortnite.pronos.service.seed.TeamSeedService;
import com.fortnite.pronos.service.seed.UserSeedService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service d'initialisation des donnees de test. Orchestrates seeding via specialized seed services.
 *
 * <p>Refactored to reduce coupling: 6 dependencies (was 11).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService {

  private static final String SEED_RESET_PROPERTY = "fortnite.seed.reset";

  // Configuration dependencies (2)
  private final SeedProperties seedProperties;
  private final Environment environment;

  // Seed service dependencies (4)
  private final UserSeedService userSeedService;
  private final PlayerSeedService playerSeedService;
  private final TeamSeedService teamSeedService;
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
      List<Player> savedPlayers = playerSeedService.initializePlayers();
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
    // Note: clearing is handled by the seed services when reset is enabled
    return true;
  }

  private List<User> initializeUsers() {
    List<User> usersToCreate = userSeedService.createDefaultUsers();
    userSeedService.saveUsers(usersToCreate);

    List<User> allUsers = userSeedService.getAllUsers();
    log.info("Total users in database: {}", allUsers.size());
    return allUsers;
  }

  private List<Team> initializeTeams(List<User> allUsers) {
    log.info("Creating teams with real players from CSV assignments...");
    MockDataGeneratorService.MockDataSet mockData = playerSeedService.loadSeedData();
    return teamSeedService.createTeamsFromCsvAssignments(allUsers, mockData);
  }

  private void logSummary(List<User> allUsers, List<Player> savedPlayers, List<Team> savedTeams) {
    long scoreCount = playerSeedService.getScoreCount();
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
    return userSeedService.getAllUsers().size() > 0 || playerSeedService.getPlayerCount() > 0;
  }
}
