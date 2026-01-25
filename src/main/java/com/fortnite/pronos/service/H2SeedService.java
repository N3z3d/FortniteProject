package com.fortnite.pronos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service de seed minimal pour le profil H2. Crée des utilisateurs de test, des joueurs et une
 * partie pour permettre le développement local.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class H2SeedService {

  private static final String H2_GAME_NAME = "H2 Test Game";
  private static final int PLAYERS_PER_USER = 5;

  private final UserRepositoryPort userRepository;
  private final PlayerRepository playerRepository;
  private final GameRepositoryPort gameRepository;
  private final TeamRepository teamRepository;
  private final Environment environment;

  @EventListener(ApplicationReadyEvent.class)
  @Order(1)
  @Transactional
  public void seedTestData() {
    if (!isH2Profile()) {
      return;
    }

    log.info("H2 Seed: creating test data for H2 profile");

    // Create users
    User thibaut = createUserIfNotExists("thibaut", "thibaut@test.com", User.UserRole.ADMIN);
    User teddy = createUserIfNotExists("teddy", "teddy@test.com", User.UserRole.USER);
    User marcel = createUserIfNotExists("marcel", "marcel@test.com", User.UserRole.USER);
    User sarah = createUserIfNotExists("sarah", "sarah@test.com", User.UserRole.USER);

    log.info("H2 Seed: {} users in database", userRepository.count());

    // Create players if none exist
    if (playerRepository.count() == 0) {
      createTestPlayers();
    }

    // Create game if none exist
    if (gameRepository.count() == 0 && thibaut != null) {
      createTestGame(thibaut, List.of(thibaut, teddy, marcel, sarah));
    }

    log.info(
        "H2 Seed: completed - {} users, {} players, {} games",
        userRepository.count(),
        playerRepository.count(),
        gameRepository.count());
  }

  private boolean isH2Profile() {
    return Arrays.asList(environment.getActiveProfiles()).contains("h2");
  }

  private User createUserIfNotExists(String username, String email, User.UserRole role) {
    return userRepository
        .findByUsername(username)
        .orElseGet(
            () -> {
              User user = new User();
              user.setUsername(username);
              user.setEmail(email);
              user.setPassword("$2a$10$DummyHashForTestUsers");
              user.setRole(role);
              User saved = userRepository.save(user);
              log.info("H2 Seed: created user {} ({})", username, role);
              return saved;
            });
  }

  private void createTestPlayers() {
    List<Player> players = new ArrayList<>();
    String[] regions = {"EU", "NAC", "BR", "ASIA"};

    for (int i = 1; i <= 20; i++) {
      Player player = new Player();
      player.setNickname("Player" + i);
      player.setUsername("player" + i);
      player.setRegion(Player.Region.valueOf(regions[i % regions.length]));
      player.setTranche("1-5");
      player.setCurrentSeason(2025);
      players.add(player);
    }

    playerRepository.saveAll(players);
    log.info("H2 Seed: created {} test players", players.size());
  }

  private void createTestGame(User creator, List<User> participants) {
    Game game =
        Game.builder()
            .name(H2_GAME_NAME)
            .description("Test game for H2 development")
            .creator(creator)
            .maxParticipants(participants.size())
            .status(GameStatus.ACTIVE)
            .participants(new ArrayList<>())
            .regionRules(new ArrayList<>())
            .build();

    // Add region rules
    for (Player.Region region : Player.Region.values()) {
      GameRegionRule rule =
          GameRegionRule.builder().game(game).region(region).maxPlayers(5).build();
      game.addRegionRule(rule);
    }

    game.generateInvitationCode();

    // Add participants
    int order = 1;
    for (User user : participants) {
      if (user != null) {
        GameParticipant participant =
            GameParticipant.builder()
                .game(game)
                .user(user)
                .draftOrder(order++)
                .joinedAt(LocalDateTime.now())
                .creator(user.getId().equals(creator.getId()))
                .selectedPlayers(new ArrayList<>())
                .build();
        game.addParticipant(participant);
      }
    }

    Game savedGame = gameRepository.save(game);
    log.info("H2 Seed: created game '{}' with {} participants", H2_GAME_NAME, participants.size());

    // Create teams with players
    List<Player> allPlayers = playerRepository.findAll();
    int playerIndex = 0;

    for (User user : participants) {
      if (user != null && playerIndex < allPlayers.size()) {
        Team team = new Team();
        team.setName("Team " + user.getUsername());
        team.setOwner(user);
        team.setSeason(2025);
        team.setGame(savedGame);

        for (int pos = 1; pos <= PLAYERS_PER_USER && playerIndex < allPlayers.size(); pos++) {
          team.addPlayer(allPlayers.get(playerIndex++), pos);
        }

        teamRepository.save(team);
      }
    }
    log.info("H2 Seed: created {} teams", participants.size());
  }
}
