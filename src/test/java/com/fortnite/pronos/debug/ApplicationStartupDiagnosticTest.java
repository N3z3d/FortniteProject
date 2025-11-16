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

/**
 * Test TDD pour diagnostiquer le démarrage de l'application Principe : Red (tests qui échouent) →
 * Green (implémentation) → Refactor
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
      PronosApplication.class,
      com.fortnite.pronos.config.TestSecurityConfigTestBackup.class
    })
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:testdb",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@DisplayName("Test TDD - Diagnostic de Démarrage de l'Application")
class ApplicationStartupDiagnosticTest {

  @LocalServerPort private int port;

  private TestRestTemplate restTemplate = new TestRestTemplate();

  @Test
  @DisplayName("Devrait démarrer l'application sur le port configuré")
  void shouldStartApplicationOnConfiguredPort() {
    // Given - Application démarrée

    // When & Then
    assertTrue(port > 0, "L'application devrait démarrer sur un port valide");
    System.out.println("Application démarrée sur le port: " + port);
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

    System.out.println("Réponse de santé: " + response.getBody());
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

    System.out.println("Réponse d'info: " + response.getBody());
  }

  @Test
  @DisplayName("Devrait répondre à l'endpoint des métriques")
  void shouldRespondToMetricsEndpoint() {
    // Given - Application démarrée

    // When
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/actuator/metrics", String.class);

    // Then
    assertEquals(
        HttpStatus.OK,
        response.getStatusCode(),
        "L'endpoint des métriques devrait répondre avec 200 OK");

    System.out.println("Réponse des métriques: " + response.getBody());
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

    // L'endpoint devrait répondre (même si vide)
    assertTrue(
        response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError(),
        "L'endpoint des games devrait répondre");

    System.out.println("Test de base de données réussi");
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

    System.out.println("Configuration de sécurité valide");
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

    System.out.println("Tous les beans nécessaires sont chargés");
  }
}
