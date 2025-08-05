package com.fortnite.pronos.config;

import java.time.Duration;
import java.util.HashMap;
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
        "leaderboard",
        defaultConfig.entryTtl(Duration.ofMinutes(10))); // 10 min pour données temps réel

    // LEADERBOARD STATS - Moins critiques, peuvent être mises en cache plus longtemps
    cacheConfigurations.put("leaderboard-stats", defaultConfig.entryTtl(Duration.ofMinutes(30)));

    // GAMES - Données moyennement dynamiques
    cacheConfigurations.put(
        "games", defaultConfig.entryTtl(Duration.ofMinutes(60))); // 1h pour les games

    // PLAYERS - Données relativement statiques (OPTIMISÉ pour 149 joueurs)
    cacheConfigurations.put(
        "players",
        defaultConfig.entryTtl(Duration.ofHours(24))); // 24h car les 149 joueurs changent très peu

    // PLAYER PAGES - Cache spécialisé pour la pagination
    cacheConfigurations.put(
        "playerPages",
        defaultConfig.entryTtl(Duration.ofHours(12))); // 12h pour les pages de joueurs

    // REGION DISTRIBUTION - Données très stables
    cacheConfigurations.put(
        "region-distribution", defaultConfig.entryTtl(Duration.ofHours(12))); // 12h car très stable

    // TEAM COMPOSITIONS - Données statiques
    cacheConfigurations.put(
        "team-compositions",
        defaultConfig.entryTtl(Duration.ofHours(2))); // 2h pour les compositions d'équipes

    // SCORES - Données critiques mais mises à jour par batch
    cacheConfigurations.put(
        "scores", defaultConfig.entryTtl(Duration.ofMinutes(15))); // 15 min pour les scores

    // DASHBOARD DATA - Données agrégées, peuvent être mises en cache
    cacheConfigurations.put(
        "dashboard", defaultConfig.entryTtl(Duration.ofMinutes(20))); // 20 min pour le dashboard

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
    cacheManager.setCacheNames(
        java.util.Arrays.asList(
            "leaderboard",
            "leaderboard-stats",
            "games",
            "players",
            "playerPages",
            "region-distribution",
            "team-compositions",
            "scores",
            "dashboard"));
    return cacheManager;
  }

  /** CACHE SPÉCIALISÉ POUR LES TESTS Cache en mémoire simple pour les performances de test */
  @Bean
  @Primary // Marquer comme primaire pour les tests
  @Profile("test")
  public CacheManager testCacheManager() {
    return new ConcurrentMapCacheManager(
        "leaderboard",
        "leaderboard-stats",
        "games",
        "players",
        "playerPages",
        "region-distribution",
        "team-compositions",
        "scores",
        "dashboard");
  }
}
