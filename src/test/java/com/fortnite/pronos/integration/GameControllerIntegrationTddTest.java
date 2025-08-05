package com.fortnite.pronos.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

/** Tests d'intégration TDD pour GameController Tests avec vraie base H2 et vraie logique métier */
@SpringBootTest(
    classes = {
      com.fortnite.pronos.PronosApplication.class,
      com.fortnite.pronos.config.TestSecurityConfig.class
    })
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class GameControllerIntegrationTddTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private UserRepository userRepository;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;
  private User testUser;
  private CreateGameRequest validGameRequest;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    objectMapper = new ObjectMapper();

    // Créer un utilisateur de test en base
    testUser = new User();
    testUser.setUsername("ThibautTest");
    testUser.setEmail("thibaut.test@fortnite-pronos.com");
    testUser.setPassword("$2a$10$dummy.password.hash.for.testing");
    testUser.setRole(User.UserRole.PARTICIPANT);
    testUser.setCurrentSeason(2025);
    testUser = userRepository.save(testUser);

    // Créer une requête de game valide
    validGameRequest =
        new CreateGameRequest(
            "Game d'Intégration TDD",
            10,
            "Game créée via test d'intégration TDD",
            false,
            false,
            300,
            43200,
            2025);
  }

  @Test
  @DisplayName("Devrait créer une game avec succès en utilisant l'utilisateur en base")
  void shouldCreateGameSuccessfullyWithUserInDatabase() throws Exception {
    // Given - L'utilisateur existe déjà en base (créé dans setUp)

    // When & Then
    mockMvc
        .perform(
            post("/api/games")
                .param("user", "ThibautTest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validGameRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Game d'Intégration TDD"))
        .andExpect(jsonPath("$.creatorUsername").value("ThibautTest"))
        .andExpect(jsonPath("$.status").value("CREATING"));
  }

  @Test
  @DisplayName("Devrait retourner 401 quand l'utilisateur n'existe pas en base")
  void shouldReturn401WhenUserDoesNotExistInDatabase() throws Exception {
    // Given - Utilisateur inexistant

    // When & Then
    mockMvc
        .perform(
            post("/api/games")
                .param("user", "UtilisateurInexistant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validGameRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Devrait utiliser le fallback par défaut quand aucun paramètre user n'est fourni")
  void shouldUseDefaultFallbackWhenNoUserParamProvided() throws Exception {
    // Given - L'utilisateur Thibaut doit exister en base (créé par data.sql)

    // When & Then
    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validGameRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Game d'Intégration TDD"));
  }

  @Test
  @DisplayName("Devrait retourner 400 pour une requête de création invalide")
  void shouldReturn400ForInvalidCreateRequest() throws Exception {
    // Given
    CreateGameRequest invalidRequest =
        new CreateGameRequest(
            "", // Nom vide
            -1, // Nombre négatif
            "Description invalide",
            false,
            false,
            300,
            43200,
            2025);

    // When & Then
    mockMvc
        .perform(
            post("/api/games")
                .param("user", "ThibautTest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Devrait créer une game avec des règles de région")
  void shouldCreateGameWithRegionRules() throws Exception {
    // Given
    CreateGameRequest requestWithRegions =
        new CreateGameRequest(
            "Game avec Règles Région - Intégration",
            10,
            "Game avec règles de région via intégration",
            false,
            false,
            300,
            43200,
            2025);
    requestWithRegions.addRegionRule(com.fortnite.pronos.model.Player.Region.EU, 3);
    requestWithRegions.addRegionRule(com.fortnite.pronos.model.Player.Region.NAC, 2);

    // When & Then
    mockMvc
        .perform(
            post("/api/games")
                .param("user", "ThibautTest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithRegions)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Game avec Règles Région - Intégration"));
  }

  @Test
  @DisplayName("Devrait vérifier que l'utilisateur de test existe bien en base")
  void shouldVerifyTestUserExistsInDatabase() {
    // Given & When
    User foundUser = userRepository.findByUsernameIgnoreCase("ThibautTest").orElse(null);

    // Then
    assertThat(foundUser).isNotNull();
    assertThat(foundUser.getUsername()).isEqualTo("ThibautTest");
    assertThat(foundUser.getEmail()).isEqualTo("thibaut.test@fortnite-pronos.com");
    assertThat(foundUser.getRole()).isEqualTo(User.UserRole.PARTICIPANT);
  }

  @Test
  @DisplayName("Devrait vérifier que l'utilisateur Thibaut existe en base (data.sql)")
  void shouldVerifyThibautUserExistsInDatabase() {
    // Given & When
    User thibautUser = userRepository.findByUsernameIgnoreCase("Thibaut").orElse(null);

    // Then
    assertThat(thibautUser).isNotNull();
    assertThat(thibautUser.getUsername()).isEqualTo("Thibaut");
    assertThat(thibautUser.getRole()).isEqualTo(User.UserRole.PARTICIPANT);
  }

  @Test
  @DisplayName("Devrait créer plusieurs games pour le même utilisateur")
  void shouldCreateMultipleGamesForSameUser() throws Exception {
    // Given
    CreateGameRequest game1 =
        new CreateGameRequest("Game 1 - Multi", 8, "Première game", false, false, 300, 43200, 2025);

    CreateGameRequest game2 =
        new CreateGameRequest(
            "Game 2 - Multi", 10, "Deuxième game", false, false, 300, 43200, 2025);

    // When & Then - Première game
    mockMvc
        .perform(
            post("/api/games")
                .param("user", "ThibautTest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(game1)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Game 1 - Multi"));

    // When & Then - Deuxième game
    mockMvc
        .perform(
            post("/api/games")
                .param("user", "ThibautTest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(game2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Game 2 - Multi"));
  }
}
