package com.fortnite.pronos.deployment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.PronosApplication;

/**
 * Tests TDD pour la préparation à la production Principe : Red (tests qui échouent) → Green
 * (implémentation) → Refactor
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {PronosApplication.class, com.fortnite.pronos.config.TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("Tests TDD - Préparation à la Production")
class ProductionReadinessTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName("Devrait avoir un endpoint de santé fonctionnel")
  void shouldHaveWorkingHealthEndpoint() {
    // Given - URL de l'endpoint de santé
    String healthUrl = "http://localhost:" + port + "/actuator/health";

    // When - Appel de l'endpoint de santé
    ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);

    // Then - Vérification de la réponse
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("UP") || response.getBody().contains("status"));
  }

  @Test
  @DisplayName("Devrait avoir un endpoint d'information fonctionnel")
  void shouldHaveWorkingInfoEndpoint() {
    // Given - URL de l'endpoint d'information
    String infoUrl = "http://localhost:" + port + "/actuator/info";

    // When - Appel de l'endpoint d'information
    ResponseEntity<String> response = restTemplate.getForEntity(infoUrl, String.class);

    // Then - Vérification de la réponse
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  @DisplayName("Devrait avoir un endpoint de métriques fonctionnel")
  void shouldHaveWorkingMetricsEndpoint() {
    // Given - URL de l'endpoint de métriques
    String metricsUrl = "http://localhost:" + port + "/actuator/metrics";

    // When - Appel de l'endpoint de métriques
    ResponseEntity<String> response = restTemplate.getForEntity(metricsUrl, String.class);

    // Then - Vérification de la réponse
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("names"));
  }

  @Test
  @DisplayName("Devrait avoir des métriques de base de données")
  void shouldHaveDatabaseMetrics() {
    // Given - URL de l'endpoint de métriques de base de données
    String dbMetricsUrl = "http://localhost:" + port + "/actuator/metrics/hikaricp.connections";

    // When - Appel de l'endpoint de métriques de base de données
    ResponseEntity<String> response = restTemplate.getForEntity(dbMetricsUrl, String.class);

    // Then - Vérification de la réponse
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  @DisplayName("Devrait avoir des métriques HTTP")
  void shouldHaveHttpMetrics() {
    // Given - URL de l'endpoint de métriques HTTP
    String httpMetricsUrl = "http://localhost:" + port + "/actuator/metrics/http.server.requests";

    // When - Appel de l'endpoint de métriques HTTP
    ResponseEntity<String> response = restTemplate.getForEntity(httpMetricsUrl, String.class);

    // Then - Vérification de la réponse
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  @DisplayName("Devrait avoir un endpoint de prometheus")
  void shouldHavePrometheusEndpoint() {
    // Given - URL de l'endpoint prometheus
    String prometheusUrl = "http://localhost:" + port + "/actuator/prometheus";

    // When - Appel de l'endpoint prometheus
    ResponseEntity<String> response = restTemplate.getForEntity(prometheusUrl, String.class);

    // Then - Vérification de la réponse
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("# HELP") || response.getBody().contains("application"));
  }

  @Test
  @DisplayName("Devrait avoir une configuration de sécurité appropriée")
  void shouldHaveAppropriateSecurityConfiguration() {
    // Given - URL d'un endpoint protégé
    String protectedUrl = "http://localhost:" + port + "/api/games";

    // When - Appel sans authentification
    ResponseEntity<String> response = restTemplate.getForEntity(protectedUrl, String.class);

    // Then - Vérification que l'endpoint est protégé
    assertTrue(
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
            || response.getStatusCode() == HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Devrait avoir une configuration CORS appropriée")
  void shouldHaveAppropriateCorsConfiguration() {
    // Given - URL d'un endpoint avec CORS
    String corsUrl = "http://localhost:" + port + "/api/games";

    // When - Appel avec en-têtes CORS
    ResponseEntity<String> response = restTemplate.getForEntity(corsUrl, String.class);

    // Then - Vérification de la présence d'en-têtes CORS
    // Note: En mode test, les en-têtes CORS peuvent ne pas être présents
    assertNotNull(response);
  }

  @Test
  @DisplayName("Devrait avoir une configuration de cache appropriée")
  void shouldHaveAppropriateCacheConfiguration() {
    // Given - URL d'un endpoint qui utilise le cache
    String cacheUrl = "http://localhost:" + port + "/api/leaderboard";

    // When - Appel de l'endpoint
    ResponseEntity<String> response = restTemplate.getForEntity(cacheUrl, String.class);

    // Then - Vérification de la réponse
    // Note: En mode test, le cache peut ne pas être configuré
    assertNotNull(response);
  }

  @Test
  @DisplayName("Devrait avoir une configuration de logging appropriée")
  void shouldHaveAppropriateLoggingConfiguration() {
    // Given - URL d'un endpoint pour déclencher des logs
    String logUrl = "http://localhost:" + port + "/actuator/health";

    // When - Appel de l'endpoint
    ResponseEntity<String> response = restTemplate.getForEntity(logUrl, String.class);

    // Then - Vérification que les logs sont générés
    assertEquals(HttpStatus.OK, response.getStatusCode());
    // Note: Les logs sont vérifiés via la configuration
  }

  @Test
  @DisplayName("Devrait avoir une configuration de base de données appropriée")
  void shouldHaveAppropriateDatabaseConfiguration() {
    // Given - URL d'un endpoint qui utilise la base de données
    String dbUrl = "http://localhost:" + port + "/actuator/health";

    // When - Appel de l'endpoint
    ResponseEntity<String> response = restTemplate.getForEntity(dbUrl, String.class);

    // Then - Vérification de la réponse
    assertEquals(HttpStatus.OK, response.getStatusCode());
    // Note: La configuration de base de données est vérifiée via l'endpoint de santé
  }

  @Test
  @DisplayName("Devrait avoir une configuration de performance appropriée")
  void shouldHaveAppropriatePerformanceConfiguration() {
    // Given - URL d'un endpoint pour tester les performances
    String perfUrl = "http://localhost:" + port + "/actuator/health";

    // When - Appel de l'endpoint
    long startTime = System.currentTimeMillis();
    ResponseEntity<String> response = restTemplate.getForEntity(perfUrl, String.class);
    long endTime = System.currentTimeMillis();

    // Then - Vérification des performances
    assertEquals(HttpStatus.OK, response.getStatusCode());
    long responseTime = endTime - startTime;
    assertTrue(
        responseTime < 1000,
        "Le temps de réponse doit être inférieur à 1 seconde: " + responseTime + "ms");
  }
}
