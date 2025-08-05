package com.fortnite.pronos.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;

/** Test d'intégration pour le GameController avec authentification */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
      com.fortnite.pronos.PronosApplication.class,
      com.fortnite.pronos.config.TestSecurityConfig.class
    })
@ActiveProfiles("test")
@Transactional
public class GameControllerIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private UserRepository userRepository;

  @Autowired private GameRepository gameRepository;

  private ObjectMapper objectMapper;
  private User testUser;
  private String baseUrl;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    baseUrl = "http://localhost:" + port + "/api/games";

    // Créer un utilisateur de test
    testUser = new User();
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword("password123");
    testUser = userRepository.save(testUser);
  }

  @Test
  @DisplayName("Devrait créer une game avec un utilisateur authentifié via paramètre")
  void shouldCreateGameWithAuthenticatedUserViaParam() {
    // Given
    CreateGameRequest request = new CreateGameRequest();
    request.setName("Test Game Integration");
    request.setMaxParticipants(4);

    // When
    ResponseEntity<GameDto> response =
        restTemplate.postForEntity(
            baseUrl + "?user=" + testUser.getUsername(), request, GameDto.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getName()).isEqualTo("Test Game Integration");
    assertThat(response.getBody().getCreatorId()).isEqualTo(testUser.getId());
  }

  @Test
  @DisplayName("Devrait refuser la création de game sans utilisateur")
  void shouldRejectGameCreationWithoutUser() {
    // Given
    CreateGameRequest request = new CreateGameRequest();
    request.setName("Test Game No User");
    request.setMaxParticipants(4);

    // When
    ResponseEntity<GameDto> response = restTemplate.postForEntity(baseUrl, request, GameDto.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Devrait refuser la création de game avec un utilisateur inexistant")
  void shouldRejectGameCreationWithNonExistentUser() {
    // Given
    CreateGameRequest request = new CreateGameRequest();
    request.setName("Test Game Invalid User");
    request.setMaxParticipants(4);

    // When
    ResponseEntity<GameDto> response =
        restTemplate.postForEntity(baseUrl + "?user=nonexistentuser", request, GameDto.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Devrait permettre de rejoindre une game avec un utilisateur authentifié")
  void shouldAllowJoiningGameWithAuthenticatedUser() {
    // Given
    // Créer d'abord une game
    Game game = new Game();
    game.setName("Test Game for Join");
    game.setCreator(testUser);
    game.setStatus(GameStatus.CREATING);
    game.setMaxParticipants(4);
    game = gameRepository.save(game);

    JoinGameRequest request = new JoinGameRequest();
    request.setGameId(game.getId());

    // When
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            baseUrl + "/join?user=" + testUser.getUsername(), request, Map.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("success")).isEqualTo(true);
  }

  @Test
  @DisplayName("Devrait refuser de rejoindre une game sans utilisateur")
  void shouldRejectJoiningGameWithoutUser() {
    // Given
    Game game = new Game();
    game.setName("Test Game for Join No User");
    game.setCreator(testUser);
    game.setStatus(GameStatus.CREATING);
    game.setMaxParticipants(4);
    game = gameRepository.save(game);

    JoinGameRequest request = new JoinGameRequest();
    request.setGameId(game.getId());

    // When
    ResponseEntity<Map> response =
        restTemplate.postForEntity(baseUrl + "/join", request, Map.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Devrait retourner une game par ID avec utilisateur authentifié")
  void shouldReturnGameByIdWithAuthenticatedUser() {
    // Given
    Game game = new Game();
    game.setName("Test Game for Get");
    game.setCreator(testUser);
    game.setStatus(GameStatus.CREATING);
    game.setMaxParticipants(4);
    game = gameRepository.save(game);

    // When
    ResponseEntity<GameDto> response =
        restTemplate.getForEntity(
            baseUrl + "/" + game.getId() + "?user=" + testUser.getUsername(), GameDto.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo(game.getId());
    assertThat(response.getBody().getName()).isEqualTo("Test Game for Get");
  }

  @Test
  @DisplayName("Devrait refuser l'accès à une game sans utilisateur")
  void shouldRejectGameAccessWithoutUser() {
    // Given
    Game game = new Game();
    game.setName("Test Game No Access");
    game.setCreator(testUser);
    game.setStatus(GameStatus.CREATING);
    game.setMaxParticipants(4);
    game = gameRepository.save(game);

    // When
    ResponseEntity<GameDto> response =
        restTemplate.getForEntity(baseUrl + "/" + game.getId(), GameDto.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Devrait démarrer un draft avec un utilisateur authentifié")
  void shouldStartDraftWithAuthenticatedUser() {
    // Given
    Game game = new Game();
    game.setName("Test Game for Draft");
    game.setCreator(testUser);
    game.setStatus(GameStatus.CREATING);
    game.setMaxParticipants(4);
    game = gameRepository.save(game);

    // When
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            baseUrl + "/" + game.getId() + "/start-draft?user=" + testUser.getUsername(),
            null,
            Map.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("success")).isEqualTo(true);
  }

  @Test
  @DisplayName("Devrait refuser de démarrer un draft sans utilisateur")
  void shouldRejectStartingDraftWithoutUser() {
    // Given
    Game game = new Game();
    game.setName("Test Game for Draft No User");
    game.setCreator(testUser);
    game.setStatus(GameStatus.CREATING);
    game.setMaxParticipants(4);
    game = gameRepository.save(game);

    // When
    ResponseEntity<Map> response =
        restTemplate.postForEntity(baseUrl + "/" + game.getId() + "/start-draft", null, Map.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Devrait retourner toutes les games disponibles")
  void shouldReturnAllAvailableGames() {
    // Given
    Game game1 = new Game();
    game1.setName("Available Game 1");
    game1.setCreator(testUser);
    game1.setStatus(GameStatus.CREATING);
    game1.setMaxParticipants(4);
    gameRepository.save(game1);

    Game game2 = new Game();
    game2.setName("Available Game 2");
    game2.setCreator(testUser);
    game2.setStatus(GameStatus.CREATING);
    game2.setMaxParticipants(4);
    gameRepository.save(game2);

    // When
    ResponseEntity<GameDto[]> response =
        restTemplate.getForEntity(baseUrl + "/available", GameDto[].class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isGreaterThanOrEqualTo(2);
  }

  @Test
  @DisplayName("Devrait gérer les erreurs de validation des requêtes")
  void shouldHandleRequestValidationErrors() {
    // Given
    CreateGameRequest invalidRequest = new CreateGameRequest();
    // Ne pas définir le nom pour créer une erreur de validation

    // When
    ResponseEntity<GameDto> response =
        restTemplate.postForEntity(
            baseUrl + "?user=" + testUser.getUsername(), invalidRequest, GameDto.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
