package com.fortnite.pronos.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

/** Test d'intégration TDD pour vérifier l'authentification des games */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GameControllerAuthenticationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private UserRepository userRepository;

  private User testUser;

  @BeforeEach
  void setUp() {
    // Réutilise l'utilisateur seedé ou crée-le si absent
    testUser =
        userRepository
            .findByUsernameIgnoreCase("Thibaut")
            .orElseGet(
                () -> {
                  User u = new User();
                  u.setUsername("Thibaut");
                  u.setEmail("thibaut@test.com");
                  u.setPassword("password123");
                  u.setRole(User.UserRole.USER);
                  u.setCurrentSeason(2025);
                  return userRepository.save(u);
                });
  }

  @Test
  @DisplayName("Devrait créer une game avec un utilisateur authentifié")
  void shouldCreateGameWithAuthenticatedUser() {
    // Given
    Map<String, Object> gameRequest = new HashMap<>();
    gameRequest.put("name", "Tournoi Test TDD");
    gameRequest.put("maxParticipants", 4);

    Map<String, Integer> regionRules = new HashMap<>();
    regionRules.put("EU", 2);
    regionRules.put("NAW", 1);
    regionRules.put("BR", 1);
    gameRequest.put("regionRules", regionRules);

    // When
    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(
            "http://localhost:" + port + "/api/games?user=Thibaut",
            org.springframework.http.HttpMethod.POST,
            new org.springframework.http.HttpEntity<>(gameRequest, withTestUserHeader("Thibaut")),
            new ParameterizedTypeReference<Map<String, Object>>() {});

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("name")).isEqualTo("Tournoi Test TDD");
    assertThat(response.getBody().get("creatorName")).isEqualTo("Thibaut");
  }

  @Test
  @DisplayName("Devrait rejeter la création de game sans utilisateur")
  void shouldRejectGameCreationWithoutUser() {
    // Given
    Map<String, Object> gameRequest = new HashMap<>();
    gameRequest.put("name", "Tournoi Test TDD");
    gameRequest.put("maxParticipants", 4);

    // When
    ResponseEntity<Void> response =
        restTemplate.exchange(
            "http://localhost:" + port + "/api/games",
            org.springframework.http.HttpMethod.POST,
            new org.springframework.http.HttpEntity<>(gameRequest),
            Void.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Devrait rejeter la création de game avec un utilisateur inexistant")
  void shouldRejectGameCreationWithNonExistentUser() {
    // Given
    Map<String, Object> gameRequest = new HashMap<>();
    gameRequest.put("name", "Tournoi Test TDD");
    gameRequest.put("maxParticipants", 4);

    // When
    ResponseEntity<Void> response =
        restTemplate.exchange(
            "http://localhost:" + port + "/api/games?user=UtilisateurInexistant",
            org.springframework.http.HttpMethod.POST,
            new org.springframework.http.HttpEntity<>(gameRequest),
            Void.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Devrait retourner les games de l'utilisateur authentifié")
  void shouldReturnUserGames() {
    // Given - Créer d'abord une game
    Map<String, Object> gameRequest = new HashMap<>();
    gameRequest.put("name", "Tournoi Test TDD");
    gameRequest.put("maxParticipants", 4);

    Map<String, Integer> regionRules = new HashMap<>();
    regionRules.put("EU", 2);
    regionRules.put("NAW", 1);
    regionRules.put("BR", 1);
    gameRequest.put("regionRules", regionRules);

    restTemplate.exchange(
        "http://localhost:" + port + "/api/games?user=Thibaut",
        org.springframework.http.HttpMethod.POST,
        new org.springframework.http.HttpEntity<>(gameRequest, withTestUserHeader("Thibaut")),
        Void.class);

    // When
    ResponseEntity<Object[]> response =
        restTemplate.exchange(
            "http://localhost:" + port + "/api/games?user=Thibaut",
            org.springframework.http.HttpMethod.GET,
            new org.springframework.http.HttpEntity<>(withTestUserHeader("Thibaut")),
            Object[].class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isGreaterThan(0);
  }

  @Test
  @DisplayName("Devrait retourner un tableau vide pour un utilisateur sans games")
  void shouldReturnEmptyArrayForUserWithoutGames() {
    User noGameUser = new User();
    noGameUser.setUsername("NoGameUser");
    noGameUser.setEmail("nogame@test.com");
    noGameUser.setPassword("password123");
    noGameUser.setRole(User.UserRole.USER);
    noGameUser.setCurrentSeason(2025);
    userRepository.save(noGameUser);

    // When
    ResponseEntity<Object[]> response =
        restTemplate.exchange(
            "http://localhost:" + port + "/api/games?user=NoGameUser",
            org.springframework.http.HttpMethod.GET,
            new org.springframework.http.HttpEntity<>(withTestUserHeader("NoGameUser")),
            Object[].class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isEqualTo(0);
  }

  private org.springframework.http.HttpHeaders withTestUserHeader(String username) {
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.add("X-Test-User", username);
    return headers;
  }
}
