package com.fortnite.pronos.service.seed;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for seeding games during initialization. Extracted from DataInitializationService for SRP
 * compliance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameSeedService {

  private final GameRepositoryPort gameRepository;

  /**
   * Creates test games with real teams based on CSV assignments.
   *
   * @param users list of users to participate
   * @param realTeams list of teams created from CSV
   */
  public void createTestGamesWithRealTeams(List<User> users, List<Team> realTeams) {
    List<User> participants = filterParticipants(users);

    if (participants.size() < 3 || realTeams.size() < 3) {
      log.warn(
          "Not enough participants ({}) or teams ({}) for complete games",
          participants.size(),
          realTeams.size());
      return;
    }

    try {
      User thibaut = findUserByName(participants, "Thibaut", 0);
      User teddy = findUserByName(participants, "Teddy", 1);
      User marcel = findUserByName(participants, "Marcel", 2);

      Game gameActive =
          createGame(
              "Fantasy League 2025 - Championnat Principal",
              "Game principale avec les equipes reelles basees sur les donnees CSV",
              thibaut,
              3,
              GameStatus.ACTIVE);

      gameActive.addParticipant(createGameParticipant(gameActive, thibaut, 1));
      gameActive.addParticipant(createGameParticipant(gameActive, teddy, 2));
      gameActive.addParticipant(createGameParticipant(gameActive, marcel, 3));

      gameRepository.save(gameActive);
      log.info("Seed game created with 3 participants and real teams");

    } catch (Exception e) {
      log.error("Seed game creation failed", e);
    }
  }

  /**
   * Creates fallback test games when CSV data is not available.
   *
   * @param users list of users to participate
   */
  public void createTestGames(List<User> users) {
    List<User> participants = filterParticipants(users);

    if (participants.size() < 3) {
      log.warn("Not enough participants for complete games");
      return;
    }

    try {
      User thibaut = findUserByName(participants, "Thibaut", 0);
      User teddy = findUserByName(participants, "Teddy", 1);
      User marcel = findUserByName(participants, "Marcel", 2);

      // Main active game
      Game gameActive =
          createGame(
              "Fantasy League Pro 2025",
              "Championnat principal avec Thibaut, Teddy et Marcel - Saison 2025",
              thibaut,
              3,
              GameStatus.ACTIVE);

      gameActive.addParticipant(createGameParticipant(gameActive, thibaut, 1));
      gameActive.addParticipant(createGameParticipant(gameActive, teddy, 2));
      gameActive.addParticipant(createGameParticipant(gameActive, marcel, 3));
      gameRepository.save(gameActive);
      log.info("Main game '{}' created with 3 participants", gameActive.getName());

      // Draft game
      Game gameDraft =
          createGame(
              "Draft League - Teddy",
              "Nouvelle game en phase de draft organisee par Teddy",
              teddy,
              3,
              GameStatus.DRAFTING);

      gameDraft.addParticipant(createGameParticipant(gameDraft, teddy, 1));
      gameDraft.addParticipant(createGameParticipant(gameDraft, thibaut, 2));
      gameDraft.addParticipant(createGameParticipant(gameDraft, marcel, 3));
      gameRepository.save(gameDraft);
      log.info("Draft game '{}' created with 3 participants", gameDraft.getName());

      // Creating game
      Game gameCreating =
          createGame(
              "Championship 2025 - Marcel",
              "Nouveau championnat en preparation par Marcel",
              marcel,
              4,
              GameStatus.CREATING);

      gameCreating.addParticipant(createGameParticipant(gameCreating, marcel, 1));
      gameRepository.save(gameCreating);
      log.info("Creating game '{}' created with 1 participant", gameCreating.getName());

      log.info("{} test games created", gameRepository.count());

    } catch (Exception e) {
      log.error("Error creating test games", e);
    }
  }

  private List<User> filterParticipants(List<User> users) {
    return users.stream().filter(u -> u.getRole() == User.UserRole.USER).toList();
  }

  private User findUserByName(List<User> participants, String name, int fallbackIndex) {
    return participants.stream()
        .filter(u -> name.equals(u.getUsername()))
        .findFirst()
        .orElse(participants.get(fallbackIndex));
  }

  /** Creates a game with specified parameters. */
  public Game createGame(
      String name, String description, User creator, int maxParticipants, GameStatus status) {

    Game game =
        Game.builder()
            .name(name)
            .description(description)
            .creator(creator)
            .maxParticipants(maxParticipants)
            .status(status)
            .participants(new ArrayList<>())
            .regionRules(new ArrayList<>())
            .build();

    addBasicRegionRules(game);
    return game;
  }

  /** Creates a game participant. */
  public GameParticipant createGameParticipant(Game game, User user, int draftOrder) {
    return GameParticipant.builder()
        .game(game)
        .user(user)
        .draftOrder(draftOrder)
        .selectedPlayers(new ArrayList<>())
        .build();
  }

  /** Adds basic region rules to a game. */
  public void addBasicRegionRules(Game game) {
    game.addRegionRule(
        GameRegionRule.builder().game(game).region(Player.Region.EU).maxPlayers(2).build());
    game.addRegionRule(
        GameRegionRule.builder().game(game).region(Player.Region.NAC).maxPlayers(2).build());
    game.addRegionRule(
        GameRegionRule.builder().game(game).region(Player.Region.ASIA).maxPlayers(2).build());
  }
}
