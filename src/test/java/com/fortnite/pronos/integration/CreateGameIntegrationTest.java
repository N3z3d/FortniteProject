package com.fortnite.pronos.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;
import com.fortnite.pronos.service.JwtService;

/**
 * Test d'intégration pour la création de games Ce test vérifie le workflow complet de création de
 * game
 */
@SpringBootTest(
    classes = {
      com.fortnite.pronos.PronosApplication.class,
      com.fortnite.pronos.config.TestSecurityConfig.class
    })
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class CreateGameIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(CreateGameIntegrationTest.class);

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private UserRepository userRepository;

  @Autowired private JwtService jwtService;

  @Autowired private ObjectMapper objectMapper;

  private MockMvc mockMvc;
  private User testUser;
  private String jwtToken;
  private static final String TEST_USERNAME = "testuser";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    // Créer un utilisateur de test
    testUser = new User();
    testUser.setUsername(TEST_USERNAME);
    testUser.setEmail("test@example.com");
    testUser.setPassword("password123");
    testUser.setRole(User.UserRole.ADMIN);
    testUser.setCurrentSeason(2025);
    testUser = ((UserRepositoryPort) userRepository).save(testUser);

    // Créer UserDetails pour le JWT
    org.springframework.security.core.userdetails.User userDetails =
        new org.springframework.security.core.userdetails.User(
            testUser.getUsername(), testUser.getPassword(), java.util.Collections.emptyList());

    // Générer un token JWT pour l'utilisateur
    jwtToken = jwtService.generateToken(userDetails);

    log.info("Test setup completed - User: {}, Token generated", testUser.getUsername());
  }

  @Test
  void shouldCreateGameSuccessfully() throws Exception {
    // Créer une requête de création de game
    CreateGameRequest request =
        CreateGameRequest.builder()
            .name("Test Game")
            .maxParticipants(5)
            .description("Test game description")
            .isPrivate(false)
            .autoStartDraft(true)
            .draftTimeLimit(300)
            .autoPickDelay(43200)
            .currentSeason(2025)
            .regionRules(Collections.emptyMap())
            .build();

    // Envoyer la requête de création
    mockMvc
        .perform(
            post("/api/games")
                .header("X-Test-User", TEST_USERNAME)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.name").value("Test Game"))
        .andExpect(jsonPath("$.maxParticipants").value(5))
        .andExpect(jsonPath("$.creatorName").value("testuser"))
        .andExpect(jsonPath("$.status").value("CREATING"));

    log.info("[OK] Game creation test passed successfully");
  }

  @Test
  void shouldRejectInvalidGameCreation() throws Exception {
    // Créer une requête invalide (nom vide)
    CreateGameRequest request =
        CreateGameRequest.builder()
            .name("") // Nom vide - invalide
            .maxParticipants(5)
            .description("Test game description")
            .build();

    // Envoyer la requête de création
    mockMvc
        .perform(
            post("/api/games")
                .header("X-Test-User", TEST_USERNAME)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest());

    log.info("[OK] Invalid game creation rejection test passed successfully");
  }

  @Test
  void shouldRejectUnauthorizedGameCreation() throws Exception {
    // Créer une requête valide mais sans token
    CreateGameRequest request =
        CreateGameRequest.builder()
            .name("Test Game")
            .maxParticipants(5)
            .description("Test game description")
            .build();

    // Envoyer la requête sans Authorization header
    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isUnauthorized());

    log.info("[OK] Unauthorized game creation rejection test passed successfully");
  }
}
