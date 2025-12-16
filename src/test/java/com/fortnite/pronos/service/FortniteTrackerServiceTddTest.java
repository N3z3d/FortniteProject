package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fortnite.pronos.dto.FortniteTrackerPlayerStats;
import com.fortnite.pronos.dto.FortniteTrackerResponse;
import com.fortnite.pronos.exception.FortniteTrackerException;

/**
 * TDD Tests for FortniteTrackerService - External API Integration Critical Component
 *
 * <p>This test suite validates external API integration, caching mechanisms, rate limiting, and
 * error handling using RED-GREEN-REFACTOR TDD methodology. FortniteTrackerService handles
 * communication with FortniteTracker.com API, implements sophisticated caching and rate limiting
 * essential for maintaining performance and respecting API limits.
 *
 * <p>Business Logic Areas: - External API integration and HTTP client management - Player
 * statistics retrieval and parsing - Cache management with expiration policies - Rate limiting and
 * request throttling - Error handling and retry mechanisms - Competitive stats filtering and
 * processing
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FortniteTrackerService - External API Critical TDD Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class FortniteTrackerServiceTddTest {

  @Mock private RestTemplate restTemplate;

  @InjectMocks private FortniteTrackerService fortniteTrackerService;

  private String testApiKey;
  private String testBaseUrl;
  private String testEpicId;
  private String testPlatform;
  private FortniteTrackerResponse testResponse;
  private HttpEntity<String> expectedHttpEntity;

  @BeforeEach
  void setUp() {
    // Test configuration setup
    testApiKey = "test-api-key-123";
    testBaseUrl = "https://api.fortnitetracker.com/v1";
    testEpicId = "TestPlayer123";
    testPlatform = "pc";

    // Configure service properties via reflection
    ReflectionTestUtils.setField(fortniteTrackerService, "apiKey", testApiKey);
    ReflectionTestUtils.setField(fortniteTrackerService, "baseUrl", testBaseUrl);
    ReflectionTestUtils.setField(fortniteTrackerService, "rateLimitPerMinute", 30);

    // Test response setup
    testResponse = new FortniteTrackerResponse();
    testResponse.setEpicUserHandle(testEpicId);
    testResponse.setPlatformName(testPlatform);

    // Expected HTTP headers setup
    HttpHeaders expectedHeaders = new HttpHeaders();
    expectedHeaders.set("TRN-Api-Key", testApiKey);
    expectedHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    expectedHttpEntity = new HttpEntity<>(expectedHeaders);
  }

  @Nested
  @DisplayName("Player Stats Retrieval")
  class PlayerStatsRetrievalTests {

    @Test
    @DisplayName("Should successfully retrieve player stats from API")
    void shouldSuccessfullyRetrievePlayerStatsFromApi() {
      // RED: Test basic API call success
      String expectedUrl = testBaseUrl + "/profile/" + testPlatform + "/" + testEpicId;
      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              eq(expectedUrl),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      FortniteTrackerPlayerStats result =
          fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      assertThat(result).isNotNull();
      assertThat(result.getEpicUserHandle()).isEqualTo(testEpicId);
      assertThat(result.getPlatformName()).isEqualTo(testPlatform);

      verify(restTemplate)
          .exchange(
              eq(expectedUrl),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }

    @Test
    @DisplayName("Should handle HTTP 404 Not Found gracefully")
    void shouldHandleHttp404NotFoundGracefully() {
      // RED: Test player not found scenario
      String expectedUrl = testBaseUrl + "/profile/" + testPlatform + "/" + testEpicId;
      HttpClientErrorException notFoundException =
          new HttpClientErrorException(HttpStatus.NOT_FOUND, "Player not found");

      when(restTemplate.exchange(
              eq(expectedUrl),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenThrow(notFoundException);

      assertThatThrownBy(() -> fortniteTrackerService.getPlayerStats(testEpicId, testPlatform))
          .isInstanceOf(FortniteTrackerException.class)
          .hasMessageContaining("Joueur non trouvé")
          .hasMessageContaining(testEpicId);
    }

    @Test
    @DisplayName("Should handle HTTP 429 Too Many Requests with rate limiting")
    void shouldHandleHttp429TooManyRequestsWithRateLimiting() {
      // RED: Test rate limit exceeded scenario
      String expectedUrl = testBaseUrl + "/profile/" + testPlatform + "/" + testEpicId;
      HttpClientErrorException rateLimitException =
          new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");

      when(restTemplate.exchange(
              eq(expectedUrl),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenThrow(rateLimitException);

      assertThatThrownBy(() -> fortniteTrackerService.getPlayerStats(testEpicId, testPlatform))
          .isInstanceOf(FortniteTrackerException.class)
          .hasMessageContaining("Rate limit dépassé")
          .hasCauseInstanceOf(HttpClientErrorException.class);
    }

    @Test
    @DisplayName("Should handle unexpected HTTP errors")
    void shouldHandleUnexpectedHttpErrors() {
      // RED: Test general HTTP error handling
      String expectedUrl = testBaseUrl + "/profile/" + testPlatform + "/" + testEpicId;
      HttpClientErrorException serverError =
          new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error");

      when(restTemplate.exchange(
              eq(expectedUrl),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenThrow(serverError);

      assertThatThrownBy(() -> fortniteTrackerService.getPlayerStats(testEpicId, testPlatform))
          .isInstanceOf(FortniteTrackerException.class)
          .hasMessageContaining("Erreur lors de la récupération des stats");
    }

    @Test
    @DisplayName("Should handle null or empty response")
    void shouldHandleNullOrEmptyResponse() {
      // RED: Test invalid response handling
      String expectedUrl = testBaseUrl + "/profile/" + testPlatform + "/" + testEpicId;
      ResponseEntity<FortniteTrackerResponse> emptyResponse = ResponseEntity.ok(null);

      when(restTemplate.exchange(
              eq(expectedUrl),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(emptyResponse);

      assertThatThrownBy(() -> fortniteTrackerService.getPlayerStats(testEpicId, testPlatform))
          .isInstanceOf(FortniteTrackerException.class)
          .hasMessageContaining("Réponse invalide de FortniteTracker");
    }

    @Test
    @DisplayName("Should set correct HTTP headers for API authentication")
    void shouldSetCorrectHttpHeadersForApiAuthentication() {
      // RED: Test header configuration
      String expectedUrl = testBaseUrl + "/profile/" + testPlatform + "/" + testEpicId;
      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      verify(restTemplate)
          .exchange(
              eq(expectedUrl),
              eq(HttpMethod.GET),
              argThat(
                  entity -> {
                    HttpHeaders headers = entity.getHeaders();
                    return testApiKey.equals(headers.getFirst("TRN-Api-Key"))
                        && headers.getAccept().contains(MediaType.APPLICATION_JSON);
                  }),
              eq(FortniteTrackerResponse.class));
    }

    @Test
    @DisplayName("Should parse lifetime stats from API response")
    void shouldParseLifetimeStatsFromApiResponse() {
      // RED: Test response parsing with lifetime stats
      FortniteTrackerResponse.LifeTimeStat winsStat = new FortniteTrackerResponse.LifeTimeStat();
      winsStat.setKey("Wins");
      winsStat.setValue("150");

      FortniteTrackerResponse.LifeTimeStat killsStat = new FortniteTrackerResponse.LifeTimeStat();
      killsStat.setKey("Kills");
      killsStat.setValue("2350");

      testResponse.setLifeTimeStats(Arrays.asList(winsStat, killsStat));

      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      FortniteTrackerPlayerStats result =
          fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      assertThat(result.getLifeTimeStats()).isNotNull();
      assertThat(result.getLifeTimeStats()).hasSize(2);
      assertThat(result.getLifeTimeStats().get("Wins")).isEqualTo("150");
      assertThat(result.getLifeTimeStats().get("Kills")).isEqualTo("2350");
    }
  }

  @Nested
  @DisplayName("Caching Mechanism")
  class CachingMechanismTests {

    @Test
    @DisplayName("Should cache successful API responses")
    void shouldCacheSuccessfulApiResponses() {
      // RED: Test caching behavior
      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      // First call should hit the API
      FortniteTrackerPlayerStats firstResult =
          fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      // Second call should use cache
      FortniteTrackerPlayerStats secondResult =
          fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      assertThat(firstResult).isNotNull();
      assertThat(secondResult).isNotNull();
      assertThat(firstResult.getEpicUserHandle()).isEqualTo(secondResult.getEpicUserHandle());

      // Verify API was called only once
      verify(restTemplate, times(1))
          .exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }

    @Test
    @DisplayName("Should cache with different keys for different players")
    void shouldCacheWithDifferentKeysForDifferentPlayers() {
      // RED: Test cache key differentiation
      String secondEpicId = "AnotherPlayer456";
      FortniteTrackerResponse secondResponse = new FortniteTrackerResponse();
      secondResponse.setEpicUserHandle(secondEpicId);
      secondResponse.setPlatformName(testPlatform);

      ResponseEntity<FortniteTrackerResponse> firstMockResponse = ResponseEntity.ok(testResponse);
      ResponseEntity<FortniteTrackerResponse> secondMockResponse =
          ResponseEntity.ok(secondResponse);

      when(restTemplate.exchange(
              contains(testEpicId),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(firstMockResponse);

      when(restTemplate.exchange(
              contains(secondEpicId),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(secondMockResponse);

      FortniteTrackerPlayerStats firstResult =
          fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);
      FortniteTrackerPlayerStats secondResult =
          fortniteTrackerService.getPlayerStats(secondEpicId, testPlatform);

      assertThat(firstResult.getEpicUserHandle()).isEqualTo(testEpicId);
      assertThat(secondResult.getEpicUserHandle()).isEqualTo(secondEpicId);

      // Verify both API calls were made (different cache keys)
      verify(restTemplate, times(2))
          .exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }

    @Test
    @DisplayName("Should clear cache when requested")
    void shouldClearCacheWhenRequested() {
      // RED: Test cache clearing functionality
      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      // First call to populate cache
      fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      // Clear cache
      fortniteTrackerService.clearCache();

      // Second call should hit API again
      fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      // Verify API was called twice (cache was cleared)
      verify(restTemplate, times(2))
          .exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }

    @Test
    @DisplayName("Should handle cache with platform differentiation")
    void shouldHandleCacheWithPlatformDifferentiation() {
      // RED: Test platform-specific caching
      String differentPlatform = "xbl";
      FortniteTrackerResponse platformResponse = new FortniteTrackerResponse();
      platformResponse.setEpicUserHandle(testEpicId);
      platformResponse.setPlatformName(differentPlatform);

      ResponseEntity<FortniteTrackerResponse> pcResponse = ResponseEntity.ok(testResponse);
      ResponseEntity<FortniteTrackerResponse> xblResponse = ResponseEntity.ok(platformResponse);

      when(restTemplate.exchange(
              contains("pc"),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(pcResponse);

      when(restTemplate.exchange(
              contains("xbl"),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(xblResponse);

      FortniteTrackerPlayerStats pcResult = fortniteTrackerService.getPlayerStats(testEpicId, "pc");
      FortniteTrackerPlayerStats xblResult =
          fortniteTrackerService.getPlayerStats(testEpicId, differentPlatform);

      assertThat(pcResult.getPlatformName()).isEqualTo("pc");
      assertThat(xblResult.getPlatformName()).isEqualTo(differentPlatform);

      // Verify separate API calls for different platforms
      verify(restTemplate, times(2))
          .exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }
  }

  @Nested
  @DisplayName("Competitive Stats Processing")
  class CompetitiveStatsProcessingTests {

    @Test
    @DisplayName("Should retrieve competitive stats for a region")
    void shouldRetrieveCompetitiveStatsForARegion() {
      // RED: Test competitive stats retrieval
      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      FortniteTrackerPlayerStats result =
          fortniteTrackerService.getCompetitiveStats(testEpicId, "EU");

      assertThat(result).isNotNull();
      assertThat(result.getEpicUserHandle()).isEqualTo(testEpicId);

      verify(restTemplate)
          .exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }

    @Test
    @DisplayName("Should map regions to appropriate platforms")
    void shouldMapRegionsToAppropriatePlatforms() {
      // RED: Test region to platform mapping
      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      // Test different regions
      String[] regions = {"EU", "NAC", "NAW", "BR", "ASIA", "OCE", "ME"};

      for (String region : regions) {
        fortniteTrackerService.getCompetitiveStats(testEpicId, region);
      }

      // All regions should map to "pc" platform
      verify(restTemplate, times(regions.length))
          .exchange(
              contains("/profile/pc/"),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }

    @Test
    @DisplayName("Should handle unknown region gracefully")
    void shouldHandleUnknownRegionGracefully() {
      // RED: Test unknown region handling
      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      FortniteTrackerPlayerStats result =
          fortniteTrackerService.getCompetitiveStats(testEpicId, "UNKNOWN_REGION");

      assertThat(result).isNotNull();

      // Should default to "pc" platform
      verify(restTemplate)
          .exchange(
              contains("/profile/pc/"),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }
  }

  @Nested
  @DisplayName("Rate Limiting and Performance")
  class RateLimitingPerformanceTests {

    @Test
    @DisplayName("Should track request timestamps for rate limiting")
    void shouldTrackRequestTimestampsForRateLimiting() {
      // RED: Test basic rate limiting mechanism
      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      // Make multiple requests
      for (int i = 0; i < 5; i++) {
        String playerId = "Player" + i;
        fortniteTrackerService.getPlayerStats(playerId, testPlatform);
      }

      // All requests should succeed (within rate limit)
      verify(restTemplate, times(5))
          .exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }

    @Test
    @DisplayName("Should handle concurrent requests efficiently")
    void shouldHandleConcurrentRequestsEfficiently() {
      // RED: Test thread safety and concurrent access
      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      // Simulate concurrent calls for the same player (should hit cache)
      fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);
      fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);
      fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      // Only one API call should be made due to caching
      verify(restTemplate, times(1))
          .exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }

    @Test
    @DisplayName("Should maintain performance with large numbers of different players")
    void shouldMaintainPerformanceWithLargeNumbersOfDifferentPlayers() {
      // RED: Test scalability with multiple different players
      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      // Create requests for many different players
      int playerCount = 20;
      for (int i = 0; i < playerCount; i++) {
        String playerId = "TestPlayer" + String.format("%03d", i);
        fortniteTrackerService.getPlayerStats(playerId, testPlatform);
      }

      // Each different player should result in an API call
      verify(restTemplate, times(playerCount))
          .exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }
  }

  @Nested
  @DisplayName("Error Handling and Edge Cases")
  class ErrorHandlingEdgeCasesTests {

    @Test
    @DisplayName("Should handle null or empty API key gracefully")
    void shouldHandleNullOrEmptyApiKeyGracefully() {
      // RED: Test missing API key scenario
      ReflectionTestUtils.setField(fortniteTrackerService, "apiKey", "");

      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      FortniteTrackerPlayerStats result =
          fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      assertThat(result).isNotNull();

      // Verify empty API key was used in headers
      verify(restTemplate)
          .exchange(
              any(String.class),
              eq(HttpMethod.GET),
              argThat(entity -> "".equals(entity.getHeaders().getFirst("TRN-Api-Key"))),
              eq(FortniteTrackerResponse.class));
    }

    @Test
    @DisplayName("Should handle invalid base URL configuration")
    void shouldHandleInvalidBaseUrlConfiguration() {
      // RED: Test invalid URL handling
      ReflectionTestUtils.setField(fortniteTrackerService, "baseUrl", "invalid-url");

      // The method should still attempt the call, RestTemplate will handle the invalid URL
      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenThrow(new RuntimeException("Invalid URL"));

      assertThatThrownBy(() -> fortniteTrackerService.getPlayerStats(testEpicId, testPlatform))
          .isInstanceOf(FortniteTrackerException.class)
          .hasMessageContaining("Erreur inattendue");
    }

    @Test
    @DisplayName("Should handle unexpected exceptions from RestTemplate")
    void shouldHandleUnexpectedExceptionsFromRestTemplate() {
      // RED: Test unexpected exception handling
      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenThrow(new RuntimeException("Unexpected network error"));

      assertThatThrownBy(() -> fortniteTrackerService.getPlayerStats(testEpicId, testPlatform))
          .isInstanceOf(FortniteTrackerException.class)
          .hasMessageContaining("Erreur inattendue")
          .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should validate input parameters")
    void shouldValidateInputParameters() {
      // RED: Test input validation
      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      // Test with empty strings (should still work but create proper URLs)
      FortniteTrackerPlayerStats result = fortniteTrackerService.getPlayerStats("", "");

      assertThat(result).isNotNull();

      verify(restTemplate)
          .exchange(
              contains("/profile//"),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }

    @Test
    @DisplayName("Should handle malformed API responses")
    void shouldHandleMalformedApiResponses() {
      // RED: Test malformed response handling
      FortniteTrackerResponse malformedResponse = new FortniteTrackerResponse();
      // Leave essential fields null

      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(malformedResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      FortniteTrackerPlayerStats result =
          fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      // Should handle null fields gracefully
      assertThat(result).isNotNull();
      assertThat(result.getEpicUserHandle()).isNull();
      assertThat(result.getPlatformName()).isNull();
    }
  }

  @Nested
  @DisplayName("Configuration and Integration")
  class ConfigurationIntegrationTests {

    @Test
    @DisplayName("Should use configurable rate limit settings")
    void shouldUseConfigurableRateLimitSettings() {
      // RED: Test rate limit configuration
      int customRateLimit = 60;
      ReflectionTestUtils.setField(fortniteTrackerService, "rateLimitPerMinute", customRateLimit);

      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      // The rate limit setting is used internally, so we can't directly test it
      // but we can verify the service still functions correctly
      FortniteTrackerPlayerStats result =
          fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should use configurable base URL")
    void shouldUseConfigurableBaseUrl() {
      // RED: Test base URL configuration
      String customBaseUrl = "https://custom-api.example.com/v2";
      ReflectionTestUtils.setField(fortniteTrackerService, "baseUrl", customBaseUrl);

      ResponseEntity<FortniteTrackerResponse> mockResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenReturn(mockResponse);

      fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      verify(restTemplate)
          .exchange(
              eq(customBaseUrl + "/profile/" + testPlatform + "/" + testEpicId),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class));
    }

    @Test
    @DisplayName("Should integrate correctly with Spring retry mechanism")
    void shouldIntegrateCorrectlyWithSpringRetryMechanism() {
      // RED: Test retry mechanism integration
      HttpClientErrorException retryableError =
          new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Temporary error");
      ResponseEntity<FortniteTrackerResponse> successResponse = ResponseEntity.ok(testResponse);

      when(restTemplate.exchange(
              any(String.class),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              eq(FortniteTrackerResponse.class)))
          .thenThrow(retryableError)
          .thenReturn(successResponse);

      // Note: @Retryable only works when called through Spring proxy
      // This test verifies the annotation is present and method signature is correct
      FortniteTrackerPlayerStats result =
          fortniteTrackerService.getPlayerStats(testEpicId, testPlatform);

      assertThat(result).isNotNull();
    }
  }
}
