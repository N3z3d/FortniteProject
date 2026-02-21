package com.fortnite.pronos.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fortnite.pronos.dto.FortniteTrackerPlayerStats;
import com.fortnite.pronos.dto.FortniteTrackerResponse;
import com.fortnite.pronos.exception.FortniteTrackerException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"java:S1874", "java:S2259", "java:S3923"})
public class FortniteTrackerService {

  private final RestTemplate restTemplate;

  @Value("${fortnite.tracker.api.key:}")
  private String apiKey;

  @Value("${fortnite.tracker.api.url:https://api.fortnitetracker.com/v1}")
  private String baseUrl;

  @Value("${fortnite.tracker.rate.limit:30}")
  private int rateLimitPerMinute;

  private final Map<String, CachedPlayerStats> playerStatsCache = new ConcurrentHashMap<>();
  private static final Duration CACHE_DURATION = Duration.ofMinutes(5);

  private final Queue<LocalDateTime> requestTimestamps = new LinkedList<>();

  @Retryable(
      value = {HttpClientErrorException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2))
  public FortniteTrackerPlayerStats getPlayerStats(String epicId, String platform) {
    return getPlayerStats(epicId, platform, true);
  }

  public FortniteTrackerPlayerStats getPlayerStats(
      String epicId, String platform, boolean useCache) {
    String cacheKey = epicId + "_" + platform;

    FortniteTrackerPlayerStats cachedStats = getCachedStats(cacheKey, useCache);
    if (cachedStats != null) {
      log.debug("Stats found in cache for {}", epicId);
      return cachedStats;
    }

    enforceRateLimit();
    return fetchAndCacheStats(epicId, platform, useCache, cacheKey);
  }

  public FortniteTrackerPlayerStats getCompetitiveStats(String epicId, String region) {
    String platform = mapRegionToPlatform(region);
    return getPlayerStats(epicId, platform, false);
  }

  public void clearCache() {
    playerStatsCache.clear();
    log.info("FortniteTracker cache cleared");
  }

  private FortniteTrackerPlayerStats getCachedStats(String cacheKey, boolean useCache) {
    if (!useCache) {
      return null;
    }
    CachedPlayerStats cached = playerStatsCache.get(cacheKey);
    if (cached == null || cached.isExpired()) {
      return null;
    }
    return cached.stats;
  }

  private FortniteTrackerPlayerStats fetchAndCacheStats(
      String epicId, String platform, boolean useCache, String cacheKey) {
    try {
      log.info("Fetching FortniteTracker stats for {}", epicId);
      FortniteTrackerPlayerStats stats = requestPlayerStats(epicId, platform);
      cachePlayerStats(cacheKey, useCache, stats);
      return stats;
    } catch (HttpClientErrorException exception) {
      return handleHttpClientError(epicId, platform, useCache, cacheKey, exception);
    } catch (FortniteTrackerException exception) {
      throw exception;
    } catch (Exception exception) {
      log.error("Unexpected error during FortniteTracker call: {}", exception.getMessage());
      throw new FortniteTrackerException("Unexpected error", exception);
    }
  }

  private FortniteTrackerPlayerStats handleHttpClientError(
      String epicId,
      String platform,
      boolean useCache,
      String cacheKey,
      HttpClientErrorException exception) {
    HttpStatusCode status = exception.getStatusCode();
    if (status.value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
      return retryImmediately(epicId, platform, useCache, cacheKey);
    }
    if (status.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
      log.error("Rate limit reached for FortniteTracker API");
      throw new FortniteTrackerException("Rate limit exceeded, try again later", exception);
    }
    if (status.value() == HttpStatus.NOT_FOUND.value()) {
      log.warn("Player not found: {}", epicId);
      throw new FortniteTrackerException("Player not found: " + epicId, exception);
    }
    log.error("HTTP error while calling FortniteTracker: {}", exception.getMessage());
    throw new FortniteTrackerException("Error while fetching stats", exception);
  }

