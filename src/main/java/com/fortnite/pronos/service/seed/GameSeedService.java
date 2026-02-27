package com.fortnite.pronos.service.seed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;

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

  private static final int MIN_REQUIRED_PARTICIPANTS = 3;
  private static final int THIBAUT_FALLBACK_INDEX = 0;
  private static final int TEDDY_FALLBACK_INDEX = 1;
  private static final int MARCEL_FALLBACK_INDEX = 2;
  private static final int FIRST_DRAFT_ORDER = 1;
  private static final int SECOND_DRAFT_ORDER = 2;
  private static final int THIRD_DRAFT_ORDER = 3;
  private static final int MAIN_GAME_MAX_PARTICIPANTS = 3;
  private static final int CREATING_GAME_MAX_PARTICIPANTS = 4;
  private static final int REGION_MAX_PLAYERS = 2;

  private final GameRepositoryPort gameRepository;

  /**
   * Creates test games with real teams based on CSV assignments.
   *
   * @param users list of users to participate
   * @param realTeams list of teams created from CSV
   */
  public void createTestGamesWithRealTeams(
      Collection<com.fortnite.pronos.model.User> users,
      Collection<com.fortnite.pronos.model.Team> realTeams) {
    List<com.fortnite.pronos.model.User> participants = filterParticipants(users);

    if (participants.size() < MIN_REQUIRED_PARTICIPANTS
        || realTeams.size() < MIN_REQUIRED_PARTICIPANTS) {
      log.warn(
          "Not enough participants ({}) or teams ({}) for complete games",
          participants.size(),
          realTeams.size());
      return;
    }

    try {
      SeedUsers seedUsers = resolveSeedUsers(participants);

      com.fortnite.pronos.model.Game gameActive =
          createGame(
              "Fantasy League 2025 - Championnat Principal",
              "Game principale avec les equipes reelles basees sur les donnees CSV",
              seedUsers.thibaut(),
              MAIN_GAME_MAX_PARTICIPANTS,
              com.fortnite.pronos.model.GameStatus.ACTIVE);

      gameActive.addParticipant(
          createGameParticipant(gameActive, seedUsers.thibaut(), FIRST_DRAFT_ORDER));
      gameActive.addParticipant(
          createGameParticipant(gameActive, seedUsers.teddy(), SECOND_DRAFT_ORDER));
      gameActive.addParticipant(
          createGameParticipant(gameActive, seedUsers.marcel(), THIRD_DRAFT_ORDER));

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
  public void createTestGames(Collection<com.fortnite.pronos.model.User> users) {
    List<com.fortnite.pronos.model.User> participants = filterParticipants(users);

    if (participants.size() < MIN_REQUIRED_PARTICIPANTS) {
      log.warn("Not enough participants for complete games");
      return;
    }

    try {
      SeedUsers seedUsers = resolveSeedUsers(participants);
      saveMainGame(seedUsers);
      saveDraftGame(seedUsers);
      saveCreatingGame(seedUsers.marcel());

      log.info("{} test games created", gameRepository.count());

    } catch (Exception e) {
      log.error("Error creating test games", e);
    }
  }

  private List<com.fortnite.pronos.model.User> filterParticipants(
      Collection<com.fortnite.pronos.model.User> users) {
    return users.stream()
        .filter(u -> u.getRole() == com.fortnite.pronos.model.User.UserRole.USER)
        .toList();
  }

  private com.fortnite.pronos.model.User findUserByName(
      List<com.fortnite.pronos.model.User> participants, String name, int fallbackIndex) {
    return participants.stream()
        .filter(u -> name.equals(u.getUsername()))
        .findFirst()
        .orElse(participants.get(fallbackIndex));
  }

  private SeedUsers resolveSeedUsers(List<com.fortnite.pronos.model.User> participants) {
    com.fortnite.pronos.model.User thibaut =
        findUserByName(participants, "Thibaut", THIBAUT_FALLBACK_INDEX);
    com.fortnite.pronos.model.User teddy =
        findUserByName(participants, "Teddy", TEDDY_FALLBACK_INDEX);
    com.fortnite.pronos.model.User marcel =
        findUserByName(participants, "Marcel", MARCEL_FALLBACK_INDEX);
    return new SeedUsers(thibaut, teddy, marcel);
  }

  private void saveMainGame(SeedUsers seedUsers) {
    com.fortnite.pronos.model.Game gameActive =
        createGame(
            "Fantasy League Pro 2025",
            "Championnat principal avec Thibaut, Teddy et Marcel - Saison 2025",
            seedUsers.thibaut(),
            MAIN_GAME_MAX_PARTICIPANTS,
            com.fortnite.pronos.model.GameStatus.ACTIVE);

    gameActive.addParticipant(
        createGameParticipant(gameActive, seedUsers.thibaut(), FIRST_DRAFT_ORDER));
    gameActive.addParticipant(
        createGameParticipant(gameActive, seedUsers.teddy(), SECOND_DRAFT_ORDER));
    gameActive.addParticipant(
        createGameParticipant(gameActive, seedUsers.marcel(), THIRD_DRAFT_ORDER));
    gameRepository.save(gameActive);
    log.info("Main game '{}' created with 3 participants", gameActive.getName());
  }

  private void saveDraftGame(SeedUsers seedUsers) {
    com.fortnite.pronos.model.Game gameDraft =
        createGame(
            "Draft League - Teddy",
            "Nouvelle game en phase de draft organisee par Teddy",
            seedUsers.teddy(),
            MAIN_GAME_MAX_PARTICIPANTS,
            com.fortnite.pronos.model.GameStatus.DRAFTING);

    gameDraft.addParticipant(
        createGameParticipant(gameDraft, seedUsers.teddy(), FIRST_DRAFT_ORDER));
    gameDraft.addParticipant(
        createGameParticipant(gameDraft, seedUsers.thibaut(), SECOND_DRAFT_ORDER));
    gameDraft.addParticipant(
        createGameParticipant(gameDraft, seedUsers.marcel(), THIRD_DRAFT_ORDER));
    gameRepository.save(gameDraft);
    log.info("Draft game '{}' created with 3 participants", gameDraft.getName());
  }

  private void saveCreatingGame(com.fortnite.pronos.model.User marcel) {
    com.fortnite.pronos.model.Game gameCreating =
        createGame(
            "Championship 2025 - Marcel",
            "Nouveau championnat en preparation par Marcel",
            marcel,
            CREATING_GAME_MAX_PARTICIPANTS,
            com.fortnite.pronos.model.GameStatus.CREATING);

    gameCreating.addParticipant(createGameParticipant(gameCreating, marcel, FIRST_DRAFT_ORDER));
    gameRepository.save(gameCreating);
    log.info("Creating game '{}' created with 1 participant", gameCreating.getName());
  }

  /** Creates a game with specified parameters. */
  public com.fortnite.pronos.model.Game createGame(
      String name,
      String description,
      com.fortnite.pronos.model.User creator,
      int maxParticipants,
      com.fortnite.pronos.model.GameStatus status) {

    com.fortnite.pronos.model.Game game =
        com.fortnite.pronos.model.Game.builder()
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
  public com.fortnite.pronos.model.GameParticipant createGameParticipant(
      com.fortnite.pronos.model.Game game, com.fortnite.pronos.model.User user, int draftOrder) {
    return com.fortnite.pronos.model.GameParticipant.builder()
        .game(game)
        .user(user)
        .draftOrder(draftOrder)
        .selectedPlayers(new ArrayList<>())
        .build();
  }

  /** Adds basic region rules to a game. */
  public void addBasicRegionRules(com.fortnite.pronos.model.Game game) {
    game.addRegionRule(
        com.fortnite.pronos.model.GameRegionRule.builder()
            .game(game)
            .region(com.fortnite.pronos.model.Player.Region.EU)
            .maxPlayers(REGION_MAX_PLAYERS)
            .build());
    game.addRegionRule(
        com.fortnite.pronos.model.GameRegionRule.builder()
            .game(game)
            .region(com.fortnite.pronos.model.Player.Region.NAC)
            .maxPlayers(REGION_MAX_PLAYERS)
            .build());
    game.addRegionRule(
        com.fortnite.pronos.model.GameRegionRule.builder()
            .game(game)
            .region(com.fortnite.pronos.model.Player.Region.ASIA)
            .maxPlayers(REGION_MAX_PLAYERS)
            .build());
  }

  private record SeedUsers(
      com.fortnite.pronos.model.User thibaut,
      com.fortnite.pronos.model.User teddy,
      com.fortnite.pronos.model.User marcel) {}
}
