package com.fortnite.pronos.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.model.*;

/**
 * Builder pour créer facilement des données de test valides Respecte toutes les contraintes de
 * validation
 */
public class TestDataBuilder {

  public static User createValidUser(String username) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(username.toLowerCase() + "@test.com");
    user.setPassword("$2a$10$dummy.hashed.password"); // BCrypt dummy hash
    user.setRole(User.UserRole.PARTICIPANT);
    user.setCurrentSeason(2025);
    return user;
  }

  public static User createThibaut() {
    User user = createValidUser("Thibaut");
    user.setEmail("thibaut@test.com");
    return user;
  }

  public static User createTeddy() {
    User user = createValidUser("Teddy");
    user.setEmail("teddy@test.com");
    return user;
  }

  public static User createMarcel() {
    User user = createValidUser("Marcel");
    user.setEmail("marcel@test.com");
    return user;
  }

  public static User createSarah() {
    User user = createValidUser("Sarah");
    user.setEmail("sarah@test.com");
    return user;
  }

  public static Player createValidPlayer(String nickname, Player.Region region) {
    Player player = new Player();
    player.setNickname(nickname);
    player.setUsername(nickname + "_fn");
    player.setFortniteId("FN_" + UUID.randomUUID().toString().substring(0, 8));
    player.setRegion(region);
    player.setTranche("1-7");
    player.setCurrentSeason(2025);
    return player;
  }

  public static Team createValidTeam(User owner, String name) {
    Team team = new Team();
    team.setName(name);
    team.setOwner(owner);
    team.setSeason(2025); // Set default season to avoid null constraint violation
    return team;
  }

  public static Game createValidGame(User creator, String name) {
    Game game = new Game();
    game.setName(name);
    game.setCreator(creator);
    game.setMaxParticipants(10);
    game.setStatus(GameStatus.CREATING);
    return game;
  }

  public static CreateGameRequest createValidGameRequest() {
    CreateGameRequest request = new CreateGameRequest();
    request.setName("Game de test");
    request.setMaxParticipants(10);
    request.setDescription("Une game de test pour les pronostics Fortnite");
    request.setIsPrivate(false);
    request.setAutoStartDraft(true);
    request.setDraftTimeLimit(300);
    request.setAutoPickDelay(43200);
    request.setCurrentSeason(2025);

    Map<Player.Region, Integer> regionRules = new HashMap<>();
    regionRules.put(Player.Region.EU, 5); // Réduit de 7 à 5
    regionRules.put(Player.Region.NAW, 5); // Réduit de 7 à 5
    // Total : 10, ne dépasse pas maxParticipants
    request.setRegionRules(regionRules);

    return request;
  }

  public static GameParticipant createValidParticipant(Game game, User user, int draftOrder) {
    GameParticipant participant = new GameParticipant();
    participant.setGame(game);
    participant.setUser(user);
    participant.setDraftOrder(draftOrder);
    return participant;
  }
}
