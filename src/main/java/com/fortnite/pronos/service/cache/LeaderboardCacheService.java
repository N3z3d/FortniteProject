package com.fortnite.pronos.service.cache;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.LeaderboardEntryDTO;

import lombok.extern.slf4j.Slf4j;

/** Service de cache pour le leaderboard - MVP basique */
@Service
@Slf4j
public class LeaderboardCacheService {

  // Cache simple en mémoire pour le MVP
  private final ConcurrentHashMap<Integer, CachedLeaderboard> leaderboardCache =
      new ConcurrentHashMap<>();
  private static final long CACHE_DURATION_MINUTES = 15; // 15 minutes

  /** Récupère le leaderboard depuis le cache */
  public List<LeaderboardEntryDTO> getCachedLeaderboard(Integer season) {
    log.info("Récupération du leaderboard mis en cache pour la saison: {}", season);

    if (season == null) {
      season = getCurrentSeason();
    }

    CachedLeaderboard cached = leaderboardCache.get(season);

    if (cached == null || isExpired(cached)) {
      log.info("Cache manquant ou expiré pour la saison {}, retour d'un leaderboard vide", season);
      return generateEmptyLeaderboard();
    }

    log.info("Leaderboard récupéré depuis le cache: {} entrées", cached.entries.size());
    return new ArrayList<>(cached.entries);
  }

  /** Met en cache le leaderboard */
  public void cacheLeaderboard(Integer season, List<LeaderboardEntryDTO> entries) {
    log.info("Mise en cache du leaderboard pour la saison {}: {} entrées", season, entries.size());

    if (season == null) {
      season = getCurrentSeason();
    }

    CachedLeaderboard cached = new CachedLeaderboard();
    cached.entries = new ArrayList<>(entries);
    cached.cachedAt = LocalDateTime.now();
    cached.season = season;

    leaderboardCache.put(season, cached);

    log.info("Leaderboard mis en cache avec succès pour la saison {}", season);
  }

  /** Invalide le cache du leaderboard */
  public void invalidateLeaderboardCache() {
    log.debug("Cache leaderboard invalidé");
    leaderboardCache.clear();
  }

  /** Invalide le cache pour une saison spécifique */
  public void invalidateLeaderboardCache(Integer season) {
    log.debug("Cache leaderboard invalidé pour la saison: {}", season);
    if (season != null) {
      leaderboardCache.remove(season);
    }
  }

  /** Invalide tout le cache */
  public void invalidateAll() {
    log.debug("Tous les caches invalidés");
    leaderboardCache.clear();
  }

  /** Vérifie si le cache est expiré */
  private boolean isExpired(CachedLeaderboard cached) {
    if (cached.cachedAt == null) {
      return true;
    }

    LocalDateTime expirationTime = cached.cachedAt.plusMinutes(CACHE_DURATION_MINUTES);
    boolean expired = LocalDateTime.now().isAfter(expirationTime);

    if (expired) {
      log.debug("Cache expiré pour la saison {}", cached.season);
    }

    return expired;
  }

  /** Génère un leaderboard vide pour les tests */
  private List<LeaderboardEntryDTO> generateEmptyLeaderboard() {
    log.debug("Génération d'un leaderboard vide");
    return new ArrayList<>();
  }

  /** Récupère la saison courante */
  private Integer getCurrentSeason() {
    return 2025; // Saison par défaut pour le MVP
  }

  /** Obtient les statistiques du cache */
  public CacheStats getCacheStats() {
    CacheStats stats = new CacheStats();
    stats.totalEntries = leaderboardCache.size();
    stats.activeEntries =
        (int) leaderboardCache.values().stream().filter(cached -> !isExpired(cached)).count();
    stats.expiredEntries = stats.totalEntries - stats.activeEntries;

    log.debug(
        "Statistiques du cache: {} entrées totales, {} actives, {} expirées",
        stats.totalEntries,
        stats.activeEntries,
        stats.expiredEntries);

    return stats;
  }

  /** Nettoie les entrées expirées du cache */
  public void cleanupExpiredEntries() {
    log.debug("Nettoyage des entrées expirées du cache");

    int initialSize = leaderboardCache.size();
    leaderboardCache.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    int finalSize = leaderboardCache.size();

    log.info(
        "Nettoyage terminé: {} entrées supprimées ({} -> {})",
        initialSize - finalSize,
        initialSize,
        finalSize);
  }

  /** Classe interne pour le cache */
  private static class CachedLeaderboard {
    List<LeaderboardEntryDTO> entries;
    LocalDateTime cachedAt;
    Integer season;
  }

  /** Classe pour les statistiques du cache */
  public static class CacheStats {
    public int totalEntries;
    public int activeEntries;
    public int expiredEntries;
  }
}
