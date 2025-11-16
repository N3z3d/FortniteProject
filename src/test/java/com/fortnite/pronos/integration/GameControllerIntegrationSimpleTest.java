package com.fortnite.pronos.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;

/** Tests d'intégration TDD simplifiés pour GameController */
@SpringBootTest(
    classes = {
      com.fortnite.pronos.PronosApplication.class,
      com.fortnite.pronos.config.TestSecurityConfigTestBackup.class
    })
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("GameController Integration - Simple Tests")
class GameControllerIntegrationSimpleTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private GameRepository gameRepository;

  @Autowired private UserRepository userRepository;

  private User testUser;
  private Game testGame;

  @BeforeEach
  void setUp() {
    // Nettoyer les données
    gameRepository.deleteAll();
    userRepository.deleteAll();

    // Créer un utilisateur de test
    testUser = new User();
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword("password");
    testUser.setRole(User.UserRole.PARTICIPANT);
    testUser = userRepository.save(testUser);

    // Créer une game de test
    testGame = new Game();
    testGame.setName("Game Test");
    testGame.setDescription("Game de test");
    testGame.setCreator(testUser);
    testGame.setMaxParticipants(4);
    testGame.setStatus(GameStatus.CREATING);
    testGame.setParticipants(new ArrayList<>());
    testGame.setRegionRules(new ArrayList<>());
    testGame = gameRepository.save(testGame);
  }

  @Test
  @DisplayName("GET /api/games doit retourner toutes les games")
  void shouldReturnAllGames() throws Exception {
    mockMvc
        .perform(get("/api/games"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Game Test"))
        .andExpect(jsonPath("$[0].status").value("CREATING"))
        .andExpect(jsonPath("$[0].maxParticipants").value(4));
  }

  @Test
  @DisplayName("GET /api/games doit retourner une liste vide quand aucune game n'existe")
  void shouldReturnEmptyListWhenNoGames() throws Exception {
    // Supprimer la game de test
    gameRepository.delete(testGame);

    mockMvc
        .perform(get("/api/games"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("GET /api/games/{id} doit retourner une game spécifique")
  void shouldReturnSpecificGame() throws Exception {
    mockMvc
        .perform(get("/api/games/" + testGame.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.name").value("Game Test"))
        .andExpect(jsonPath("$.id").value(testGame.getId().toString()));
  }

  @Test
  @DisplayName("GET /api/games/{id} doit retourner 404 pour une game inexistante")
  void shouldReturn404ForNonExistentGame() throws Exception {
    UUID nonExistentId = UUID.randomUUID();

    mockMvc.perform(get("/api/games/" + nonExistentId)).andExpect(status().isNotFound());
  }
}
