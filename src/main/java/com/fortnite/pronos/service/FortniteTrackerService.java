package com.fortnite.pronos.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

  // Cache simple pour éviter les appels excessifs
  private final Map<String, CachedPlayerStats> playerStatsCache = new ConcurrentHashMap<>();
  private static final Duration CACHE_DURATION = Duration.ofMinutes(5);

  // Rate limiting simple
  private final Queue<LocalDateTime> requestTimestamps = new LinkedList<>();

  /**
   * Récupère les statistiques d'un joueur depuis FortniteTracker Utilise un cache et gère le rate
   * limiting
   */
  @Retryable(
      value = {HttpClientErrorException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2))
  public FortniteTrackerPlayerStats getPlayerStats(String epicId, String platform) {
    String cacheKey = epicId + "_" + platform;

    // Vérifier le cache
    CachedPlayerStats cached = playerStatsCache.get(cacheKey);
    if (cached != null && !cached.isExpired()) {
      log.debug("Statistiques trouvées dans le cache pour {}", epicId);
      return cached.stats;
    }

    // Rate limiting
    enforceRateLimit();

    try {
      log.info("Récupération des statistiques FortniteTracker pour {}", epicId);

      String url =
          UriComponentsBuilder.fromHttpUrl(baseUrl)
              .pathSegment("profile", platform, epicId)
              .toUriString();

      HttpHeaders headers = new HttpHeaders();
      headers.set("TRN-Api-Key", apiKey);
      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

      HttpEntity<String> entity = new HttpEntity<>(headers);

      ResponseEntity<FortniteTrackerResponse> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, FortniteTrackerResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        FortniteTrackerPlayerStats stats = parseResponse(response.getBody());

        // Mettre en cache
        playerStatsCache.put(cacheKey, new CachedPlayerStats(stats));

        return stats;
      } else {
        throw new FortniteTrackerException("Réponse invalide de FortniteTracker");
      }

    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
        log.error("Rate limit atteint pour FortniteTracker API");
        throw new FortniteTrackerException("Rate limit dépassé, réessayez plus tard", e);
      } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        log.warn("Joueur non trouvé : {}", epicId);
        throw new FortniteTrackerException("Joueur non trouvé : " + epicId, e);
      } else {
        log.error("Erreur HTTP lors de l'appel à FortniteTracker : {}", e.getMessage());
        throw new FortniteTrackerException("Erreur lors de la récupération des stats", e);
      }
    } catch (Exception e) {
      log.error("Erreur inattendue lors de l'appel à FortniteTracker : {}", e.getMessage());
      throw new FortniteTrackerException("Erreur inattendue", e);
    }
  }

  /** Récupère les statistiques de tournoi/compétition pour un joueur */
  public FortniteTrackerPlayerStats getCompetitiveStats(String epicId, String region) {
    // Pour l'instant, on utilise la même méthode mais on pourrait
    // filtrer spécifiquement les stats compétitives
    String platform = mapRegionToPlatform(region);
    FortniteTrackerPlayerStats stats = getPlayerStats(epicId, platform);

    // Filtrer uniquement les stats compétitives si disponibles
    if (stats != null && stats.getCompetitiveStats() != null) {
      return stats;
    }

    return stats;
  }

  /** Parse la réponse de FortniteTracker */
  private FortniteTrackerPlayerStats parseResponse(FortniteTrackerResponse response) {
    FortniteTrackerPlayerStats stats = new FortniteTrackerPlayerStats();

    stats.setEpicUserHandle(response.getEpicUserHandle());
    stats.setPlatformName(response.getPlatformName());

    // Parser les statistiques globales
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

    // Parser les statistiques compétitives (si disponibles)
    if (response.getStats() != null && response.getStats().getCurr_p2() != null) {
      // TODO: Mapper correctement les stats competitive
      FortniteTrackerPlayerStats.CompetitiveStats competitiveStats =
          new FortniteTrackerPlayerStats.CompetitiveStats();
      // Temporairement, on ne mappe pas les stats
      stats.setCompetitiveStats(competitiveStats);
    }

    return stats;
  }

  /** Mappe une région Fortnite vers une plateforme FortniteTracker */
  private String mapRegionToPlatform(String region) {
    // FortniteTracker utilise des plateformes, pas des régions
    // On peut mapper ou utiliser une plateforme par défaut
    return switch (region.toUpperCase()) {
      case "EU", "NAC", "NAW" -> "pc";
      case "BR", "ASIA" -> "pc";
      case "OCE", "ME" -> "pc";
      default -> "pc";
    };
  }

  /** Applique le rate limiting */
  private void enforceRateLimit() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime oneMinuteAgo = now.minusMinutes(1);

    // Nettoyer les anciennes timestamps
    requestTimestamps.removeIf(timestamp -> timestamp.isBefore(oneMinuteAgo));

    // Vérifier si on a atteint la limite
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

  /** Vide le cache */
  public void clearCache() {
    playerStatsCache.clear();
    log.info("Cache FortniteTracker vidé");
  }

  /** Classe interne pour le cache avec expiration */
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
