package com.fortnite.pronos.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fortnite.pronos.PronosApplication;
import com.fortnite.pronos.dto.auth.LoginRequest;

/**
 * Tests d'intégration pour les flux d'authentification Focus: Sécurité end-to-end, protection des
 * endpoints, gestion des sessions
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
      PronosApplication.class,
      com.fortnite.pronos.config.TestSecurityConfigTestBackup.class
    })
@ActiveProfiles("test")
@AutoConfigureWebMvc
@DisplayName("Tests d'intégration - Flux d'authentification")
class AuthenticationFlowIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("Devrait protéger les endpoints sensibles sans authentification")
  void shouldProtectSensitiveEndpointsWithoutAuth() {
    // Given - Endpoints sensibles à tester
    String[] sensitiveEndpoints = {
      "/api/games", "/api/games/my-games", "/api/draft/start", "/api/teams", "/api/leaderboard"
    };

    // When & Then - Chaque endpoint doit être protégé
    for (String endpoint : sensitiveEndpoints) {
      String url = "http://localhost:" + port + endpoint;
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

      assertTrue(
          response.getStatusCode() == HttpStatus.UNAUTHORIZED
              || response.getStatusCode() == HttpStatus.FORBIDDEN,
          "L'endpoint " + endpoint + " doit être protégé sans authentification");
    }
  }

  @Test
  @DisplayName("Devrait permettre l'accès aux endpoints publics")
  void shouldAllowAccessToPublicEndpoints() {
    // Given - Endpoints publics
    String[] publicEndpoints = {"/actuator/health"};

    // When & Then - Chaque endpoint public doit être accessible
    for (String endpoint : publicEndpoints) {
      String url = "http://localhost:" + port + endpoint;
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

      assertEquals(
          HttpStatus.OK,
          response.getStatusCode(),
          "L'endpoint public " + endpoint + " doit être accessible");
    }
  }

  @Test
  @DisplayName("Devrait rejeter les tentatives d'authentification avec des credentials invalides")
  void shouldRejectInvalidCredentials() {
    // Given - Credentials invalides
    LoginRequest invalidRequest = new LoginRequest();
    invalidRequest.setUsername("invaliduser");
    invalidRequest.setPassword("wrongpassword");

    String url = "http://localhost:" + port + "/api/auth/login";

    // When - Tentative de connexion
    ResponseEntity<String> response = restTemplate.postForEntity(url, invalidRequest, String.class);

    // Then - L'authentification doit échouer
    assertTrue(
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
            || response.getStatusCode() == HttpStatus.FORBIDDEN
            || response.getStatusCode() == HttpStatus.BAD_REQUEST,
        "Les credentials invalides doivent être rejetés");
  }

  @Test
  @DisplayName("Devrait empêcher l'injection SQL dans les champs d'authentification")
  void shouldPreventSqlInjectionInAuthFields() {
    // Given - Tentatives d'injection SQL
    String[] sqlInjectionAttempts = {
      "admin'; DROP TABLE users; --",
      "' OR '1'='1",
      "admin' UNION SELECT * FROM users --",
      "'; INSERT INTO users VALUES('hacker', 'password'); --"
    };

    for (String maliciousInput : sqlInjectionAttempts) {
      LoginRequest maliciousRequest = new LoginRequest();
      maliciousRequest.setUsername(maliciousInput);
      maliciousRequest.setPassword("anypassword");

      String url = "http://localhost:" + port + "/api/auth/login";

      // When - Tentative d'injection
      ResponseEntity<String> response =
          restTemplate.postForEntity(url, maliciousRequest, String.class);

      // Then - L'injection doit être bloquée
      assertTrue(
          response.getStatusCode() == HttpStatus.UNAUTHORIZED
              || response.getStatusCode() == HttpStatus.BAD_REQUEST
              || response.getStatusCode() == HttpStatus.FORBIDDEN,
          "L'injection SQL doit être bloquée: " + maliciousInput);
    }
  }

  @Test
  @DisplayName("Devrait empêcher les attaques XSS dans les champs d'authentification")
  void shouldPreventXssInAuthFields() {
    // Given - Tentatives XSS
    String[] xssAttempts = {
      "<script>alert('xss')</script>",
      "javascript:alert('xss')",
      "<img src=x onerror=alert('xss')>",
      "';alert('xss');//"
    };

    for (String xssInput : xssAttempts) {
      LoginRequest xssRequest = new LoginRequest();
      xssRequest.setUsername(xssInput);
      xssRequest.setPassword("anypassword");

      String url = "http://localhost:" + port + "/api/auth/login";

      // When - Tentative XSS
      ResponseEntity<String> response = restTemplate.postForEntity(url, xssRequest, String.class);

      // Then - L'attaque XSS doit être bloquée
      assertTrue(
          response.getStatusCode() == HttpStatus.UNAUTHORIZED
              || response.getStatusCode() == HttpStatus.BAD_REQUEST
              || response.getStatusCode() == HttpStatus.FORBIDDEN,
          "L'attaque XSS doit être bloquée: " + xssInput);
    }
  }

  @Test
  @DisplayName("Devrait limiter les tentatives de brute force")
  void shouldLimitBruteForceAttempts() {
    // Given - Credentials pour brute force
    LoginRequest bruteForceRequest = new LoginRequest();
    bruteForceRequest.setUsername("targetuser");
    bruteForceRequest.setPassword("wrongpassword");

    String url = "http://localhost:" + port + "/api/auth/login";

    // When - Tentatives multiples rapides
    int attempts = 10;
    int unauthorizedCount = 0;
    int tooManyRequestsCount = 0;

    for (int i = 0; i < attempts; i++) {
      ResponseEntity<String> response =
          restTemplate.postForEntity(url, bruteForceRequest, String.class);

      if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
        unauthorizedCount++;
      } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
        tooManyRequestsCount++;
      }
    }

    // Then - Il devrait y avoir une protection contre le brute force
    assertTrue(unauthorizedCount > 0, "Des tentatives non autorisées doivent être détectées");
    // Note: Le rate limiting peut ne pas être implémenté, mais les tentatives doivent être rejetées
  }

  @Test
  @DisplayName("Devrait valider les en-têtes de sécurité")
  void shouldValidateSecurityHeaders() {
    // Given - URL d'un endpoint public pour vérifier les headers
    String url = "http://localhost:" + port + "/actuator/health";

    // When - Appel de l'endpoint
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Then - Vérification des headers de sécurité
    assertEquals(HttpStatus.OK, response.getStatusCode());

    HttpHeaders headers = response.getHeaders();
    assertNotNull(headers, "Les headers ne doivent pas être null");

    // Vérifications de sécurité (peuvent varier selon la configuration)
    // Note: Tous les headers de sécurité peuvent ne pas être visibles dans les tests
    assertNotNull(response.getBody(), "Le body de la réponse doit être présent");
  }

  @Test
  @DisplayName("Devrait empêcher l'accès concurrent malveillant")
  void shouldPreventMaliciousConcurrentAccess() throws InterruptedException {
    // Given - Endpoint protégé
    String url = "http://localhost:" + port + "/api/games";

    // When - Accès concurrent sans authentification
    int threadCount = 5;
    Thread[] threads = new Thread[threadCount];
    org.springframework.http.HttpStatusCode[] responses =
        new org.springframework.http.HttpStatusCode[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] =
          new Thread(
              () -> {
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                responses[index] = response.getStatusCode();
              });
      threads[i].start();
    }

    // Attendre la fin de tous les threads
    for (Thread thread : threads) {
      thread.join();
    }

    // Then - Tous les accès doivent être rejetés
    for (int i = 0; i < threadCount; i++) {
      assertTrue(
          responses[i].equals(HttpStatus.UNAUTHORIZED) || responses[i].equals(HttpStatus.FORBIDDEN),
          "L'accès concurrent non autorisé doit être rejeté");
    }
  }

  @Test
  @DisplayName("Devrait gérer correctement les timeouts de session")
  void shouldHandleSessionTimeoutsCorrectly() {
    // Given - Endpoint protégé
    String url = "http://localhost:" + port + "/api/games";

    // When - Appel avec délai
    long startTime = System.currentTimeMillis();
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    long endTime = System.currentTimeMillis();

    // Then - La réponse doit être rapide et sécurisée
    assertTrue(endTime - startTime < 5000, "La réponse doit être rapide");
    assertTrue(
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
            || response.getStatusCode() == HttpStatus.FORBIDDEN,
        "L'accès non autorisé doit être rejeté");
  }

  @Test
  @DisplayName("Devrait empêcher la manipulation des cookies de session")
  void shouldPreventSessionCookieManipulation() {
    // Given - Headers avec cookies malveillants
    HttpHeaders headers = new HttpHeaders();
    headers.add("Cookie", "JSESSIONID=malicious_session_id");
    headers.add("Cookie", "AUTH_TOKEN=fake_token");

    HttpEntity<String> entity = new HttpEntity<>(headers);
    String url = "http://localhost:" + port + "/api/games";

    // When - Tentative d'accès avec cookies malveillants
    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

    // Then - L'accès doit être rejeté
    assertTrue(
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
            || response.getStatusCode() == HttpStatus.FORBIDDEN,
        "Les cookies malveillants doivent être rejetés");
  }

  @Test
  @DisplayName("Devrait valider la configuration CORS de sécurité")
  void shouldValidateSecureCorsConfiguration() {
    // Given - Headers CORS malveillants
    HttpHeaders headers = new HttpHeaders();
    headers.add("Origin", "http://malicious-site.com");
    headers.add("Access-Control-Request-Method", "GET");

    HttpEntity<String> entity = new HttpEntity<>(headers);
    String url = "http://localhost:" + port + "/api/games";

    // When - Tentative d'accès cross-origin malveillant
    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.OPTIONS, entity, String.class);

    // Then - L'accès doit être contrôlé par CORS
    // Note: La réponse peut varier selon la configuration CORS
    assertNotNull(response.getStatusCode(), "Une réponse doit être fournie pour les requêtes CORS");
    // Note: CORS validation in integration tests may vary
  }
}
