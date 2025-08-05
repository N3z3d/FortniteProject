package com.fortnite.pronos.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;

/**
 * Tests d'Intégration TDD pour les workflows de Games Principe : Red (tests qui échouent) → Green
 * (implémentation) → Refactor
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureWebMvc
@DisplayName("Tests d'Intégration TDD - Workflows de Games")
class GameWorkflowIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private GameRepository gameRepository;

  @Autowired private UserRepository userRepository;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  private User testUser;
  private Game testGame;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    objectMapper = new ObjectMapper();

    // Création d'un utilisateur de test
    testUser = createTestUser("TestUser", "user@test.com");

    // Création d'une game de test
    testGame = createTestGame("Test Game", testUser);
  }

  @Test
  @DisplayName("Devrait créer une game et la récupérer avec succès")
  void shouldCreateGameAndRetrieveItSuccessfully() throws Exception {
    // Given
    CreateGameRequest request = new CreateGameRequest();
    request.setName("New Integration Game");
    request.setMaxParticipants(8);
    request.setRegionRules(new HashMap<>());
    request.getRegionRules().put(Player.Region.EU, 5);
    request.getRegionRules().put(Player.Region.NAC, 3);
    request.setCreatorId(testUser.getId());

    // When & Then
    String response =
        mockMvc
            .perform(
                post("/api/games")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("New Integration Game"))
            .andExpect(jsonPath("$.status").value("CREATING"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Vérifier que la game est bien en base
    assertTrue(gameRepository.findAll().size() > 0);
  }

  @Test
  @DisplayName("Devrait permettre de rejoindre une game existante")
  void shouldAllowJoiningExistingGame() throws Exception {
    // Given
    User secondUser = createTestUser("SecondUser", "second@test.com");
    JoinGameRequest joinRequest = new JoinGameRequest();
    joinRequest.setGameId(testGame.getId());
    joinRequest.setUserId(secondUser.getId());

    // When & Then
    mockMvc
        .perform(
            post("/api/games/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequest)))
        .andExpect(status().isOk());

    // Vérifier que le participant a été ajouté
    Game updatedGame = gameRepository.findById(testGame.getId()).orElse(null);
    assertNotNull(updatedGame);
    assertEquals(2, updatedGame.getParticipants().size());
  }

  @Test
  @DisplayName("Devrait démarrer le draft d'une game avec suffisamment de participants")
  void shouldStartDraftWithEnoughParticipants() throws Exception {
    // Given
    User secondUser = createTestUser("SecondUser", "second@test.com");
    User thirdUser = createTestUser("ThirdUser", "third@test.com");

    // Ajouter des participants
    joinGame(testGame.getId(), secondUser.getId());
    joinGame(testGame.getId(), thirdUser.getId());

    // When & Then
    mockMvc
        .perform(post("/api/games/{id}/draft/start", testGame.getId()))
        .andExpect(status().isOk());

    // Vérifier que le statut a changé
    Game updatedGame = gameRepository.findById(testGame.getId()).orElse(null);
    assertNotNull(updatedGame);
    assertEquals(GameStatus.DRAFTING, updatedGame.getStatus());
  }

  @Test
  @DisplayName("Devrait récupérer toutes les games disponibles")
  void shouldRetrieveAllAvailableGames() throws Exception {
    // When & Then
    mockMvc
        .perform(get("/api/games/available"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].name").value("Test Game"));
  }

  @Test
  @DisplayName("Devrait récupérer les games d'un utilisateur spécifique")
  void shouldRetrieveGamesForSpecificUser() throws Exception {
    // When & Then
    mockMvc
        .perform(get("/api/games/user/{userId}", testUser.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].name").value("Test Game"));
  }

  @Test
  @DisplayName("Devrait supprimer une game avec succès")
  void shouldDeleteGameSuccessfully() throws Exception {
    // When & Then
    mockMvc.perform(delete("/api/games/{id}", testGame.getId())).andExpect(status().isOk());

    // Vérifier que la game a été supprimée
    assertFalse(gameRepository.findById(testGame.getId()).isPresent());
  }

  @Test
  @DisplayName("Devrait retourner 404 pour une game inexistante")
  void shouldReturn404ForNonExistentGame() throws Exception {
    // Given
    UUID nonExistentGameId = UUID.randomUUID();

    // When & Then
    mockMvc.perform(get("/api/games/{id}", nonExistentGameId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Devrait gérer les erreurs de validation lors de la création")
  void shouldHandleValidationErrorsOnCreation() throws Exception {
    // Given
    CreateGameRequest invalidRequest = new CreateGameRequest();
    invalidRequest.setName(""); // Nom vide invalide
    invalidRequest.setMaxParticipants(10);

    // When & Then
    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Devrait empêcher de rejoindre une game pleine")
  void shouldPreventJoiningFullGame() throws Exception {
    // Given - Créer une game avec seulement 2 participants max
    Game fullGame = createTestGame("Full Game", testUser);
    fullGame.setMaxParticipants(2);
    gameRepository.save(fullGame);

    // Ajouter un premier participant
    User secondUser = createTestUser("SecondUser", "second@test.com");
    joinGame(fullGame.getId(), secondUser.getId());

    // Essayer d'ajouter un troisième participant
    User thirdUser = createTestUser("ThirdUser", "third@test.com");
    JoinGameRequest joinRequest = new JoinGameRequest();
    joinRequest.setGameId(fullGame.getId());
    joinRequest.setUserId(thirdUser.getId());

    // When & Then
    mockMvc
        .perform(
            post("/api/games/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Devrait empêcher de démarrer le draft avec trop peu de participants")
  void shouldPreventStartingDraftWithTooFewParticipants() throws Exception {
    // Given - Game avec seulement 1 participant (le créateur)

    // When & Then
    mockMvc
        .perform(post("/api/games/{id}/draft/start", testGame.getId()))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Devrait maintenir la cohérence des données lors des opérations multiples")
  void shouldMaintainDataConsistencyDuringMultipleOperations() throws Exception {
    // Given
    User secondUser = createTestUser("SecondUser", "second@test.com");
    User thirdUser = createTestUser("ThirdUser", "third@test.com");

    // When - Opérations multiples
    joinGame(testGame.getId(), secondUser.getId());
    joinGame(testGame.getId(), thirdUser.getId());

    // Then - Vérifier la cohérence
    Game updatedGame = gameRepository.findById(testGame.getId()).orElse(null);
    assertNotNull(updatedGame);
    assertEquals(3, updatedGame.getParticipants().size());
    assertTrue(
        updatedGame.getParticipants().stream()
            .anyMatch(p -> p.getUser().getId().equals(secondUser.getId())));
    assertTrue(
        updatedGame.getParticipants().stream()
            .anyMatch(p -> p.getUser().getId().equals(thirdUser.getId())));
  }

  @Test
  @DisplayName("Devrait gérer les erreurs de base de données gracieusement")
  void shouldHandleDatabaseErrorsGracefully() throws Exception {
    // Given - Requête avec des données invalides qui causeraient une erreur DB
    CreateGameRequest request = new CreateGameRequest();
    request.setName("Test Game");
    request.setMaxParticipants(null); // Null causera une erreur de validation

    // When & Then
    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // Méthodes utilitaires
  private User createTestUser(String username, String email) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setRole(User.UserRole.PARTICIPANT);
    user.setCurrentSeason(2025);
    return userRepository.save(user);
  }

  private Game createTestGame(String name, User creator) {
    Game game = new Game();
    game.setName(name);
    game.setCreator(creator);
    game.setMaxParticipants(10);
    game.setStatus(GameStatus.CREATING);
    return gameRepository.save(game);
  }

  private void joinGame(UUID gameId, UUID userId) throws Exception {
    JoinGameRequest joinRequest = new JoinGameRequest();
    joinRequest.setGameId(gameId);
    joinRequest.setUserId(userId);

    mockMvc
        .perform(
            post("/api/games/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequest)))
        .andExpect(status().isOk());
  }
}
