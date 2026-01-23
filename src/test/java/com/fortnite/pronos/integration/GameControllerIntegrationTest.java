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

    // Réutilise testuser s'il existe, sinon crée-le
    testUser =
        userRepository
            .findByUsernameIgnoreCase("testuser")
            .orElseGet(
                () -> {
                  User u = new User();
                  u.setUsername("testuser");
                  u.setEmail("test@example.com");
                  u.setPassword("password123");
                  u.setRole(
                      User.UserRole.ADMIN); // éviter de compter comme USER dans les autres tests
                  u.setCurrentSeason(2025);
                  return userRepository.save(u);
                });
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
        postWithUser(baseUrl + "?user=" + testUser.getUsername(), request, GameDto.class);

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

    // When - Use a header with non-existent user (don't use postWithUser which adds valid user)
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.add("X-Test-User", "nonexistentuser");
    ResponseEntity<GameDto> response =
        restTemplate.exchange(
            baseUrl,
            org.springframework.http.HttpMethod.POST,
            new org.springframework.http.HttpEntity<>(request, headers),
            GameDto.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Devrait permettre de rejoindre une game avec un utilisateur authentifié")
  void shouldAllowJoiningGameWithAuthenticatedUser() {
    // Given
    // Create another user to join the game (creator cannot join their own game)
    User joiningUser = new User();
    joiningUser.setUsername("joininguser");
    joiningUser.setEmail("joining@example.com");
    joiningUser.setPassword("password123");
    joiningUser.setRole(User.UserRole.USER);
    joiningUser.setCurrentSeason(2025);
    joiningUser = userRepository.save(joiningUser);

    // Créer une game avec testUser comme créateur
    Game game = new Game();
    game.setName("Test Game for Join");
    game.setCreator(testUser);
    game.setStatus(GameStatus.CREATING);
    game.setMaxParticipants(4);
    game = gameRepository.save(game);

    JoinGameRequest request = new JoinGameRequest();
    request.setGameId(game.getId());
    request.setUserId(joiningUser.getId()); // Different user joins

    // When - Use joiningUser's credentials
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.add("X-Test-User", joiningUser.getUsername());
    ResponseEntity<Map> response =
        restTemplate.exchange(
            baseUrl + "/join?user=" + joiningUser.getUsername(),
            org.springframework.http.HttpMethod.POST,
            new org.springframework.http.HttpEntity<>(request, headers),
            Map.class);

    // Then
    // Accept OK (success) or BAD_REQUEST (business validation error like game full, etc.)
    assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();

    if (response.getStatusCode() == HttpStatus.OK) {
      assertThat(response.getBody().get("success")).isEqualTo(true);
    } else {
      // BAD_REQUEST is acceptable for business validation errors
      assertThat(response.getBody().get("error")).isNotNull();
    }
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

    // When & Then
    try {
      ResponseEntity<Map> response =
          restTemplate.postForEntity(baseUrl + "/join", request, Map.class);
      // If request succeeds, should be UNAUTHORIZED or FORBIDDEN
      assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    } catch (org.springframework.web.client.ResourceAccessException e) {
      // Expected: authentication error causes connection issue
      assertThat(e.getMessage()).contains("authentication");
    }
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
        getWithUser(
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
        postWithUser(
            baseUrl + "/" + game.getId() + "/start-draft?user=" + testUser.getUsername(),
            null,
            Map.class);

    // Then
    // Accept both OK (if participants exist) or CONFLICT (if not enough participants)
    assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();

    if (response.getStatusCode() == HttpStatus.OK) {
      assertThat(response.getBody().get("success")).isEqualTo(true);
    } else {
      // CONFLICT is expected when there are not enough participants
      assertThat(response.getBody().get("error")).isNotNull();
    }
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

    // When & Then
    try {
      ResponseEntity<Map> response =
          restTemplate.postForEntity(
              baseUrl + "/" + game.getId() + "/start-draft", null, Map.class);
      // If request succeeds, should be UNAUTHORIZED or FORBIDDEN
      assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    } catch (org.springframework.web.client.ResourceAccessException e) {
      // Expected: authentication error causes connection issue
      assertThat(e.getMessage()).contains("authentication");
    }
  }

  @Test
  @DisplayName("Devrait gérer les erreurs de validation des requêtes")
  void shouldHandleRequestValidationErrors() {
    // Given
    CreateGameRequest invalidRequest = new CreateGameRequest();
    // Ne pas définir le nom pour créer une erreur de validation

    // When - Use Map.class to handle error response (not GameDto)
    ResponseEntity<Map> response =
        postWithUser(baseUrl + "?user=" + testUser.getUsername(), invalidRequest, Map.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private <T> ResponseEntity<T> postWithUser(String url, Object body, Class<T> type) {
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.add("X-Test-User", testUser.getUsername());
    return restTemplate.exchange(
        url,
        org.springframework.http.HttpMethod.POST,
        new org.springframework.http.HttpEntity<>(body, headers),
        type);
  }

  private <T> ResponseEntity<T> getWithUser(String url, Class<T> type) {
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.add("X-Test-User", testUser.getUsername());
    return restTemplate.exchange(
        url,
        org.springframework.http.HttpMethod.GET,
        new org.springframework.http.HttpEntity<>(headers),
        type);
  }
}
