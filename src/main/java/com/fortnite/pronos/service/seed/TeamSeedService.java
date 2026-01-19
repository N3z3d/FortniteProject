package com.fortnite.pronos.service.seed;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
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
public class TeamSeedService {

  private final TeamRepository teamRepository;
  private final PlayerRepository playerRepository;

  /**
   * Creates teams from CSV assignments.
   *
   * @param users list of users to assign as owners
   * @param mockData seed data containing player assignments
   * @return list of saved teams
   */
  public List<Team> createTeamsFromCsvAssignments(
      List<User> users, MockDataGeneratorService.MockDataSet mockData) {

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

    List<Team> savedTeams = teamRepository.saveAll(teams);
    log.info("{} teams created from CSV assignments", savedTeams.size());
    return savedTeams;
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

  /**
   * Creates fallback teams when CSV data is not available.
   *
   * @param users list of users to assign as owners
   * @return list of saved teams
   */
  public List<Team> createFallbackTeams(List<User> users) {
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

    List<Team> savedTeams = teamRepository.saveAll(teams);
    return savedTeams;
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

  /** Returns all teams in the repository. */
  public List<Team> getAllTeams() {
    return teamRepository.findAll();
  }
}