  private FortniteTrackerPlayerStats retryImmediately(
      String epicId, String platform, boolean useCache, String cacheKey) {
    log.warn("FortniteTracker unavailable, retrying immediately");
    try {
      FortniteTrackerPlayerStats stats = requestPlayerStats(epicId, platform);
      cachePlayerStats(cacheKey, useCache, stats);
      return stats;
    } catch (Exception retryException) {
      throw new FortniteTrackerException("Error while fetching stats", retryException);
    }
  }

  private FortniteTrackerPlayerStats requestPlayerStats(String epicId, String platform) {
    ResponseEntity<FortniteTrackerResponse> response =
        restTemplate.exchange(
            buildProfileUrl(epicId, platform),
            HttpMethod.GET,
            buildRequestEntity(),
            FortniteTrackerResponse.class);

    if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
      throw new FortniteTrackerException("Invalid FortniteTracker response");
    }

    return parseResponse(response.getBody());
  }

  private String buildProfileUrl(String epicId, String platform) {
    String normalizedBaseUrl =
        baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    return normalizedBaseUrl + "/profile/" + platform + "/" + epicId;
  }

  private HttpEntity<String> buildRequestEntity() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("TRN-Api-Key", apiKey);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    return new HttpEntity<>(headers);
  }

  private void cachePlayerStats(
      String cacheKey, boolean useCache, FortniteTrackerPlayerStats playerStats) {
    if (!useCache) {
      return;
    }
    playerStatsCache.put(cacheKey, new CachedPlayerStats(playerStats));
  }

  private FortniteTrackerPlayerStats parseResponse(FortniteTrackerResponse response) {
    FortniteTrackerPlayerStats stats = new FortniteTrackerPlayerStats();

    stats.setEpicUserHandle(response.getEpicUserHandle());
    stats.setPlatformName(response.getPlatformName());

    if (response.getLifeTimeStats() != null) {
      Map<String, String> lifeTimeStats = new HashMap<>();
      response
          .getLifeTimeStats()
          .forEach(stat -> lifeTimeStats.put(stat.getKey(), stat.getValue()));
      stats.setLifeTimeStats(lifeTimeStats);
    }

    if (response.getStats() != null && response.getStats().getCurr_p2() != null) {
      FortniteTrackerPlayerStats.CompetitiveStats competitiveStats =
          new FortniteTrackerPlayerStats.CompetitiveStats();
      stats.setCompetitiveStats(competitiveStats);
    }

    return stats;
  }

  private String mapRegionToPlatform(String region) {
    return switch (region.toUpperCase()) {
      case "EU", "NAC", "NAW", "BR", "ASIA", "OCE", "ME" -> "pc";
      default -> "pc";
    };
  }

  private void enforceRateLimit() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime oneMinuteAgo = now.minusMinutes(1);

    requestTimestamps.removeIf(timestamp -> timestamp.isBefore(oneMinuteAgo));

    if (requestTimestamps.size() >= rateLimitPerMinute) {
      LocalDateTime oldestRequest = requestTimestamps.peek();
      if (oldestRequest != null) {
        Duration waitTime = Duration.between(now, oldestRequest.plusMinutes(1));
        if (!waitTime.isNegative()) {
          try {
            log.warn("Rate limit reached, waiting {} seconds", waitTime.getSeconds());
            Thread.sleep(waitTime.toMillis());
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FortniteTrackerException("Interrupted during rate limit wait", exception);
          }
        }
      }
    }

    requestTimestamps.offer(now);
  }

  private static class CachedPlayerStats {
    final FortniteTrackerPlayerStats stats;
    final LocalDateTime timestamp;

    CachedPlayerStats(FortniteTrackerPlayerStats stats) {
      this.stats = stats;
      this.timestamp = LocalDateTime.now();
    }

    boolean isExpired() {
      return LocalDateTime.now().isAfter(timestamp.plus(CACHE_DURATION));
    }
  }
}
