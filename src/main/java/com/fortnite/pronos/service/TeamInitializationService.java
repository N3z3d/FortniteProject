package com.fortnite.pronos.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.port.out.TeamRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service to initialize teams from CSV assignments. */
@Service
@ConditionalOnProperty(name = "fortnite.data.provider", havingValue = "csv", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"java:S2676"})
public class TeamInitializationService {

  private static final int CURRENT_SEASON = 2025;
  private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]");

  private final CsvDataLoaderService csvDataLoaderService;
  private final Environment environment;
  private final UserRepositoryPort userRepository;
  private final com.fortnite.pronos.repository.TeamRepository teamRepository;
  private final com.fortnite.pronos.repository.PlayerRepository playerRepository;

  /** Initialize teams after CSV load. Runs after CsvBootstrapService. */
  @EventListener(ApplicationReadyEvent.class)
  @Order(2)
  @Transactional
  public void createTeamsFromCsvDataOnStartup() {
    if (isTestProfile()) {
      log.info("Team initialization: skipped for test profile");
      return;
    }

    createTeamsFromCsvData();
  }

  @Transactional
  public void createTeamsFromCsvData() {
    Map<String, List<com.fortnite.pronos.model.Player>> playerAssignments =
        csvDataLoaderService.getAllPlayerAssignments();
    if (playerAssignments.isEmpty()) {
      log.warn("Team initialization: no assignments found in memory, loading CSV");
      csvDataLoaderService.loadAllCsvData();
      playerAssignments = csvDataLoaderService.getAllPlayerAssignments();
    }
    if (playerAssignments.isEmpty()) {
      log.warn("Team initialization: no assignments found in CSV");
      return;
    }

    Map<String, Boolean> existingOwners =
        buildExistingOwners(teamRepository.findBySeason(CURRENT_SEASON));
    TeamInitializationStats stats = new TeamInitializationStats();

    for (Map.Entry<String, List<com.fortnite.pronos.model.Player>> entry :
        playerAssignments.entrySet()) {
      createTeamIfMissing(entry, existingOwners, stats);
    }

    logInitializationSummary(stats);
  }

  private boolean isTestProfile() {
    return Arrays.asList(environment.getActiveProfiles()).contains("test");
  }

  private Map<String, Boolean> buildExistingOwners(
      List<com.fortnite.pronos.model.Team> existingTeams) {
    Map<String, Boolean> existingOwners = new HashMap<>();
    for (com.fortnite.pronos.model.Team team : existingTeams) {
      if (team.getOwner() != null && team.getOwner().getUsername() != null) {
        existingOwners.put(toOwnerKey(team.getOwner().getUsername()), true);
      }
    }
    return existingOwners;
  }

  private void createTeamIfMissing(
      Map.Entry<String, List<com.fortnite.pronos.model.Player>> entry,
      Map<String, Boolean> existingOwners,
      TeamInitializationStats stats) {
    String pronostiqueurName = entry.getKey();
    List<com.fortnite.pronos.model.Player> assignedPlayers = entry.getValue();

    if (assignedPlayers.isEmpty()) {
      log.warn("Team initialization: no players assigned to {}", pronostiqueurName);
      return;
    }
    if (existingOwners.containsKey(toOwnerKey(pronostiqueurName))) {
      return;
    }

    com.fortnite.pronos.model.User user = findOrCreateUser(pronostiqueurName);
    com.fortnite.pronos.model.Team team = buildTeam(pronostiqueurName, user, assignedPlayers);
    ((TeamRepositoryPort) teamRepository).save(team);
    existingOwners.put(toOwnerKey(pronostiqueurName), true);

    stats.teamsCreated++;
    stats.totalPlayersAssigned += assignedPlayers.size();
    log.info(
        "Team initialization: team created name={}, owner={}, players={}",
        team.getName(),
        pronostiqueurName,
        assignedPlayers.size());
  }

  private com.fortnite.pronos.model.User findOrCreateUser(String pronostiqueurName) {
    return userRepository
        .findByUsernameIgnoreCase(pronostiqueurName)
        .orElseGet(() -> createUserForPronostiqueur(pronostiqueurName));
  }

  private com.fortnite.pronos.model.Team buildTeam(
      String pronostiqueurName,
      com.fortnite.pronos.model.User owner,
      List<com.fortnite.pronos.model.Player> assignedPlayers) {
    com.fortnite.pronos.model.Team team = new com.fortnite.pronos.model.Team();
    team.setName("Equipe de " + pronostiqueurName);
    team.setOwner(owner);
    team.setSeason(CURRENT_SEASON);
    team.setCompletedTradesCount(0);

    int position = 1;
    for (com.fortnite.pronos.model.Player player : assignedPlayers) {
      com.fortnite.pronos.model.Player managedPlayer =
          playerRepository.getReferenceById(player.getId());
      team.addPlayer(managedPlayer, position);
      position = position + 1;
    }

    return team;
  }

  private void logInitializationSummary(TeamInitializationStats stats) {
    log.info("Team initialization: completed");
    log.info("  - teamsCreated={}", stats.teamsCreated);
    log.info("  - totalPlayersAssigned={}", stats.totalPlayersAssigned);
    log.info("  - season={}", CURRENT_SEASON);
  }

  /** Create a basic user for a pronosticator. */
  private com.fortnite.pronos.model.User createUserForPronostiqueur(String pronostiqueurName) {
    String cleanUsername =
        NON_ALPHANUMERIC_PATTERN.matcher(pronostiqueurName.toLowerCase(Locale.ROOT)).replaceAll("");
    if (cleanUsername.isEmpty()) {
      cleanUsername = "pronostiqueur" + Integer.toUnsignedString(pronostiqueurName.hashCode());
    }

    String email = cleanUsername + "@fortnite-pronos.local";

    com.fortnite.pronos.model.User user = new com.fortnite.pronos.model.User();
    user.setUsername(cleanUsername);
    user.setEmail(email);
    user.setPassword("$2a$10$DummyHashForInitialUsers");

    com.fortnite.pronos.model.User savedUser = userRepository.save(user);
    log.debug(
        "Team initialization: user created username={}, email={}",
        savedUser.getUsername(),
        savedUser.getEmail());

    return savedUser;
  }

  private String toOwnerKey(String username) {
    return username.toLowerCase(Locale.ROOT);
  }

  private static final class TeamInitializationStats {
    private int teamsCreated;
    private int totalPlayersAssigned;
  }
}
