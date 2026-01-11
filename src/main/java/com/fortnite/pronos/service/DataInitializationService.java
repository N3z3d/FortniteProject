package com.fortnite.pronos.service;

import java.util.*;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.config.SeedProperties;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;
import com.fortnite.pronos.service.seed.SeedDataProviderSelectorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service d'initialisation des données de test */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService {

  private static final String SEED_RESET_PROPERTY = "fortnite.seed.reset";

  private final UserRepository userRepository;
  private final PlayerRepository playerRepository;
  private final TeamRepository teamRepository;
  private final ScoreRepository scoreRepository;
  private final GameRepository gameRepository;
  private final Environment environment;
  private final CsvDataLoaderService csvDataLoaderService;
  private final SeedDataProviderSelectorService seedDataProviderSelector;
  private final PasswordEncoder passwordEncoder;
  private final SeedProperties seedProperties;

  /** Initialise les données de test au démarrage de l'application */
  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void initializeTestData() {
    if (!seedProperties.isEnabled()) {
      log.info("Seed disabled (fortnite.seed.enabled=false)");
      return;
    }
    if (!seedProperties.isLegacyEnabled()) {
      log.info("Skipping legacy data initialization (fortnite.seed.legacy-enabled=false)");
      return;
    }
    // Vérifier si on est en mode dev
    if (!isDevProfile()) {
      log.info("Skipping test data initialization - not in dev mode");
      return;
    }

    // Keep existing data unless a reset is requested.
    if (hasExistingData()) {
      if (!isSeedResetEnabled()) {
        log.info(
            "Seed skipped: existing data detected. Set {}=true to re-seed.", SEED_RESET_PROPERTY);
        return;
      }

      log.info("Seed reset enabled. Clearing existing data before CSV re-seed.");
      teamRepository.deleteAll();
      scoreRepository.deleteAll();
      gameRepository.deleteAll();
      playerRepository.deleteAll();
      userRepository.deleteAll();
      log.info("Database cleared before seed.");
    }

    try {
      // VALIDATION CRITIQUE : Créer et valider les utilisateurs avant sauvegarde
      List<User> usersToCreate = createUsers();
      List<User> savedUsers = new ArrayList<>();

      if (!usersToCreate.isEmpty()) {
        // Validation préalable pour éviter les erreurs de contraintes
        for (User user : usersToCreate) {
          validateUser(user);
        }
        savedUsers = userRepository.saveAll(usersToCreate);
        log.info("✅ {} nouveaux utilisateurs créés et validés", savedUsers.size());
      }

      // Récupérer TOUS les utilisateurs existants (nouveaux + éventuels existants)
      List<User> allUsers = userRepository.findAll();
      log.info("📊 Total utilisateurs en base: {}", allUsers.size());

      // OPTION 1: Charger les données réelles du CSV avec CsvDataLoaderService
      // OPTION 2: Charger les données mock avec MockDataGeneratorService (nouveau)
      log.info("🎮 Chargement des données depuis le CSV...");

      // Utiliser le nouveau service de mock data
      MockDataGeneratorService.MockDataSet mockData = seedDataProviderSelector.loadSeedData();

      if (mockData.total() == 0) {
        log.warn("⚠️ Aucune donnée mock chargée, fallback vers CsvDataLoaderService");
        csvDataLoaderService.loadAllCsvData();
      } else {
        // Sauvegarder les players et scores depuis le mock
        List<Player> mockPlayers = mockData.getAllPlayers();
        List<Player> savedMockPlayers = playerRepository.saveAll(mockPlayers);

        // Lier les scores aux players sauvegardés
        List<Score> mockScores = mockData.getAllScores();
        for (int i = 0; i < savedMockPlayers.size() && i < mockScores.size(); i++) {
          mockScores.get(i).setPlayer(savedMockPlayers.get(i));
        }
        scoreRepository.saveAll(mockScores);

        log.info("✅ {} joueurs mock chargés depuis le CSV", savedMockPlayers.size());
      }

      List<Player> savedPlayers = playerRepository.findAll();
      log.info(
          "✅ {} joueurs réels chargés depuis le CSV avec leurs scores complets",
          savedPlayers.size());

      // Créer des équipes pour les utilisateurs avec les vrais joueurs selon les assignations CSV
      log.info("🏆 Création d'équipes avec les joueurs réels selon les assignations CSV...");
      List<Team> teams = createTeamsFromCsvAssignments(allUsers);
      List<Team> savedTeams = teamRepository.saveAll(teams);
      log.info("✅ {} équipes créées avec des joueurs réels selon le CSV", savedTeams.size());

      // Créer des games de test pour les utilisateurs avec équipes réelles
      log.info("🎮 Création de games de test avec équipes réelles...");
      createTestGamesWithRealTeams(allUsers, savedTeams);

      // Les scores sont déjà chargés par le CSV - pas besoin de créer des scores de test
      long scoreCount = scoreRepository.count();
      log.info("📊 {} scores réels chargés depuis le CSV", scoreCount);

      log.info("✅ Données réelles initialisées avec succès depuis le CSV");
      log.info("   👥 Utilisateurs: {}", allUsers.size());
      log.info("   🎮 Joueurs réels: {}", savedPlayers.size());
      log.info("   🏆 Équipes: {}", savedTeams.size());
      log.info("   📊 Scores réels: {}", scoreCount);

    } catch (Exception e) {
      log.error("❌ Erreur lors de l'initialisation des données de test", e);
      log.error("❌ Message d'erreur: {}", e.getMessage());
      // Fallback : Créer des données minimales pour permettre le démarrage
      try {
        createMinimalTestData();
      } catch (Exception fallbackError) {
        log.error("❌ Erreur critique lors du fallback", fallbackError);
      }
    }
  }

  /** Méthode de validation robuste pour les utilisateurs */
  private void validateUser(User user) {
    if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
      throw new IllegalArgumentException("Username cannot be empty for user: " + user.getEmail());
    }
    if (user.getEmail() == null || !user.getEmail().contains("@")) {
      throw new IllegalArgumentException("Invalid email for user: " + user.getUsername());
    }
    if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
      throw new IllegalArgumentException(
          "Password cannot be empty for user: " + user.getUsername());
    }
  }

  /** Création de données minimales en cas d'erreur dans l'initialisation principale */
  private void createMinimalTestData() {
    log.info("🔄 Initialisation des données minimales de fallback...");

    // Créer seulement les utilisateurs essentiels
    if (userRepository.count() == 0) {
      User admin = createUser("admin", "admin@test.com", User.UserRole.ADMIN);
      User testUser = createUser("testuser", "test@test.com", User.UserRole.USER);

      validateUser(admin);
      validateUser(testUser);

      userRepository.saveAll(Arrays.asList(admin, testUser));
      log.info("✅ Données minimales créées avec succès (2 utilisateurs)");
    }
  }

  private boolean isDevProfile() {
    return Arrays.asList(environment.getActiveProfiles()).contains("dev");
  }

  private boolean isSeedResetEnabled() {
    return seedProperties.isReset();
  }

  private boolean hasExistingData() {
    return userRepository.count() > 0 || playerRepository.count() > 0;
  }

  private List<User> createUsers() {
    List<User> users = new ArrayList<>();

    // Créer les utilisateurs seulement s'ils n'existent pas déjà
    if (!userRepository.existsByUsername("admin")) {
      users.add(createUser("admin", "admin@fortnite-pronos.com", User.UserRole.ADMIN));
    }
    if (!userRepository.existsByUsername("Thibaut")) {
      users.add(createUser("Thibaut", "thibaut@test.com", User.UserRole.USER));
    }
    if (!userRepository.existsByUsername("Teddy")) {
      users.add(createUser("Teddy", "teddy@test.com", User.UserRole.USER));
    }
    if (!userRepository.existsByUsername("Marcel")) {
      users.add(createUser("Marcel", "marcel@test.com", User.UserRole.USER));
    }

    return users;
  }

  private User createUser(String username, String email, User.UserRole role) {
    User user = new User();
    // Ne pas définir l'ID manuellement - laisser JPA le générer automatiquement
    // user.setId(UUID.randomUUID()); // Commenté pour éviter les conflits d'UUID
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode("password")); // Mot de passe par défaut "password"
    user.setRole(role);
    user.setCurrentSeason(2025);
    return user;
  }

  private List<Player> createPlayers() {
    return Arrays.asList(
        createPlayer("Peterbot", "NAC", "1", 143509),
        createPlayer("ふーくん", "ASIA", "1", 101818),
        createPlayer("Oatley", "OCE", "1", 97364),
        createPlayer("FKS", "ME", "1", 88445),
        createPlayer("MariusCOW", "EU", "2", 75632),
        createPlayer("PXMP", "NAC", "2", 68921),
        createPlayer("Eomzo", "ASIA", "2", 62345),
        createPlayer("Koyota", "OCE", "2", 58734),
        createPlayer("Wreckless", "EU", "3", 52345),
        createPlayer("KING", "NAC", "3", 48765),
        createPlayer("Parz", "ASIA", "3", 45678),
        createPlayer("Kchorro", "EU", "3", 42345));
  }

  private Player createPlayer(String nickname, String region, String tranche, int points) {
    Player player = new Player();
    // Ne pas définir l'ID manuellement - laisser JPA le générer automatiquement
    // player.setId(UUID.randomUUID()); // Commenté pour éviter les conflits d'UUID

    // Créer un username valide à partir du nickname avec validation @NotBlank
    String cleanUsername = generateValidUsername(nickname);

    // Validation finale pour s'assurer que le username n'est pas vide (contrainte @NotBlank)
    if (cleanUsername == null || cleanUsername.trim().isEmpty()) {
      cleanUsername = "player" + Math.abs(nickname.hashCode());
    }

    player.setUsername(cleanUsername);
    player.setNickname(nickname != null && !nickname.trim().isEmpty() ? nickname : cleanUsername);
    player.setRegion(Player.Region.valueOf(region));
    player.setTranche(tranche != null && !tranche.trim().isEmpty() ? tranche : "1");
    player.setCurrentSeason(2025);
    return player;
  }

  /** Génère un username valide respectant les contraintes @NotBlank */
  private String generateValidUsername(String nickname) {
    if (nickname == null || nickname.trim().isEmpty()) {
      return "player" + System.currentTimeMillis();
    }

    // Nettoyer le nickname pour créer un username valide
    String cleaned = nickname.toLowerCase().replaceAll("[^a-z0-9]", "").trim();

    // Si après nettoyage il ne reste rien, générer un username basé sur le hash
    if (cleaned.isEmpty()) {
      return "player" + Math.abs(nickname.hashCode());
    }

    // S'assurer que le username fait au moins 3 caractères
    if (cleaned.length() < 3) {
      cleaned = cleaned + "usr" + (System.currentTimeMillis() % 1000);
    }

    // VALIDATION FINALE CRITIQUE : Garantir que le username n'est jamais vide
    if (cleaned == null || cleaned.trim().isEmpty()) {
      cleaned = "player" + System.currentTimeMillis();
    }

    return cleaned;
  }

  /** Crée des équipes à partir des assignations CSV réelles */
  private List<Team> createTeamsFromCsvAssignments(List<User> users) {
    log.info("🏗️ Création d'équipes avec les assignations CSV réelles");

    // Charger les données mock pour obtenir les assignations
    MockDataGeneratorService.MockDataSet mockData = seedDataProviderSelector.loadSeedData();

    if (mockData.total() == 0) {
      log.warn("⚠️ Aucune donnée mock disponible, utilisation de la méthode de fallback");
      return createFallbackTeams(users);
    }

    List<Team> teams = new ArrayList<>();
    List<String> pronosticators = mockData.getPronosticators();

    // Créer une équipe pour chaque pronostiqueur du CSV
    for (String pronostiqueur : pronosticators) {
      List<MockDataGeneratorService.PlayerWithScore> playerDataList =
          mockData.getPlayersFor(pronostiqueur);

      if (playerDataList.isEmpty()) {
        log.warn("⚠️ Aucun joueur assigné pour {}, équipe ignorée", pronostiqueur);
        continue;
      }

      // Chercher l'utilisateur correspondant au pronostiqueur
      User owner =
          users.stream()
              .filter(u -> u.getUsername().equalsIgnoreCase(pronostiqueur))
              .findFirst()
              .orElse(null);

      if (owner == null) {
        log.warn(
            "⚠️ Utilisateur non trouvé pour le pronostiqueur '{}', équipe ignorée", pronostiqueur);
        continue;
      }

      // Extraire les players et les récupérer depuis la base de données (entités attachées)
      List<Player> assignedPlayers = new ArrayList<>();
      for (MockDataGeneratorService.PlayerWithScore playerData : playerDataList) {
        String username = playerData.player().getUsername();
        // Utiliser findAll + filter pour gérer les éventuels doublons
        List<Player> matchingPlayers =
            playerRepository.findAll().stream()
                .filter(p -> p.getUsername().equals(username))
                .limit(1)
                .toList();
        if (!matchingPlayers.isEmpty()) {
          assignedPlayers.add(matchingPlayers.get(0));
        }
      }

      if (assignedPlayers.isEmpty()) {
        log.warn("⚠️ Aucun joueur trouvé en base pour {}, équipe ignorée", pronostiqueur);
        continue;
      }

      // Créer l'équipe avec le nom du pronostiqueur
      String teamName = "Équipe " + pronostiqueur;
      Team team = createTeam(teamName, owner, assignedPlayers);
      teams.add(team);

      log.info(
          "✅ {} créée avec {} joueurs assignés depuis le CSV", teamName, assignedPlayers.size());
    }

    log.info("🏆 {} équipes créées depuis les assignations CSV", teams.size());
    return teams;
  }

  /** Méthode de fallback si les assignations CSV ne sont pas disponibles */
  private List<Team> createFallbackTeams(List<User> users) {
    log.info("🔄 Utilisation de la méthode de fallback pour créer des équipes");

    List<Player> allPlayers = playerRepository.findAll();
    if (allPlayers.size() < 3) {
      log.warn("Pas assez de joueurs pour créer des équipes de fallback");
      return new ArrayList<>();
    }

    // Récupérer les utilisateurs participants (pas admin)
    List<User> participants =
        users.stream().filter(u -> u.getRole() == User.UserRole.USER).toList();

    if (participants.size() < 3) {
      log.warn("Pas assez d'utilisateurs participants pour créer des équipes");
      return new ArrayList<>();
    }

    List<Team> teams = new ArrayList<>();

    // Créer une équipe pour chaque participant avec des joueurs répartis équitablement
    int playersPerTeam = Math.max(1, allPlayers.size() / participants.size());

    for (int i = 0; i < Math.min(participants.size(), 3); i++) {
      User owner = participants.get(i);
      String teamName = "Équipe " + owner.getUsername();

      // Assigner des joueurs à chaque équipe
      int startIndex = i * playersPerTeam;
      int endIndex = Math.min(startIndex + playersPerTeam, allPlayers.size());
      List<Player> teamPlayers = allPlayers.subList(startIndex, endIndex);

      if (!teamPlayers.isEmpty()) {
        Team team = createTeam(teamName, owner, teamPlayers);
        teams.add(team);
        log.info("✅ {} créée avec {} joueurs (fallback)", teamName, teamPlayers.size());
      }
    }

    return teams;
  }

  /**
   * Retourne le nom d'équipe selon l'index Clean Code : Méthode utilitaire pour éviter la
   * duplication
   */
  private String getTeamName(int teamIndex) {
    return switch (teamIndex) {
      case 0 -> "Équipe Thibaut";
      case 1 -> "Équipe Teddy";
      case 2 -> "Équipe Marcel";
      default -> "Équipe " + (teamIndex + 1);
    };
  }

  private Team createTeam(String name, User owner, List<Player> players) {
    Team team = new Team();
    // Ne pas définir l'ID manuellement - laisser JPA le générer automatiquement
    // team.setId(UUID.randomUUID()); // Commenté pour éviter les conflits d'UUID
    team.setOwner(owner);
    team.setName(name);
    team.setSeason(2025);

    // Ajouter les joueurs à l'équipe
    for (int i = 0; i < players.size(); i++) {
      Player player = players.get(i);
      // S'assurer que le player est attaché au contexte de persistance
      if (player.getId() != null) {
        player = playerRepository.findById(player.getId()).orElse(player);
      }
      team.addPlayer(player, i + 1);
    }

    return team;
  }

  private List<Score> createScores(List<Player> players) {
    return players.stream().map(player -> createScore(player, "FNCS_2025_WEEK1", 2025)).toList();
  }

  private Score createScore(Player player, String tournamentId, int season) {
    Score score = new Score();
    score.setPlayer(player);
    score.setSeason(season);
    score.setPoints(100 + (int) (Math.random() * 900)); // Score aléatoire entre 100 et 1000
    score.setDate(java.time.LocalDate.now());
    score.setTimestamp(java.time.OffsetDateTime.now());
    return score;
  }

  /** Crée des games de test avec les équipes réelles créées depuis le CSV */
  private void createTestGamesWithRealTeams(List<User> users, List<Team> realTeams) {
    List<User> participants =
        users.stream().filter(u -> u.getRole() == User.UserRole.USER).toList();

    if (participants.size() < 3 || realTeams.size() < 3) {
      log.warn(
          "Pas assez d'utilisateurs participants ou d'équipes réelles pour créer des games complètes");
      return;
    }

    try {
      User thibaut =
          participants.stream()
              .filter(u -> "Thibaut".equals(u.getUsername()))
              .findFirst()
              .orElse(participants.get(0));

      User teddy =
          participants.stream()
              .filter(u -> "Teddy".equals(u.getUsername()))
              .findFirst()
              .orElse(participants.get(1));

      User marcel =
          participants.stream()
              .filter(u -> "Marcel".equals(u.getUsername()))
              .findFirst()
              .orElse(participants.get(2));

      // Game principale active avec les 3 équipes réelles
      Game gameActive =
          createGame(
              "Fantasy League 2025 - Championnat Principal",
              "Game principale avec les équipes réelles de Thibaut, Teddy et Marcel basées sur les données CSV",
              thibaut,
              3,
              GameStatus.ACTIVE);

      // Ajouter tous les participants avec leurs équipes réelles
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
   * Méthode de fallback pour créer des games de test classiques Clean Code : Méthode focalisée sur
   * la création de games de démonstration complètes
   */
  private void createTestGames(List<User> users) {
    List<User> participants =
        users.stream().filter(u -> u.getRole() == User.UserRole.USER).toList();

    if (participants.size() < 3) {
      log.warn("Pas assez d'utilisateurs participants trouvés pour créer des games complètes");
      return;
    }

    try {
      User thibaut =
          participants.stream()
              .filter(u -> "Thibaut".equals(u.getUsername()))
              .findFirst()
              .orElse(participants.get(0));

      User teddy =
          participants.stream()
              .filter(u -> "Teddy".equals(u.getUsername()))
              .findFirst()
              .orElse(participants.get(1));

      User marcel =
          participants.stream()
              .filter(u -> "Marcel".equals(u.getUsername()))
              .findFirst()
              .orElse(participants.get(2));

      // Game active principale avec tous les participants
      Game gameActive =
          createGame(
              "Fantasy League Pro 2025",
              "Championnat principal avec Thibaut, Teddy et Marcel - Saison 2025",
              thibaut,
              3,
              GameStatus.ACTIVE);

      // Ajouter tous les participants
      gameActive.addParticipant(createGameParticipant(gameActive, thibaut, 1));
      gameActive.addParticipant(createGameParticipant(gameActive, teddy, 2));
      gameActive.addParticipant(createGameParticipant(gameActive, marcel, 3));

      gameRepository.save(gameActive);
      log.info("✅ Game principale '{}' créée avec 3 participants", gameActive.getName());

      // Game de draft en cours pour Teddy
      Game gameDraft =
          createGame(
              "Draft League - Teddy",
              "Nouvelle game en phase de draft organisée par Teddy",
              teddy,
              3,
              GameStatus.DRAFTING);

      // Ajouter les participants au draft
      gameDraft.addParticipant(createGameParticipant(gameDraft, teddy, 1));
      gameDraft.addParticipant(createGameParticipant(gameDraft, thibaut, 2));
      gameDraft.addParticipant(createGameParticipant(gameDraft, marcel, 3));

      gameRepository.save(gameDraft);
      log.info("✅ Game draft '{}' créée avec 3 participants", gameDraft.getName());

      // Game en création pour Marcel
      Game gameCreating =
          createGame(
              "Championship 2025 - Marcel",
              "Nouveau championnat en préparation par Marcel",
              marcel,
              4,
              GameStatus.CREATING);

      // Ajouter Marcel comme seul participant pour l'instant
      gameCreating.addParticipant(createGameParticipant(gameCreating, marcel, 1));

      gameRepository.save(gameCreating);
      log.info("✅ Game création '{}' créée avec 1 participant", gameCreating.getName());

      long totalGames = gameRepository.count();
      log.info("🎮 {} games de test créées au total", totalGames);

    } catch (Exception e) {
      log.error("❌ Erreur lors de la création des games de test", e);
    }
  }

  /**
   * Crée un participant de game Clean Code TDD : Méthode utilitaire pour créer un participant avec
   * liste initialisée
   */
  private GameParticipant createGameParticipant(Game game, User user, int draftOrder) {
    return GameParticipant.builder()
        .game(game)
        .user(user)
        .draftOrder(draftOrder)
        .selectedPlayers(new ArrayList<>()) // TDD: Initialiser explicitement la liste
        .build();
  }

  /**
   * Crée une game avec les paramètres spécifiés Clean Code TDD : Méthode utilitaire pour la
   * création d'une game avec initialisation des listes
   */
  private Game createGame(
      String name, String description, User creator, int maxParticipants, GameStatus status) {
    Game game =
        Game.builder()
            .name(name)
            .description(description)
            .creator(creator)
            .maxParticipants(maxParticipants)
            .status(status)
            .participants(new ArrayList<>()) // TDD: Initialiser explicitement les listes
            .regionRules(new ArrayList<>()) // TDD: Initialiser explicitement les listes
            .build();

    // Ajouter des règles régionales basiques
    addBasicRegionRules(game);

    return game;
  }

  /**
   * Ajoute des règles régionales basiques à une game Clean Code : Méthode utilitaire pour les
   * règles par défaut
   */
  private void addBasicRegionRules(Game game) {
    // Règles équilibrées entre les régions
    game.addRegionRule(
        GameRegionRule.builder().game(game).region(Player.Region.EU).maxPlayers(2).build());

    game.addRegionRule(
        GameRegionRule.builder().game(game).region(Player.Region.NAC).maxPlayers(2).build());

    game.addRegionRule(
        GameRegionRule.builder().game(game).region(Player.Region.ASIA).maxPlayers(2).build());
  }
}
