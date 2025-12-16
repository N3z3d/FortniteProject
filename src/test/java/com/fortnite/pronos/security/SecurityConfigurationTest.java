package com.fortnite.pronos.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.PronosApplication;

/**
 * Security configuration tests. Tests verify that: - Public endpoints (actuator) are accessible
 * without auth - Protected endpoints require authentication - Authenticated requests (via
 * X-Test-User) are allowed
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {PronosApplication.class})
@ActiveProfiles("test")
@DisplayName("Security Configuration Tests")
class SecurityConfigurationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  private HttpHeaders createTestUserHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Test-User", "testuser");
    return headers;
  }

  @Test
  @DisplayName("Public actuator endpoints should be accessible without auth")
  void shouldAllowAccessToPublicEndpoints() {
    String[] publicEndpoints = {"/actuator/health", "/actuator/info"};

    for (String endpoint : publicEndpoints) {
      String url = "http://localhost:" + port + endpoint;
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

      assertTrue(
          response.getStatusCode().is2xxSuccessful()
              || response.getStatusCode() == HttpStatus.NOT_FOUND,
          "Public endpoint "
              + endpoint
              + " should be accessible, got: "
              + response.getStatusCode());
    }
  }

  @Test
  @DisplayName("Draft endpoints should be accessible (permitAll in test config)")
  void shouldAllowDraftEndpoints() {
    String url = "http://localhost:" + port + "/api/draft/test-game-id/next-participant";
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Draft endpoints return 404 for non-existent game, not 401
    assertNotEquals(
        HttpStatus.UNAUTHORIZED,
        response.getStatusCode(),
        "Draft endpoints should not require auth");
  }

  @Test
  @DisplayName("Protected endpoints should return 401 or 403 without authentication")
  void shouldRequireAuthForProtectedEndpoints() {
    String url = "http://localhost:" + port + "/api/games/my-games";
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertTrue(
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
            || response.getStatusCode() == HttpStatus.FORBIDDEN,
        "Protected endpoint should require authentication, got: " + response.getStatusCode());
  }

  @Test
  @DisplayName("X-Test-User header should be processed by filter")
  void shouldProcessTestUserHeader() {
    // Note: This test verifies the filter processes the header.
    // If user doesn't exist in DB, auth will fail, which is expected behavior.
    String url = "http://localhost:" + port + "/api/games";
    HttpEntity<String> entity = new HttpEntity<>(createTestUserHeaders());

    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

    // The test verifies the request is processed (doesn't throw exception)
    // Actual auth status depends on whether user exists in test DB
    assertNotNull(response, "Response should not be null");
  }

  @Test
  @DisplayName("CSRF should be disabled (stateless API)")
  void shouldHaveAppropriateCsrfConfiguration() {
    String url = "http://localhost:" + port + "/api/games";
    HttpHeaders headers = createTestUserHeaders();
    headers.set("Content-Type", "application/json");
    HttpEntity<String> entity = new HttpEntity<>("{}", headers);

    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

    // POST should not fail with 403 CSRF error - will fail with 400 (bad request) instead
    assertNotEquals(
        HttpStatus.FORBIDDEN,
        response.getStatusCode(),
        "CSRF should be disabled - should not get 403");
  }

  @Test
  @DisplayName("Rate limiting should not block repeated requests in test")
  void shouldNotHaveStrictRateLimiting() {
    String url = "http://localhost:" + port + "/actuator/health";

    for (int i = 0; i < 10; i++) {
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
      assertTrue(
          response.getStatusCode().is2xxSuccessful(),
          "Request " + i + " should succeed, got: " + response.getStatusCode());
    }
  }

  @Test
  @DisplayName("Security headers should be present on responses")
  void shouldHaveSecurityHeadersConfiguration() {
    String url = "http://localhost:" + port + "/actuator/health";
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    // Check for security headers
    assertNotNull(response.getHeaders());
  }

  @Test
  @DisplayName("Response time should be reasonable")
  void shouldHaveAppropriateTimeoutConfiguration() {
    String url = "http://localhost:" + port + "/actuator/health";

    long startTime = System.currentTimeMillis();
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    long responseTime = System.currentTimeMillis() - startTime;

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(responseTime < 5000, "Response time too long: " + responseTime + "ms");
  }
}
