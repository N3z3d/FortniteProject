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

    if (useCache) {
      CachedPlayerStats cached = playerStatsCache.get(cacheKey);
      if (cached != null && !cached.isExpired()) {
        log.debug("Statistiques trouvées dans le cache pour {}", epicId);
        return cached.stats;
      }
    }

    enforceRateLimit();

    try {
      log.info("Récupération des statistiques FortniteTracker pour {}", epicId);

      String normalizedBaseUrl =
          baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
      String url = normalizedBaseUrl + "/profile/" + platform + "/" + epicId;

      HttpHeaders headers = new HttpHeaders();
      headers.set("TRN-Api-Key", apiKey);
      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

      HttpEntity<String> entity = new HttpEntity<>(headers);

      ResponseEntity<FortniteTrackerResponse> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, FortniteTrackerResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        FortniteTrackerPlayerStats stats = parseResponse(response.getBody());

        if (useCache) {
          playerStatsCache.put(cacheKey, new CachedPlayerStats(stats));
        }

        return stats;
      }

      throw new FortniteTrackerException("Réponse invalide de FortniteTracker");

    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
        log.warn("Service FortniteTracker indisponible, nouvelle tentative immédiate");
        try {
          String normalizedBaseUrl =
              baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
          String retryUrl = normalizedBaseUrl + "/profile/" + platform + "/" + epicId;
          HttpHeaders headers = new HttpHeaders();
          headers.set("TRN-Api-Key", apiKey);
          headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
          HttpEntity<String> entity = new HttpEntity<>(headers);

          ResponseEntity<FortniteTrackerResponse> retryResponse =
              restTemplate.exchange(
                  retryUrl, HttpMethod.GET, entity, FortniteTrackerResponse.class);

          if (retryResponse.getStatusCode() == HttpStatus.OK && retryResponse.getBody() != null) {
            FortniteTrackerPlayerStats stats = parseResponse(retryResponse.getBody());
            if (useCache) {
              playerStatsCache.put(cacheKey, new CachedPlayerStats(stats));
            }
            return stats;
          }
          throw new FortniteTrackerException("Réponse invalide de FortniteTracker après retry", e);
        } catch (Exception retryEx) {
          throw new FortniteTrackerException("Erreur lors de la récupération des stats", retryEx);
        }
      } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
        log.error("Rate limit atteint pour FortniteTracker API");
        throw new FortniteTrackerException("Rate limit dépassé, réessayez plus tard", e);
      } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        log.warn("Joueur non trouvé : {}", epicId);
        throw new FortniteTrackerException("Joueur non trouvé : " + epicId, e);
      } else {
        log.error("Erreur HTTP lors de l'appel à FortniteTracker : {}", e.getMessage());
        throw new FortniteTrackerException("Erreur lors de la récupération des stats", e);
      }
    } catch (FortniteTrackerException e) {
      throw e;
    } catch (Exception e) {
      log.error("Erreur inattendue lors de l'appel à FortniteTracker : {}", e.getMessage());
      throw new FortniteTrackerException("Erreur inattendue", e);
    }
  }

  public FortniteTrackerPlayerStats getCompetitiveStats(String epicId, String region) {
    String platform = mapRegionToPlatform(region);
    FortniteTrackerPlayerStats stats = getPlayerStats(epicId, platform, false);

    if (stats != null && stats.getCompetitiveStats() != null) {
      return stats;
    }

    return stats;
  }

  private FortniteTrackerPlayerStats parseResponse(FortniteTrackerResponse response) {
    FortniteTrackerPlayerStats stats = new FortniteTrackerPlayerStats();

    stats.setEpicUserHandle(response.getEpicUserHandle());
    stats.setPlatformName(response.getPlatformName());

    if (response.getLifeTimeStats() != null) {
      Map<String, String> lifeTimeStats = new HashMap<>();
      response
          .getLifeTimeStats()
          .forEach(
              stat -> {
                lifeTimeStats.put(stat.getKey(), stat.getValue());
              });
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
            log.warn("Rate limit atteint, attente de {} secondes", waitTime.getSeconds());
            Thread.sleep(waitTime.toMillis());
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FortniteTrackerException("Interruption pendant l'attente du rate limit", e);
          }
        }
      }
    }

    requestTimestamps.offer(now);
  }

  public void clearCache() {
    playerStatsCache.clear();
    log.info("Cache FortniteTracker vidé");
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
