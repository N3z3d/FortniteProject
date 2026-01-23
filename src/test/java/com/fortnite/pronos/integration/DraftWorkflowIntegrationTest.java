package com.fortnite.pronos.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.UserRepository;

/**
 * Tests d'intégration TDD pour les workflows de draft Principe : Red (tests qui échouent) → Green
 * (implémentation) → Refactor
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Tests d'Intégration TDD - Workflows de Draft")
class DraftWorkflowIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private GameRepository gameRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private PlayerRepository playerRepository;

  @Autowired private GameParticipantRepository gameParticipantRepository;

  @Autowired private ObjectMapper objectMapper;

  private MockMvc mockMvc;
  private User testUser;
  private User secondUser;
  private Game testGame;
  private Player testPlayer;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    // Création des utilisateurs de test
    testUser = createTestUser("TestUser", "user@test.com");
    secondUser = createTestUser("SecondUser", "second@test.com");

    // Création d'un joueur de test
    testPlayer = createTestPlayer("TestPlayer", Player.Region.EU);

    // Création d'une game de test
    testGame = createTestGame("Draft Test Game", testUser, 3);
  }

  private User createTestUser(String username, String email) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword("password123");
    user.setRole(User.UserRole.USER);
    user.setCurrentSeason(2025);
    return userRepository.save(user);
  }

  private Player createTestPlayer(String username, Player.Region region) {
    Player player = new Player();
    player.setId(UUID.randomUUID());
    player.setUsername(username);
    player.setNickname(username);
    player.setRegion(region);
    player.setTranche("3");
    player.setCurrentSeason(2025);
    return playerRepository.save(player);
  }

  private Game createTestGame(String name, User creator, int maxParticipants) {
    Game game = new Game();
    game.setId(UUID.randomUUID());
    game.setName(name);
    game.setCreator(creator);
    game.setMaxParticipants(maxParticipants);
    game.setStatus(GameStatus.DRAFTING);
    game.setCreatedAt(LocalDateTime.now());
    return gameRepository.save(game);
  }

  private GameParticipant createTestParticipant(Game game, User user, int draftOrder) {
    GameParticipant participant = new GameParticipant();
    participant.setId(UUID.randomUUID());
    participant.setGame(game);
    participant.setUser(user);
    participant.setDraftOrder(draftOrder);
    participant.setLastSelectionTime(LocalDateTime.now());
    return gameParticipantRepository.save(participant);
  }

  @Test
  @DisplayName("Devrait sélectionner un joueur avec succès")
  void shouldSelectPlayerSuccessfully() throws Exception {
    // Given - Création d'un participant
    GameParticipant participant = createTestParticipant(testGame, testUser, 1);
    UUID gameId = testGame.getId();
    UUID playerId = testPlayer.getId();

    Map<String, Object> request = Map.of("playerId", playerId.toString());

    // When & Then - Sélection du joueur
    mockMvc
        .perform(
            post("/api/draft/{gameId}/select-player", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Joueur selectionne avec succes"));
  }

  @Test
  @DisplayName("Devrait obtenir le prochain participant à jouer")
  void shouldGetNextParticipantToPlay() throws Exception {
    // Given - Création de participants avec ordre de draft
    GameParticipant participant1 = createTestParticipant(testGame, testUser, 1);
    GameParticipant participant2 = createTestParticipant(testGame, secondUser, 2);
    UUID gameId = testGame.getId();

    // When & Then - Récupération du prochain participant
    mockMvc
        .perform(get("/api/draft/{gameId}/next-participant", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(participant1.getId().toString()))
        .andExpect(jsonPath("$.draftOrder").value(1));
  }

  @Test
  @DisplayName("Devrait obtenir l'ordre de draft complet")
  void shouldGetCompleteDraftOrder() throws Exception {
    // Given - Création de plusieurs participants
    GameParticipant participant1 = createTestParticipant(testGame, testUser, 1);
    GameParticipant participant2 = createTestParticipant(testGame, secondUser, 2);
    User thirdUser = createTestUser("ThirdUser", "third@test.com");
    GameParticipant participant3 = createTestParticipant(testGame, thirdUser, 3);
    UUID gameId = testGame.getId();

    // When & Then - Récupération de l'ordre de draft
    mockMvc
        .perform(get("/api/draft/{gameId}/order", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].draftOrder").value(1))
        .andExpect(jsonPath("$[1].draftOrder").value(2))
        .andExpect(jsonPath("$[2].draftOrder").value(3));
  }

  @Test
  @DisplayName("Devrait vérifier si c'est le tour d'un utilisateur")
  void shouldCheckIfItIsUserTurn() throws Exception {
    // Given - Création de participants
    GameParticipant currentParticipant = createTestParticipant(testGame, testUser, 1);
    createTestParticipant(testGame, secondUser, 2);
    UUID gameId = testGame.getId();
    UUID userId = testUser.getId();

    // When & Then - Vérification du tour
    mockMvc
        .perform(get("/api/draft/{gameId}/is-turn/{userId}", gameId, userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isTurn").value(true));
  }

  @Test
  @DisplayName("Devrait passer au participant suivant")
  void shouldMoveToNextParticipant() throws Exception {
    // Given - Création de participants
    createTestParticipant(testGame, testUser, 1);
    createTestParticipant(testGame, secondUser, 2);
    UUID gameId = testGame.getId();

    // When & Then - Passage au participant suivant
    mockMvc
        .perform(post("/api/draft/{gameId}/next", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Passage au participant suivant"));
  }

  @Test
  @DisplayName("Devrait vérifier si le draft est terminé")
  void shouldCheckIfDraftIsComplete() throws Exception {
    // Given - Création d'un draft avec sélections complètes
    GameParticipant participant = createTestParticipant(testGame, testUser, 1);
    // Simulation de sélections complètes
    // (Dans une vraie implémentation, on ajouterait les sélections)

    UUID gameId = testGame.getId();

    // When & Then - Vérification de fin de draft
    mockMvc
        .perform(get("/api/draft/{gameId}/complete", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isComplete").exists());
  }

  @Test
  @DisplayName("Devrait obtenir les joueurs disponibles pour une région")
  void shouldGetAvailablePlayersForRegion() throws Exception {
    // Given - Création de joueurs pour différentes régions
    Player euPlayer1 = createTestPlayer("EUPlayer1", Player.Region.EU);
    Player euPlayer2 = createTestPlayer("EUPlayer2", Player.Region.EU);
    Player nacPlayer = createTestPlayer("NACPlayer", Player.Region.NAC);
    UUID gameId = testGame.getId();

    // When & Then - Récupération des joueurs EU disponibles
    mockMvc
        .perform(get("/api/draft/{gameId}/available-players/EU", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[?(@.region == 'EU')]").exists())
        .andExpect(jsonPath("$[?(@.region == 'NAC')]").doesNotExist());
  }

  @Test
  @DisplayName("Devrait gérer les timeouts")
  void shouldHandleTimeouts() throws Exception {
    // Given - Création d'un participant avec timeout
    GameParticipant timeoutParticipant = createTestParticipant(testGame, testUser, 1);
    // Simulation d'un timeout (dans une vraie implémentation, on modifierait lastSelectionTime)
    UUID gameId = testGame.getId();

    // When & Then - Gestion des timeouts
    mockMvc
        .perform(post("/api/draft/{gameId}/handle-timeouts", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timeoutCount").exists())
        .andExpect(jsonPath("$.message").value("Timeouts geres avec succes"));
  }

  @Test
  @DisplayName("Devrait terminer le draft avec succès")
  void shouldFinishDraftSuccessfully() throws Exception {
    // Given - Création d'un draft complet
    GameParticipant participant1 = createTestParticipant(testGame, testUser, 1);
    GameParticipant participant2 = createTestParticipant(testGame, secondUser, 2);
    // Simulation de sélections complètes
    UUID gameId = testGame.getId();

    // When & Then - Finalisation du draft
    mockMvc
        .perform(post("/api/draft/{gameId}/finish", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Draft termine avec succes"));

    // Vérification en base
    Game updatedGame = gameRepository.findById(gameId).orElseThrow();
    assert updatedGame.getStatus() == GameStatus.ACTIVE;
  }

  /*
   * JIRA-DRAFT-002: Draft /next endpoint logic - FIXED
   *
   * FIX IMPLEMENTED:
   * - DraftService.buildNextParticipantResponse() now uses snake draft position calculation
   * - DraftService.advanceDraftToNextParticipant() now uses snake draft position calculation
   * - DraftService.isUserTurnForGame() now uses snake draft position calculation
   * - The Draft entity already had currentPick/currentRound/totalRounds tracking
   * - calculateCurrentPosition() handles snake draft: odd rounds normal, even rounds reverse
   */
  @Test
  @DisplayName("Devrait gérer le workflow complet de draft")
  void shouldHandleCompleteDraftWorkflow() throws Exception {
    // Given - Création d'un draft avec 3 participants
    User thirdUser = createTestUser("ThirdUser", "third@test.com");
    GameParticipant participant1 = createTestParticipant(testGame, testUser, 1);
    GameParticipant participant2 = createTestParticipant(testGame, secondUser, 2);
    GameParticipant participant3 = createTestParticipant(testGame, thirdUser, 3);
    UUID gameId = testGame.getId();

    // Création de joueurs pour les sélections
    Player player1 = createTestPlayer("Player1", Player.Region.EU);
    Player player2 = createTestPlayer("Player2", Player.Region.NAC);
    Player player3 = createTestPlayer("Player3", Player.Region.EU);

    // When & Then - Étape 1: Vérification du premier participant
    mockMvc
        .perform(get("/api/draft/{gameId}/next-participant", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.draftOrder").value(1));

    // Étape 2: Sélection du premier joueur
    Map<String, Object> selection1 = Map.of("playerId", player1.getId().toString());
    mockMvc
        .perform(
            post("/api/draft/{gameId}/select-player", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(selection1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // Étape 3: Passage au participant suivant
    mockMvc
        .perform(post("/api/draft/{gameId}/next", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // Étape 4: Vérification du deuxième participant
    mockMvc
        .perform(get("/api/draft/{gameId}/next-participant", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.draftOrder").value(2));

    // Étape 5: Sélection du deuxième joueur
    Map<String, Object> selection2 = Map.of("playerId", player2.getId().toString());
    mockMvc
        .perform(
            post("/api/draft/{gameId}/select-player", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(selection2)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // Étape 6: Passage au participant suivant
    mockMvc
        .perform(post("/api/draft/{gameId}/next", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // Étape 7: Sélection du troisième joueur
    Map<String, Object> selection3 = Map.of("playerId", player3.getId().toString());
    mockMvc
        .perform(
            post("/api/draft/{gameId}/select-player", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(selection3)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // Étape 8: Finalisation du draft
    mockMvc
        .perform(post("/api/draft/{gameId}/finish", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // Vérification finale
    Game finalGame = gameRepository.findById(gameId).orElseThrow();
    assert finalGame.getStatus() == GameStatus.ACTIVE;
  }

  @Test
  @DisplayName("Devrait gérer les erreurs de sélection invalide")
  void shouldHandleInvalidSelectionErrors() throws Exception {
    // Given - Tentative de sélection d'un joueur inexistant
    UUID gameId = testGame.getId();
    UUID nonExistentPlayerId = UUID.randomUUID();

    Map<String, Object> invalidRequest = Map.of("playerId", nonExistentPlayerId.toString());

    // When & Then - Sélection invalide
    mockMvc
        .perform(
            post("/api/draft/{gameId}/select-player", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Devrait gérer les erreurs de game inexistante")
  void shouldHandleNonExistentGameErrors() throws Exception {
    // Given - Tentative d'accès à une game inexistante
    UUID nonExistentGameId = UUID.randomUUID();

    // When & Then - Tentatives d'accès
    mockMvc
        .perform(get("/api/draft/{gameId}/next-participant", nonExistentGameId))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/draft/{gameId}/order", nonExistentGameId))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(post("/api/draft/{gameId}/finish", nonExistentGameId))
        .andExpect(status().isNotFound());
  }
}
