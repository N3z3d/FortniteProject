package com.fortnite.pronos.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration du cache pour l'application - OPTIMISÉE POUR LES PERFORMANCES
 *
 * <p>STRATÉGIE MULTI-NIVEAUX: - Production: Redis avec TTL différenciés par type de données -
 * Développement: Redis ou fallback vers cache en mémoire - Tests: Cache en mémoire pour les
 * performances
 */
@Configuration
@EnableCaching
public class CacheConfig {

  private static final String CACHE_LEADERBOARD = "leaderboard";
  private static final String CACHE_LEADERBOARD_STATS = "leaderboard-stats";
  private static final String CACHE_GAME_STATS = "gameStats";
  private static final String CACHE_REGION_DISTRIBUTION = "regionDistribution";
  private static final String CACHE_PLAYER_SCORES = "playerScores";
  private static final String CACHE_GAMES = "games";
  private static final String CACHE_PLAYERS = "players";
  private static final String CACHE_PLAYER_PAGES = "playerPages";
  private static final String CACHE_REGION_DISTRIBUTION_AGGREGATED = "region-distribution";
  private static final String CACHE_TEAM_COMPOSITIONS = "team-compositions";
  private static final String CACHE_SCORES = "scores";
  private static final String CACHE_DASHBOARD = "dashboard";
  private static final List<String> CACHE_NAMES =
      List.of(
          CACHE_LEADERBOARD,
          CACHE_LEADERBOARD_STATS,
          CACHE_GAME_STATS,
          CACHE_REGION_DISTRIBUTION,
          CACHE_PLAYER_SCORES,
          CACHE_GAMES,
          CACHE_PLAYERS,
          CACHE_PLAYER_PAGES,
          CACHE_REGION_DISTRIBUTION_AGGREGATED,
          CACHE_TEAM_COMPOSITIONS,
          CACHE_SCORES,
          CACHE_DASHBOARD);

  /**
   * CACHE REDIS HAUTE PERFORMANCE - Configuration Production TTL optimisés selon la fréquence de
   * mise à jour des données
   */
  @Bean
  @Primary
  @ConditionalOnProperty(name = "spring.data.redis.host")
  public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {

    // Configuration par défaut optimisée
    RedisCacheConfiguration defaultConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30)) // TTL par défaut
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues(); // Éviter de cacher les valeurs null

    // CONFIGURATIONS SPÉCIALISÉES par type de données
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

    // LEADERBOARD - Données critiques, mise à jour fréquente
    cacheConfigurations.put(
        CACHE_LEADERBOARD,
        defaultConfig.entryTtl(Duration.ofMinutes(10))); // 10 min pour données temps réel

    // LEADERBOARD STATS - Moins critiques, peuvent être mises en cache plus longtemps
    cacheConfigurations.put(
        CACHE_LEADERBOARD_STATS, defaultConfig.entryTtl(Duration.ofMinutes(30)));

    // GAME STATS - Statistiques du leaderboard (utilisé par LeaderboardService)
    cacheConfigurations.put(CACHE_GAME_STATS, defaultConfig.entryTtl(Duration.ofMinutes(30)));

    // REGION DISTRIBUTION - Répartition par région (utilisé par LeaderboardService)
    cacheConfigurations.put(
        CACHE_REGION_DISTRIBUTION, defaultConfig.entryTtl(Duration.ofHours(12)));

    // PLAYER SCORES - Scores des joueurs (utilisé par LeaderboardService)
    cacheConfigurations.put(CACHE_PLAYER_SCORES, defaultConfig.entryTtl(Duration.ofMinutes(15)));

    // GAMES - Données moyennement dynamiques
    cacheConfigurations.put(
        CACHE_GAMES, defaultConfig.entryTtl(Duration.ofMinutes(60))); // 1h pour les games

    // PLAYERS - Données relativement statiques (OPTIMISÉ pour 147 joueurs)
    cacheConfigurations.put(
        CACHE_PLAYERS,
        defaultConfig.entryTtl(Duration.ofHours(24))); // 24h car les 147 joueurs changent très peu

    // PLAYER PAGES - Cache spécialisé pour la pagination
    cacheConfigurations.put(
        CACHE_PLAYER_PAGES,
        defaultConfig.entryTtl(Duration.ofHours(12))); // 12h pour les pages de joueurs

    // REGION DISTRIBUTION - Données très stables
    cacheConfigurations.put(
        CACHE_REGION_DISTRIBUTION_AGGREGATED,
        defaultConfig.entryTtl(Duration.ofHours(12))); // 12h car très stable

    // TEAM COMPOSITIONS - Données statiques
    cacheConfigurations.put(
        CACHE_TEAM_COMPOSITIONS,
        defaultConfig.entryTtl(Duration.ofHours(2))); // 2h pour les compositions d'équipes

    // SCORES - Données critiques mais mises à jour par batch
    cacheConfigurations.put(
        CACHE_SCORES, defaultConfig.entryTtl(Duration.ofMinutes(15))); // 15 min pour les scores

    // DASHBOARD DATA - Données agrégées, peuvent être mises en cache
    cacheConfigurations.put(
        CACHE_DASHBOARD,
        defaultConfig.entryTtl(Duration.ofMinutes(20))); // 20 min pour le dashboard

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .transactionAware() // Support des transactions
        .build();
  }

  /**
   * FALLBACK CACHE - Cache en mémoire pour développement Utilisé quand Redis n'est pas disponible
   * et pas en mode test
   */
  @Bean
  @ConditionalOnProperty(
      name = "spring.data.redis.host",
      havingValue = "false",
      matchIfMissing = true)
  @Profile("!test") // Exclure du profil test pour éviter les conflits
  public CacheManager concurrentMapCacheManager() {
    ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
    cacheManager.setCacheNames(CACHE_NAMES);
    return cacheManager;
  }

  /** CACHE SPÉCIALISÉ POUR LES TESTS Cache en mémoire simple pour les performances de test */
  @Bean
  @Primary // Marquer comme primaire pour les tests
  @Profile("test")
  public CacheManager testCacheManager() {
    return new ConcurrentMapCacheManager(CACHE_NAMES.toArray(String[]::new));
  }
}
