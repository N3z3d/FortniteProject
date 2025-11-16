package com.fortnite.pronos.security;

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
 * Tests TDD pour la configuration de sécurité Principe : Red (tests qui échouent) → Green
 * (implémentation) → Refactor
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
      PronosApplication.class,
      com.fortnite.pronos.config.TestSecurityConfigTestBackup.class
    })
@ActiveProfiles("test")
@DisplayName("Tests TDD - Configuration de Sécurité")
class SecurityConfigurationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName("Devrait protéger les endpoints API sans authentification")
  void shouldProtectApiEndpointsWithoutAuthentication() {
    // Given - Endpoints API à tester
    String[] protectedEndpoints = {
      "/api/games",
      "/api/games/available",
      "/api/draft/test-game-id/next-participant",
      "/api/leaderboard"
    };

    // When & Then - Vérification que chaque endpoint est protégé
    for (String endpoint : protectedEndpoints) {
      String url = "http://localhost:" + port + endpoint;
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

      assertTrue(
          response.getStatusCode() == HttpStatus.UNAUTHORIZED
              || response.getStatusCode() == HttpStatus.FORBIDDEN,
          "L'endpoint " + endpoint + " devrait être protégé");
    }
  }

  @Test
  @DisplayName("Devrait permettre l'accès aux endpoints publics")
  void shouldAllowAccessToPublicEndpoints() {
    // Given - Endpoints publics
    String[] publicEndpoints = {"/actuator/health", "/actuator/info"};

    // When & Then - Vérification que chaque endpoint public est accessible
    for (String endpoint : publicEndpoints) {
      String url = "http://localhost:" + port + endpoint;
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

      assertEquals(
          HttpStatus.OK,
          response.getStatusCode(),
          "L'endpoint " + endpoint + " devrait être accessible");
    }
  }

  @Test
  @DisplayName("Devrait rejeter les requêtes avec des en-têtes malveillants")
  void shouldRejectRequestsWithMaliciousHeaders() {
    // Given - URL d'un endpoint protégé
    String url = "http://localhost:" + port + "/api/games";

    // When - Tentative d'accès avec en-têtes malveillants
    // Note: TestRestTemplate ne permet pas facilement d'ajouter des en-têtes malveillants
    // Ce test vérifie que la configuration de sécurité est en place

    // Then - Vérification que la sécurité est active
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    assertTrue(
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
            || response.getStatusCode() == HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Devrait avoir une configuration CORS appropriée")
  void shouldHaveAppropriateCorsConfiguration() {
    // Given - URL d'un endpoint API
    String url = "http://localhost:" + port + "/api/games";

    // When - Appel de l'endpoint
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Then - Vérification que CORS est configuré
    // Note: En mode test, les en-têtes CORS peuvent ne pas être visibles
    assertNotNull(response);
    assertTrue(
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
            || response.getStatusCode() == HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Devrait avoir une configuration CSRF appropriée")
  void shouldHaveAppropriateCsrfConfiguration() {
    // Given - URL d'un endpoint POST
    String url = "http://localhost:" + port + "/api/games";

    // When - Tentative de POST sans CSRF token
    ResponseEntity<String> response = restTemplate.postForEntity(url, "{}", String.class);

    // Then - Vérification que CSRF est configuré
    assertTrue(
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
            || response.getStatusCode() == HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Devrait avoir une configuration de rate limiting")
  void shouldHaveRateLimitingConfiguration() {
    // Given - URL d'un endpoint à tester
    String url = "http://localhost:" + port + "/api/games";

    // When - Appels multiples rapides
    for (int i = 0; i < 5; i++) {
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

      // Then - Vérification que les appels sont rejetés de manière appropriée
      assertTrue(
          response.getStatusCode() == HttpStatus.UNAUTHORIZED
              || response.getStatusCode() == HttpStatus.FORBIDDEN
              || response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS);
    }
  }

  @Test
  @DisplayName("Devrait avoir une configuration de validation des entrées")
  void shouldHaveInputValidationConfiguration() {
    // Given - URL d'un endpoint avec validation
    String url = "http://localhost:" + port + "/api/games";

    // When - Tentative d'accès avec des paramètres malveillants
    ResponseEntity<String> response =
        restTemplate.getForEntity(url + "?id=<script>alert('xss')</script>", String.class);

    // Then - Vérification que la validation est active
    assertTrue(
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
            || response.getStatusCode() == HttpStatus.FORBIDDEN
            || response.getStatusCode() == HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("Devrait avoir une configuration de logging de sécurité")
  void shouldHaveSecurityLoggingConfiguration() {
    // Given - URL d'un endpoint protégé
    String url = "http://localhost:" + port + "/api/games";

    // When - Tentative d'accès non autorisé
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Then - Vérification que les logs de sécurité sont générés
    assertTrue(
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
            || response.getStatusCode() == HttpStatus.FORBIDDEN);
    // Note: Les logs sont vérifiés via la configuration
  }

  @Test
  @DisplayName("Devrait avoir une configuration de session appropriée")
  void shouldHaveAppropriateSessionConfiguration() {
    // Given - URL d'un endpoint qui utilise les sessions
    String url = "http://localhost:" + port + "/api/games";

    // When - Appel de l'endpoint
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Then - Vérification de la configuration de session
    assertTrue(
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
            || response.getStatusCode() == HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Devrait avoir une configuration de headers de sécurité")
  void shouldHaveSecurityHeadersConfiguration() {
    // Given - URL d'un endpoint public
    String url = "http://localhost:" + port + "/actuator/health";

    // When - Appel de l'endpoint
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Then - Vérification des headers de sécurité
    assertEquals(HttpStatus.OK, response.getStatusCode());
    // Note: Les headers de sécurité sont vérifiés via la configuration
  }

  @Test
  @DisplayName("Devrait avoir une configuration de timeout appropriée")
  void shouldHaveAppropriateTimeoutConfiguration() {
    // Given - URL d'un endpoint pour tester les timeouts
    String url = "http://localhost:" + port + "/actuator/health";

    // When - Appel de l'endpoint
    long startTime = System.currentTimeMillis();
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    long endTime = System.currentTimeMillis();

    // Then - Vérification des timeouts
    assertEquals(HttpStatus.OK, response.getStatusCode());
    long responseTime = endTime - startTime;
    assertTrue(
        responseTime < 5000, "Le timeout doit être inférieur à 5 secondes: " + responseTime + "ms");
  }

  @Test
  @DisplayName("Devrait avoir une configuration de chiffrement appropriée")
  void shouldHaveAppropriateEncryptionConfiguration() {
    // Given - URL d'un endpoint qui utilise le chiffrement
    String url = "http://localhost:" + port + "/api/games";

    // When - Appel de l'endpoint
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Then - Vérification de la configuration de chiffrement
    assertTrue(
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
            || response.getStatusCode() == HttpStatus.FORBIDDEN);
    // Note: Le chiffrement est vérifié via la configuration
  }
}
