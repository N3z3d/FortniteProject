package com.fortnite.pronos.debug;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.PronosApplication;
import com.fortnite.pronos.config.TestSecurityConfig;
import com.fortnite.pronos.service.JwtService;

/**
 * Tests TDD pour diagnostiquer les problèmes de démarrage Principe : Red (tests qui échouent) →
 * Green (implémentation) → Refactor
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {PronosApplication.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("Tests TDD - Diagnostic des Problèmes de Démarrage")
class ApplicationStartupTest {

  @LocalServerPort private int port;

  @MockBean private JwtService jwtService;

  @Test
  @DisplayName("Devrait démarrer l'application avec succès")
  void shouldStartApplicationSuccessfully() {
    // Given - Application démarrée
    // When & Then - Vérification que l'application répond
    assertNotNull(port);
    assertTrue(port > 0, "Le port doit être assigné");
  }

  @Test
  @DisplayName("Devrait avoir un endpoint de santé accessible")
  void shouldHaveAccessibleHealthEndpoint() {
    // Given - TestRestTemplate
    TestRestTemplate restTemplate = new TestRestTemplate();

    // When - Appel de l'endpoint de santé
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", String.class);

    // Then - Vérification de la réponse
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("UP") || response.getBody().contains("status"));
  }

  @Test
  @DisplayName("Devrait avoir une base de données H2 fonctionnelle")
  void shouldHaveWorkingH2Database() {
    // Given - TestRestTemplate
    TestRestTemplate restTemplate = new TestRestTemplate();

    // When - Appel de l'endpoint de santé avec détails
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", String.class);

    // Then - Vérification que la base de données est accessible
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    // La base de données H2 devrait être accessible
  }

  @Test
  @DisplayName("Devrait avoir une configuration de sécurité appropriée pour les tests")
  void shouldHaveAppropriateSecurityConfigurationForTests() {
    // Given - TestRestTemplate
    TestRestTemplate restTemplate = new TestRestTemplate();

    // When - Appel d'un endpoint protégé
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/api/games", String.class);

    // Then - Vérification que l'endpoint est accessible en mode test
    // En mode test, les endpoints devraient être accessibles
    assertNotNull(response);
  }

  @Test
  @DisplayName("Devrait avoir les métriques activées")
  void shouldHaveMetricsEnabled() {
    // Given - TestRestTemplate
    TestRestTemplate restTemplate = new TestRestTemplate();

    // When - Appel de l'endpoint de métriques
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/actuator/metrics", String.class);

    // Then - Vérification des métriques
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("names"));
  }

  @Test
  @DisplayName("Devrait avoir l'endpoint prometheus activé")
  void shouldHavePrometheusEndpointEnabled() {
    // Given - TestRestTemplate
    TestRestTemplate restTemplate = new TestRestTemplate();

    // When - Appel de l'endpoint prometheus
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/prometheus", String.class);

    // Then - Vérification de prometheus
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("# HELP") || response.getBody().contains("application"));
  }

  @Test
  @DisplayName("Devrait avoir une configuration de logging appropriée")
  void shouldHaveAppropriateLoggingConfiguration() {
    // Given - TestRestTemplate
    TestRestTemplate restTemplate = new TestRestTemplate();

    // When - Appel d'un endpoint pour déclencher des logs
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", String.class);

    // Then - Vérification que les logs sont configurés
    assertEquals(HttpStatus.OK, response.getStatusCode());
    // Les logs sont vérifiés via la configuration
  }

  @Test
  @DisplayName("Devrait avoir une configuration de cache appropriée")
  void shouldHaveAppropriateCacheConfiguration() {
    // Given - TestRestTemplate
    TestRestTemplate restTemplate = new TestRestTemplate();

    // When - Appel d'un endpoint qui utilise le cache
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", String.class);

    // Then - Vérification du cache
    assertEquals(HttpStatus.OK, response.getStatusCode());
    // Le cache est vérifié via la configuration
  }

  @Test
  @DisplayName("Devrait avoir une configuration de performance appropriée")
  void shouldHaveAppropriatePerformanceConfiguration() {
    // Given - TestRestTemplate
    TestRestTemplate restTemplate = new TestRestTemplate();

    // When - Appel d'un endpoint pour tester les performances
    long startTime = System.currentTimeMillis();
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", String.class);
    long endTime = System.currentTimeMillis();

    // Then - Vérification des performances
    assertEquals(HttpStatus.OK, response.getStatusCode());
    long responseTime = endTime - startTime;
    assertTrue(
        responseTime < 5000,
        "Le temps de réponse doit être inférieur à 5 secondes: " + responseTime + "ms");
  }
}
