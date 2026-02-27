package com.fortnite.pronos.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.domain.port.out.TeamRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

/** Initialise les donnees de test pour les suites d'integration (profil test). */
@Configuration
@Profile("test")
@RequiredArgsConstructor
public class TestDataInitializerConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestDataInitializerConfig.class);

  private static final int TEST_SEASON = 2025;
  private static final int TOTAL_PLAYERS = 147;
  private static final int TEAM_SLOT_COUNT = 5;
  private static final int TEAM_TWO_START = 5;
  private static final int TEAM_THREE_START = 10;
  private static final int TEAM_THREE_END = 15;
  private static final int GAME_MAX_PARTICIPANTS = 10;
  private static final int REGION_MAX_PLAYERS = 7;
  private static final int FIRST_DRAFT_ORDER = 1;
  private static final int SECOND_DRAFT_ORDER = 2;
  private static final int THIRD_DRAFT_ORDER = 3;
  private static final String TEST_EMAIL_SUFFIX = "@test.com";
  private static final String TEST_PASSWORD = "password123";
  private static final String DEFAULT_TRANCHE = "1-7";

  private final UserRepositoryPort userRepository;
  private final PlayerRepository playerRepository;
  private final TeamRepository teamRepository;
  private final GameRepositoryPort gameRepository;
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
    return args -> executeInitializationInTransaction();
  }

  private void executeInitializationInTransaction() {
    transactionTemplate.executeWithoutResult(status -> initializeTestData());
  }

  private void initializeTestData() {
    LOGGER.info("Initialisation des donnees de test (profil test)...");

    teamRepository.deleteAllInBatch();
    gameRepository.deleteAllInBatch();
    playerRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();

    User thibaut =
        createUserAsUser("Thibaut", UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));
    User marcel =
        createUserAsUser("Marcel", UUID.fromString("550e8400-e29b-41d4-a716-446655440002"));
    User teddy = createUserAsUser("Teddy", UUID.fromString("550e8400-e29b-41d4-a716-446655440003"));
    User sarah = createUserAsUser("Sarah", UUID.fromString("550e8400-e29b-41d4-a716-446655440004"));

    List<Player> players = generatePlayers(TOTAL_PLAYERS);

    Game premiereSaison =
        createPremiereSaisonGame(
            UUID.fromString("880e8400-e29b-41d4-a716-446655440000"),
            marcel,
            thibaut,
            marcel,
            teddy);
    Game savedGame = gameRepository.saveAndFlush(premiereSaison);
    LOGGER.info("Game test enregistre avec id={}", savedGame.getId());

    Team teamThibaut = createTeam("Team Thibaut", thibaut, players.subList(0, TEAM_SLOT_COUNT));
    Team teamTeddy =
        createTeam("Team Teddy", teddy, players.subList(TEAM_TWO_START, TEAM_THREE_START));
    Team teamMarcel =
        createTeam("Team Marcel", marcel, players.subList(TEAM_THREE_START, TEAM_THREE_END));
    createTeam("Equipe Sarah", sarah, List.of());

    attachTeamsToGame(savedGame, List.of(teamThibaut, teamMarcel, teamTeddy));

    LOGGER.info("Donnees de test initialisees avec succes");
    LOGGER.info("- 4 utilisateurs crees");
    LOGGER.info("- {} joueurs Fortnite crees", players.size());
    LOGGER.info("- 3 equipes avec joueurs, 1 equipe vide");
    LOGGER.info("- 1 game active creee avec regles regionales et participants");
  }

  private User createUserAsUser(String username, UUID id) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    user.setEmail(username.toLowerCase(Locale.ROOT) + TEST_EMAIL_SUFFIX);
    user.setPassword(TEST_PASSWORD);
    user.setRole(User.UserRole.USER);
    user.setCurrentSeason(TEST_SEASON);
    return userRepository.save(user);
  }

  private List<Player> generatePlayers(int count) {
    List<Player> players = new ArrayList<>(count);
    for (int i = FIRST_DRAFT_ORDER; i <= count; i++) {
      Player player = new Player();
      player.setNickname("Player" + String.format("%03d", i));
      player.setUsername("player" + String.format("%03d", i));
      player.setFortniteId("FN_" + String.format("%03d", i));
      player.setRegion(ALLOWED_REGIONS.get((i - FIRST_DRAFT_ORDER) % ALLOWED_REGIONS.size()));
      player.setTranche(DEFAULT_TRANCHE);
      player.setCurrentSeason(TEST_SEASON);
      players.add(((PlayerRepositoryPort) playerRepository).save(player));
    }
    return players;
  }

  private Team createTeam(String name, User owner, List<Player> roster) {
    Team team = new Team();
    team.setName(name);
    team.setOwner(owner);
    team.setSeason(TEST_SEASON);

    int position = FIRST_DRAFT_ORDER;
    List<TeamPlayer> teamPlayers = new ArrayList<>();
    for (Player player : roster) {
      TeamPlayer teamPlayer = new TeamPlayer();
      teamPlayer.setTeam(team);
      Player managedPlayer =
          ((PlayerRepositoryPort) playerRepository)
              .findById(player.getId())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Joueur introuvable pour l'initialisation de test: " + player.getId()));
      teamPlayer.setPlayer(managedPlayer);
      teamPlayer.setPosition(position);
      position++;
      teamPlayers.add(teamPlayer);
    }
    team.setPlayers(teamPlayers);
    return ((TeamRepositoryPort) teamRepository).save(team);
  }

  private Game createPremiereSaisonGame(
      UUID gameId,
      User creator,
      User firstParticipant,
      User secondParticipant,
      User thirdParticipant) {
    Game game = new Game();
    game.setId(gameId);
    game.setName("Premiere Saison");
    game.setCreator(creator);
    game.setMaxParticipants(GAME_MAX_PARTICIPANTS);
    game.setStatus(GameStatus.ACTIVE);
    game.setCreatedAt(LocalDateTime.now());
    game.setDescription("Game de test integration");

    for (Player.Region region : ALLOWED_REGIONS) {
      GameRegionRule rule = new GameRegionRule();
      rule.setGame(game);
      rule.setRegion(region);
      rule.setMaxPlayers(REGION_MAX_PLAYERS);
      game.addRegionRule(rule);
    }

    addParticipant(game, firstParticipant, FIRST_DRAFT_ORDER, creator.equals(firstParticipant));
    addParticipant(game, secondParticipant, SECOND_DRAFT_ORDER, creator.equals(secondParticipant));
    addParticipant(game, thirdParticipant, THIRD_DRAFT_ORDER, creator.equals(thirdParticipant));
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
          ((TeamRepositoryPort) teamRepository).save(team);
        });
    teamRepository.flush();
  }
}
