package com.fortnite.pronos.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service to initialize teams from CSV assignments. */
@Service
@ConditionalOnProperty(name = "fortnite.data.provider", havingValue = "csv", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class TeamInitializationService {

  private static final int CURRENT_SEASON = 2025;

  private final CsvDataLoaderService csvDataLoaderService;
  private final Environment environment;
  private final UserRepositoryPort userRepository;
  private final TeamRepository teamRepository;
  private final PlayerRepository playerRepository;

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
    Map<String, List<Player>> playerAssignments = csvDataLoaderService.getAllPlayerAssignments();
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

    for (Map.Entry<String, List<Player>> entry : playerAssignments.entrySet()) {
      createTeamIfMissing(entry, existingOwners, stats);
    }

    logInitializationSummary(stats);
  }

  private boolean isTestProfile() {
    return Arrays.asList(environment.getActiveProfiles()).contains("test");
  }

  private Map<String, Boolean> buildExistingOwners(List<Team> existingTeams) {
    Map<String, Boolean> existingOwners = new HashMap<>();
    for (Team team : existingTeams) {
      if (team.getOwner() != null && team.getOwner().getUsername() != null) {
        existingOwners.put(team.getOwner().getUsername().toLowerCase(), true);
      }
    }
    return existingOwners;
  }

  private void createTeamIfMissing(
      Map.Entry<String, List<Player>> entry,
      Map<String, Boolean> existingOwners,
      TeamInitializationStats stats) {
    String pronostiqueurName = entry.getKey();
    List<Player> assignedPlayers = entry.getValue();

    if (assignedPlayers.isEmpty()) {
      log.warn("Team initialization: no players assigned to {}", pronostiqueurName);
      return;
    }
    if (existingOwners.containsKey(pronostiqueurName.toLowerCase())) {
      return;
    }

    User user = findOrCreateUser(pronostiqueurName);
    Team team = buildTeam(pronostiqueurName, user, assignedPlayers);
    teamRepository.save(team);
    existingOwners.put(pronostiqueurName.toLowerCase(), true);

    stats.teamsCreated++;
    stats.totalPlayersAssigned += assignedPlayers.size();
    log.info(
        "Team initialization: team created name={}, owner={}, players={}",
        team.getName(),
        pronostiqueurName,
        assignedPlayers.size());
  }

  private User findOrCreateUser(String pronostiqueurName) {
    return userRepository
        .findByUsernameIgnoreCase(pronostiqueurName)
        .orElseGet(() -> createUserForPronostiqueur(pronostiqueurName));
  }

  private Team buildTeam(String pronostiqueurName, User owner, List<Player> assignedPlayers) {
    Team team = new Team();
    team.setName("Equipe de " + pronostiqueurName);
    team.setOwner(owner);
    team.setSeason(CURRENT_SEASON);
    team.setCompletedTradesCount(0);

    int position = 1;
    for (Player player : assignedPlayers) {
      Player managedPlayer = playerRepository.getReferenceById(player.getId());
      team.addPlayer(managedPlayer, position++);
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
  private User createUserForPronostiqueur(String pronostiqueurName) {
    String cleanUsername = pronostiqueurName.toLowerCase().replaceAll("[^a-z0-9]", "");
    if (cleanUsername.isEmpty()) {
      cleanUsername = "pronostiqueur" + Math.abs(pronostiqueurName.hashCode());
    }

    String email = cleanUsername + "@fortnite-pronos.local";

    User user = new User();
    user.setUsername(cleanUsername);
    user.setEmail(email);
    user.setPassword("$2a$10$DummyHashForInitialUsers");

    User savedUser = userRepository.save(user);
    log.debug(
        "Team initialization: user created username={}, email={}",
        savedUser.getUsername(),
        savedUser.getEmail());

    return savedUser;
  }

  private static final class TeamInitializationStats {
    private int teamsCreated;
    private int totalPlayersAssigned;
  }
}
