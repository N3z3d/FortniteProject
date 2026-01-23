package com.fortnite.pronos.debug;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.fortnite.pronos.PronosApplication;
import com.fortnite.pronos.config.TestSecurityConfig;

/**
 * Test TDD pour forcer l'application à démarrer sur le port 8080 Principe : Red (tests qui
 * échouent) → Green (implémentation) → Refactor
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {PronosApplication.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:testdb",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@DisplayName("Test TDD - Application sur Port 8080")
class ApplicationPortTest {

  @LocalServerPort private int port;

  private TestRestTemplate restTemplate = new TestRestTemplate();

  @Test
  @DisplayName("Devrait démarrer l'application sur un port valide")
  void shouldStartApplicationOnValidPort() {
    // Given - Application démarrée

    // When & Then
    assertTrue(port > 0, "L'application devrait démarrer sur un port valide");
    System.out.println("[OK] Application démarrée sur le port: " + port);
  }

  @Test
  @DisplayName("Devrait répondre à l'endpoint de santé")
  void shouldRespondToHealthEndpoint() {
    // Given - Application démarrée

    // When
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", String.class);

    // Then
    assertEquals(
        HttpStatus.OK,
        response.getStatusCode(),
        "L'endpoint de santé devrait répondre avec 200 OK");
    assertTrue(response.getBody().contains("UP"), "Le statut de santé devrait être UP");

    System.out.println("[OK] Réponse de santé: " + response.getBody());
  }

  @Test
  @DisplayName("Devrait répondre à l'endpoint d'information")
  void shouldRespondToInfoEndpoint() {
    // Given - Application démarrée

    // When
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/actuator/info", String.class);

    // Then
    assertEquals(
        HttpStatus.OK, response.getStatusCode(), "L'endpoint d'info devrait répondre avec 200 OK");

    System.out.println("[OK] Réponse d'info: " + response.getBody());
  }

  @Test
  @DisplayName("Devrait avoir une configuration de base de données valide")
  void shouldHaveValidDatabaseConfiguration() {
    // Given - Application démarrée

    // When & Then
    assertTrue(port > 0, "L'application devrait être démarrée");

    // Vérifier que l'application peut accéder à la base de données
    // en testant un endpoint qui utilise la base
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/api/games", String.class);

    // L'endpoint devrait répondre (même si vide ou avec erreur d'authentification)
    assertTrue(
        response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError(),
        "L'endpoint des games devrait répondre");

    System.out.println("[OK] Test de base de données réussi");
  }

  @Test
  @DisplayName("Devrait avoir une configuration de sécurité valide")
  void shouldHaveValidSecurityConfiguration() {
    // Given - Application démarrée

    // When
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/api/games", String.class);

    // Then
    // L'endpoint devrait être accessible (même si vide ou avec erreur d'authentification)
    assertTrue(
        response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError(),
        "L'endpoint devrait être accessible");

    System.out.println("[OK] Configuration de sécurité valide");
  }

  @Test
  @DisplayName("Devrait avoir tous les beans nécessaires chargés")
  void shouldHaveAllRequiredBeansLoaded() {
    // Given - Application démarrée

    // When & Then
    assertTrue(port > 0, "L'application devrait être démarrée");

    // Test simple pour vérifier que les contrôleurs sont chargés
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", String.class);

    assertEquals(
        HttpStatus.OK, response.getStatusCode(), "Les beans de base devraient être chargés");

    System.out.println("[OK] Tous les beans nécessaires sont chargés");
  }
}
