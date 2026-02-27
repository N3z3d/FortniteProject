package com.fortnite.pronos.service.seed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.service.MockDataGeneratorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for seeding teams during initialization. Extracted from DataInitializationService for SRP
 * compliance and reduced coupling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"java:S1488"})
public class TeamSeedService {

  private static final int MIN_FALLBACK_PLAYERS = 3;
  private static final int MIN_FALLBACK_PARTICIPANTS = 3;
  private static final int MAX_FALLBACK_TEAMS = 3;
  private static final int DEFAULT_SEASON = 2025;

  private final com.fortnite.pronos.repository.TeamRepository teamRepository;
  private final com.fortnite.pronos.repository.PlayerRepository playerRepository;

  /**
   * Creates teams from CSV assignments.
   *
   * @param users list of users to assign as owners
   * @param mockData seed data containing player assignments
   * @return list of saved teams
   */
  public List<com.fortnite.pronos.model.Team> createTeamsFromCsvAssignments(
      Collection<com.fortnite.pronos.model.User> users,
      MockDataGeneratorService.MockDataSet mockData) {

    if (mockData.total() == 0) {
      log.warn("No mock data available, using fallback method");
      return createFallbackTeams(users);
    }

    List<com.fortnite.pronos.model.Team> teams = new ArrayList<>();
    List<String> pronosticators = mockData.getPronosticators();

    for (String pronostiqueur : pronosticators) {
      com.fortnite.pronos.model.Team team =
          createTeamForPronostiqueur(pronostiqueur, users, mockData);
      if (team != null) {
        teams.add(team);
      }
    }

    List<com.fortnite.pronos.model.Team> savedTeams = teamRepository.saveAll(teams);
    log.info("{} teams created from CSV assignments", savedTeams.size());
    return savedTeams;
  }

  private com.fortnite.pronos.model.Team createTeamForPronostiqueur(
      String pronostiqueur,
      Collection<com.fortnite.pronos.model.User> users,
      MockDataGeneratorService.MockDataSet mockData) {

    List<MockDataGeneratorService.PlayerWithScore> playerDataList =
        mockData.getPlayersFor(pronostiqueur);

    if (playerDataList.isEmpty()) {
      log.warn("No players assigned for {}, team skipped", pronostiqueur);
      return null;
    }

    com.fortnite.pronos.model.Team team = null;
    com.fortnite.pronos.model.User owner = findOwnerByUsername(users, pronostiqueur);
    if (owner != null) {
      List<com.fortnite.pronos.model.Player> assignedPlayers = findPlayersForTeam(playerDataList);
      if (assignedPlayers.isEmpty()) {
        log.warn("No players found in DB for {}, team skipped", pronostiqueur);
      } else {
        String teamName = "Equipe " + pronostiqueur;
        team = createTeam(teamName, owner, assignedPlayers);
        log.info("{} created with {} players assigned from CSV", teamName, assignedPlayers.size());
      }
    } else {
      log.warn("User not found for pronosticator '{}', team skipped", pronostiqueur);
    }
    return team;
  }

  private com.fortnite.pronos.model.User findOwnerByUsername(
      Collection<com.fortnite.pronos.model.User> users, String username) {
    return users.stream()
        .filter(u -> u.getUsername().equalsIgnoreCase(username))
        .findFirst()
        .orElse(null);
  }

  private List<com.fortnite.pronos.model.Player> findPlayersForTeam(
      Collection<MockDataGeneratorService.PlayerWithScore> playerDataList) {

    List<com.fortnite.pronos.model.Player> assignedPlayers = new ArrayList<>();
    for (MockDataGeneratorService.PlayerWithScore playerData : playerDataList) {
      String username = playerData.player().getUsername();
      List<com.fortnite.pronos.model.Player> matchingPlayers =
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

  /**
   * Creates fallback teams when CSV data is not available.
   *
   * @param users list of users to assign as owners
   * @return list of saved teams
   */
  public List<com.fortnite.pronos.model.Team> createFallbackTeams(
      Collection<com.fortnite.pronos.model.User> users) {
    log.info("Using fallback method to create teams");

    List<com.fortnite.pronos.model.Player> allPlayers = playerRepository.findAll();
    if (allPlayers.size() < MIN_FALLBACK_PLAYERS) {
      log.warn("Not enough players for fallback teams");
      return new ArrayList<>();
    }

    List<com.fortnite.pronos.model.User> participants =
        users.stream()
            .filter(u -> u.getRole() == com.fortnite.pronos.model.User.UserRole.USER)
            .toList();

    if (participants.size() < MIN_FALLBACK_PARTICIPANTS) {
      log.warn("Not enough participant users for teams");
      return new ArrayList<>();
    }

    List<com.fortnite.pronos.model.Team> teams = new ArrayList<>();
    int playersPerTeam = Math.max(1, allPlayers.size() / participants.size());

    for (int i = 0; i < Math.min(participants.size(), MAX_FALLBACK_TEAMS); i++) {
      com.fortnite.pronos.model.User owner = participants.get(i);
      String teamName = "Equipe " + owner.getUsername();

      int startIndex = i * playersPerTeam;
      int endIndex = Math.min(startIndex + playersPerTeam, allPlayers.size());
      List<com.fortnite.pronos.model.Player> teamPlayers = allPlayers.subList(startIndex, endIndex);

      if (!teamPlayers.isEmpty()) {
        com.fortnite.pronos.model.Team team = createTeam(teamName, owner, teamPlayers);
        teams.add(team);
        log.info("{} created with {} players (fallback)", teamName, teamPlayers.size());
      }
    }

    List<com.fortnite.pronos.model.Team> savedTeams = teamRepository.saveAll(teams);
    return savedTeams;
  }

  private com.fortnite.pronos.model.Team createTeam(
      String name,
      com.fortnite.pronos.model.User owner,
      Collection<com.fortnite.pronos.model.Player> players) {
    com.fortnite.pronos.model.Team team = new com.fortnite.pronos.model.Team();
    team.setOwner(owner);
    team.setName(name);
    team.setSeason(DEFAULT_SEASON);

    int position = 1;
    for (com.fortnite.pronos.model.Player player : players) {
      com.fortnite.pronos.model.Player managedPlayer = resolveManagedPlayer(player);
      team.addPlayer(managedPlayer, position);
      position = position + 1;
    }

    return team;
  }

  private com.fortnite.pronos.model.Player resolveManagedPlayer(
      com.fortnite.pronos.model.Player player) {
    if (player.getId() == null) {
      return player;
    }
    return ((PlayerRepositoryPort) playerRepository).findById(player.getId()).orElse(player);
  }

  /** Returns all teams in the repository. */
  public List<com.fortnite.pronos.model.Team> getAllTeams() {
    return teamRepository.findAll();
  }
}
