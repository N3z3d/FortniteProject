package com.fortnite.pronos.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** Initialise les donnees de test pour les suites d'integration (profil test). */
@Configuration
@Profile("test")
@RequiredArgsConstructor
public class TestDataInitializerConfig {

  private static final Logger log = LoggerFactory.getLogger(TestDataInitializerConfig.class);

  private final UserRepository userRepository;
  private final PlayerRepository playerRepository;
  private final TeamRepository teamRepository;
  private final GameRepository gameRepository;
  private final TransactionTemplate transactionTemplate;

  private static final List<Player.Region> ALLOWED_REGIONS =
      List.of(
          Player.Region.EU,
          Player.Region.NAC,
          Player.Region.NAW,
          Player.Region.BR,
          Player.Region.ASIA,
          Player.Region.OCE,
          Player.Region.ME);

  @Bean
  public CommandLineRunner initTestData() {
    return args ->
        transactionTemplate.executeWithoutResult(
            status -> {
              log.info("Initialisation des donnees de test (profil test)...");

              teamRepository.deleteAllInBatch();
              gameRepository.deleteAllInBatch();
              playerRepository.deleteAllInBatch();
              userRepository.deleteAllInBatch();

              User thibaut =
                  createUserAsUser(
                      "Thibaut", UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));
              User marcel =
                  createUserAsUser(
                      "Marcel", UUID.fromString("550e8400-e29b-41d4-a716-446655440002"));
              User teddy =
                  createUserAsUser(
                      "Teddy", UUID.fromString("550e8400-e29b-41d4-a716-446655440003"));
              User sarah =
                  createUserAsUser(
                      "Sarah", UUID.fromString("550e8400-e29b-41d4-a716-446655440004"));

              List<Player> players = generatePlayers(147);

              Game premiereSaison =
                  createPremiereSaisonGame(
                      UUID.fromString("880e8400-e29b-41d4-a716-446655440000"),
                      marcel,
                      thibaut,
                      marcel,
                      teddy);
              Game savedGame = gameRepository.saveAndFlush(premiereSaison);
              log.info("Game test enregistre avec id={}", savedGame.getId());

              Team teamThibaut = createTeam("Team Thibaut", thibaut, players.subList(0, 5));
              Team teamTeddy = createTeam("Team Teddy", teddy, players.subList(5, 10));
              Team teamMarcel = createTeam("Team Marcel", marcel, players.subList(10, 15));
              createTeam("Équipe Sarah", sarah, List.of());

              attachTeamsToGame(savedGame, List.of(teamThibaut, teamMarcel, teamTeddy));

              log.info("Donnees de test initialisees avec succes");
              log.info("- 4 utilisateurs crees");
              log.info("- {} joueurs Fortnite crees", players.size());
              log.info("- 3 equipes avec joueurs, 1 equipe vide");
              log.info("- 1 game active creee avec regles regionales et participants");
            });
  }

  private User createUserAsUser(String username, UUID id) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    user.setEmail(username.toLowerCase() + "@test.com");
    user.setPassword("password123");
    user.setRole(User.UserRole.USER);
    user.setCurrentSeason(2025);
    return userRepository.save(user);
  }

  private List<Player> generatePlayers(int count) {
    List<Player> players = new ArrayList<>(count);
    for (int i = 1; i <= count; i++) {
      Player player = new Player();
      player.setNickname("Player" + String.format("%03d", i));
      player.setUsername("player" + String.format("%03d", i));
      player.setFortniteId("FN_" + String.format("%03d", i));
      player.setRegion(ALLOWED_REGIONS.get((i - 1) % ALLOWED_REGIONS.size()));
      player.setTranche("1-7");
      player.setCurrentSeason(2025);
      players.add(playerRepository.save(player));
    }
    return players;
  }

  private Team createTeam(String name, User owner, List<Player> roster) {
    Team team = new Team();
    team.setName(name);
    team.setOwner(owner);
    team.setSeason(2025);

    int position = 1;
    List<TeamPlayer> teamPlayers = new ArrayList<>();
    for (Player player : roster) {
      TeamPlayer tp = new TeamPlayer();
      tp.setTeam(team);
      Player managedPlayer =
          playerRepository
              .findById(player.getId())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Joueur introuvable pour l'initialisation de test: " + player.getId()));
      tp.setPlayer(managedPlayer);
      tp.setPosition(position++);
      teamPlayers.add(tp);
    }
    team.setPlayers(teamPlayers);
    return teamRepository.save(team);
  }

  private Game createPremiereSaisonGame(
      UUID gameId,
      User creator,
      User firstParticipant,
      User secondParticipant,
      User thirdParticipant) {
    Game game = new Game();
    game.setId(gameId);
    game.setName("Première Saison");
    game.setCreator(creator);
    game.setMaxParticipants(10);
    game.setStatus(GameStatus.ACTIVE);
    game.setCreatedAt(LocalDateTime.now());
    game.setDescription("Game de test integration");

    for (Player.Region region : ALLOWED_REGIONS) {
      GameRegionRule rule = new GameRegionRule();
      rule.setGame(game);
      rule.setRegion(region);
      rule.setMaxPlayers(7);
      game.addRegionRule(rule);
    }

    addParticipant(game, firstParticipant, 1, creator.equals(firstParticipant));
    addParticipant(game, secondParticipant, 2, creator.equals(secondParticipant));
    addParticipant(game, thirdParticipant, 3, creator.equals(thirdParticipant));
    return game;
  }

  private void addParticipant(Game game, User user, int draftOrder, boolean creator) {
    GameParticipant participant = new GameParticipant();
    participant.setGame(game);
    participant.setUser(user);
    participant.setDraftOrder(draftOrder);
    participant.setJoinedAt(LocalDateTime.now());
    participant.setCreator(creator);
    game.addParticipant(participant);
  }

  private void attachTeamsToGame(Game game, List<Team> teams) {
    teams.forEach(
        team -> {
          team.setGame(game);
          teamRepository.save(team);
        });
    teamRepository.flush();
  }
}
